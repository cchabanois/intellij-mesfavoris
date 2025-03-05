package mesfavoris.internal.markers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentListener;
import com.intellij.psi.PsiFile;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static mesfavoris.internal.markers.BookmarksMarkers.BookmarksMarkersListener;

public class BookmarksHighlighters implements Disposable {
    public static final Key<BookmarkId> BOOKMARK_ID_KEY = new Key<>("bookmarkId");
    private final Project project;

    public BookmarksHighlighters(Project project) {
        this.project = project;
    }

    public void init() {
        Disposer.register(project, this);
        project.getMessageBus().connect().subscribe(PsiDocumentListener.TOPIC, this::documentCreated);
        project.getMessageBus().connect().subscribe(BookmarksMarkers.BookmarksMarkersListener.TOPIC, getBookmarksMarkersListener());
        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        multicaster.addDocumentListener(new HighlightersDocumentListener(), this);
        createHighlightersForOpenFiles();
    }

    @Override
    public void dispose() {
    }

    private BookmarksMarkersListener getBookmarksMarkersListener() {
        return new BookmarksMarkersListener() {
            @Override
            public void bookmarkMarkerDeleted(BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
                    RangeHighlighterEx highlighter = getHighlighter(project, bookmarkMarker);
                    if (highlighter != null) {
                        highlighter.dispose();
                        project.getMessageBus().syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterDeleted(highlighter);
                    }
                });
            }

            @Override
            public void bookmarkMarkerAdded(BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
                    createOrUpdateHighlighter(bookmarkMarker);
                });

                /*
                ReadAction.nonBlocking(() -> {
                    VirtualFile file = bookmarkMarker.getResource();
                    FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
                    Document document = fileDocumentManager.getDocument(file);
                    if (document != null) {
                        MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
                        AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
                            createHighlighter(markupModel, bookmarkMarker);
                        });
                    }
                }).expireWith(project).submit(NonUrgentExecutor.getInstance()); */
            }

            @Override
            public void bookmarkMarkerUpdated(BookmarkMarker previous, BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
                    createOrUpdateHighlighter(bookmarkMarker);
                });
            }
        };
    }

    private void createOrUpdateHighlighter(BookmarkMarker bookmarkMarker) {
        RangeHighlighterEx previous = getHighlighter(project, bookmarkMarker);
        RangeHighlighterEx highlighter = createHighlighter(project, bookmarkMarker);
        if (highlighter == null) {
            if (previous != null) {
                previous.dispose();
                project.getMessageBus().syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterDeleted(previous);
            }
        } else {
            if (previous != null) {
                previous.dispose();
                project.getMessageBus().syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterUpdated(highlighter);
            } else {
                project.getMessageBus().syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterAdded(highlighter);
            }
        }
    }

    private void createHighlightersForOpenFiles() {
        ReadAction.nonBlocking(() -> {
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

            for (VirtualFile file : fileEditorManager.getOpenFiles()) {
                Document document = fileDocumentManager.getDocument(file);
                if (document != null) {
                    createHighlightersForFile(file, document);
                }
            }
        }).expireWith(project).submit(NonUrgentExecutor.getInstance());
    }

    private void documentCreated(@NotNull Document document, @Nullable PsiFile psiFile, @NotNull Project project) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        createHighlightersForFile(file, document);
    }

    private void createHighlightersForFile(VirtualFile file, Document document) {
        List<BookmarkMarker> markers = getMarkers(project, file);
        AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
            MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
//            markupModel.addMarkupModelListener(project, markupModelListener);
            for (BookmarkMarker marker : markers) {
                createHighlighter(markupModel, marker);
            }
        });
    }

    /*
        private final MarkupModelListener markupModelListener = new MarkupModelListener() {

                @Override
                public void afterAdded(@NotNull RangeHighlighterEx highlighter) {

                }

                @Override
                public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
                    project
                }

                @Override
                public void attributesChanged(@NotNull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
                    int lineNumber = highlighter.getDocument().getLineNumber(highlighter.getStartOffset());

                }
        };
    */
    private RangeHighlighterEx createHighlighter(Project project, BookmarkMarker bookmarkMarker) {
        VirtualFile file = bookmarkMarker.getResource();
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getDocument(file);
        if (document != null) {
            MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
            return createHighlighter(markupModel, bookmarkMarker);
        } else {
            return null;
        }
    }

    private RangeHighlighterEx createHighlighter(MarkupModelEx markupModel, BookmarkMarker bookmarkMarker) {
        String lineAsString = bookmarkMarker.getAttributes().get(BookmarkMarker.LINE_NUMBER);
        if (lineAsString == null) {
            return null;
        }
        int line = Integer.parseInt(lineAsString);
        RangeHighlighterEx highlighter = markupModel.addPersistentLineHighlighter(CodeInsightColors.BOOKMARKS_ATTRIBUTES, line, HighlighterLayer.ERROR + 1);
        if (highlighter != null) {
            highlighter.setGutterIconRenderer(new BookmarkGutterIconRenderer(bookmarkMarker));
            highlighter.putUserData(BOOKMARK_ID_KEY, bookmarkMarker.getBookmarkId());
        }
        return highlighter;
    }

    private List<BookmarkMarker> getMarkers(Project project, VirtualFile file) {
        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        IBookmarksMarkers bookmarksMarkers = bookmarksService.getBookmarksMarkers();
        return bookmarksMarkers.getMarkers(file);
    }

    private RangeHighlighterEx getHighlighter(Project project, BookmarkMarker bookmarkMarker) {
        VirtualFile file = bookmarkMarker.getResource();
        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document == null) return null;
        MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
        final Document markupDocument = markup.getDocument();
        final int startOffset = 0;
        final int endOffset = markupDocument.getTextLength();

        final Ref<RangeHighlighterEx> found = new Ref<>();
        markup.processRangeHighlightersOverlappingWith(startOffset, endOffset, highlighter -> {
            BookmarkId bookmarkId = highlighter.getUserData(BOOKMARK_ID_KEY);
            if (bookmarkMarker.getBookmarkId().equals(bookmarkId)) {
                found.set(highlighter);
                return false;
            }
            return true;
        });
        return found.get();
    }

    private List<RangeHighlighterEx> getHighlighters(Document document) {
        MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, false);
        if (markupModel == null) {
            return Collections.emptyList();
        }
        List<RangeHighlighterEx> highlighters = new ArrayList<>();
        markupModel.processRangeHighlightersOverlappingWith(0, document.getTextLength(), highlighter -> {
            BookmarkId bookmarkId = highlighter.getUserData(BOOKMARK_ID_KEY);
            if (bookmarkId != null) {
                highlighters.add(highlighter);
            }
            return true;
        });
        return highlighters;
    }

    private static class BookmarkHighlighterRangeSnapshot {
        private final RangeHighlighterEx highlighter;
        private final BookmarkId bookmarkId;
        private final int startOffset;
        private final int endOffset;

        public BookmarkHighlighterRangeSnapshot(RangeHighlighterEx highlighter) {
            this.highlighter = highlighter;
            this.bookmarkId = highlighter.getUserData(BOOKMARK_ID_KEY);
            this.startOffset = highlighter.getStartOffset();
            this.endOffset = highlighter.getEndOffset();
        }

    }

    private class HighlightersDocumentListener implements BulkAwareDocumentListener.Simple {
        private final Map<Document, List<BookmarkHighlighterRangeSnapshot>> beforeChangeHighlighters = new HashMap<>();

        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent e) {
            Document document = e.getDocument();
            beforeChangeHighlighters.put(document, takeHighlightersSnapshot(document));
        }

        private List<BookmarkHighlighterRangeSnapshot> takeHighlightersSnapshot(Document document) {
            return getHighlighters(document).stream().map(highlighter -> new BookmarkHighlighterRangeSnapshot(highlighter)).collect(Collectors.toList());
        }

        private Map<BookmarkId, BookmarkHighlighterRangeSnapshot> asMap(List<BookmarkHighlighterRangeSnapshot> highlighters) {
            return highlighters.stream().collect(Collectors.toMap(highlighter -> highlighter.bookmarkId, highlighter -> highlighter));
        }

        private void diff(List<BookmarkHighlighterRangeSnapshot> beforeChangeHighlighters, List<BookmarkHighlighterRangeSnapshot> afterChangeHighlighters, Set<RangeHighlighterEx> addedHighlighters, Set<RangeHighlighterEx> removedHighlighters, Set<RangeHighlighterEx> updatedHighlighters) {
            Map<BookmarkId, BookmarkHighlighterRangeSnapshot> beforeChangeHighlightersMap = asMap(beforeChangeHighlighters);
            Map<BookmarkId, BookmarkHighlighterRangeSnapshot> afterChangeHighlightersMap = asMap(afterChangeHighlighters);
            for (BookmarkHighlighterRangeSnapshot beforeChangeHighlighter : beforeChangeHighlighters) {
                BookmarkId bookmarkId = beforeChangeHighlighter.bookmarkId;
                BookmarkHighlighterRangeSnapshot afterChangeHighlighter = afterChangeHighlightersMap.get(bookmarkId);
                if (afterChangeHighlighter == null) {
                    removedHighlighters.add(beforeChangeHighlighter.highlighter);
                } else {
                    if (beforeChangeHighlighter.startOffset != afterChangeHighlighter.startOffset) {
                        updatedHighlighters.add(afterChangeHighlighter.highlighter);
                    }
                }
            }
            for (BookmarkHighlighterRangeSnapshot afterChangeHighlighter : afterChangeHighlighters) {
                BookmarkId bookmarkId = afterChangeHighlighter.bookmarkId;
                BookmarkHighlighterRangeSnapshot beforeChangeHighlighter = beforeChangeHighlightersMap.get(bookmarkId);
                if (beforeChangeHighlighter == null) {
                    addedHighlighters.add(afterChangeHighlighter.highlighter);
                }
            }
        }

        @Override
        public void documentChanged(@NotNull DocumentEvent e) {
            if (!ApplicationManager.getApplication().isDispatchThread()) {
                return;// Changes in lightweight documents are irrelevant to bookmarks and have to be ignored
            }
            Document document = e.getDocument();
            List<BookmarkHighlighterRangeSnapshot> beforeChangeHighlighters = this.beforeChangeHighlighters.remove(document);
            if (beforeChangeHighlighters == null || beforeChangeHighlighters.isEmpty()) {
                return;
            }
            List<BookmarkHighlighterRangeSnapshot> afterChangeHighlighters = takeHighlightersSnapshot(document);

            Set<RangeHighlighterEx> addedHighlighters = new HashSet<>();
            Set<RangeHighlighterEx> removedHighlighters = new HashSet<>();
            Set<RangeHighlighterEx> updatedHighlighters = new HashSet<>();
            diff(beforeChangeHighlighters, afterChangeHighlighters, addedHighlighters, removedHighlighters, updatedHighlighters);

            MessageBus messageBus = project.getMessageBus();
            addedHighlighters.forEach(highlighter -> messageBus.syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterAdded(highlighter));
            removedHighlighters.forEach(highlighter -> messageBus.syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterDeleted(highlighter));
            updatedHighlighters.forEach(highlighter -> messageBus.syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterUpdated(highlighter));
        }

    }

    public interface BookmarksHighlightersListener {
        Topic<BookmarksHighlightersListener> TOPIC = Topic.create("BookmarksHighlightersListener", BookmarksHighlightersListener.class);

        default BookmarkId getBookmarkId(RangeHighlighterEx highlighter) {
            return highlighter.getUserData(BookmarksHighlighters.BOOKMARK_ID_KEY);
        }

        void bookmarkHighlighterDeleted(RangeHighlighterEx bookmarkHighlighter);

        void bookmarkHighlighterAdded(RangeHighlighterEx bookmarkHighlighter);

        void bookmarkHighlighterUpdated(RangeHighlighterEx bookmarkHighlighter);

    }

    public static class BookmarksHighlightersStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            BookmarksHighlighters bookmarksHighlighters = new BookmarksHighlighters(project);
            bookmarksHighlighters.init();
        }
    }


}
