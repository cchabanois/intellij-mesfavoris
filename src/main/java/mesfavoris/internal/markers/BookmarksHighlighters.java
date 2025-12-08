package mesfavoris.internal.markers;

import com.intellij.openapi.Disposable;
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
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentListener;
import com.intellij.psi.PsiFile;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.messages.MessageBus;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.internal.service.BookmarksService;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static mesfavoris.internal.markers.BookmarksMarkers.BookmarksMarkersListener;

public class BookmarksHighlighters implements Disposable {
    public static final Key<BookmarkId> BOOKMARK_ID_KEY = new Key<>("bookmarkId");
    private final Project project;
    private BookmarksHighlightersDocumentListener documentListener;

    public BookmarksHighlighters(Project project) {
        this.project = project;
    }

    public void init() {
        project.getMessageBus().connect(this).subscribe(PsiDocumentListener.TOPIC, this::documentCreated);
        project.getMessageBus().connect(this).subscribe(BookmarksMarkers.BookmarksMarkersListener.TOPIC, getBookmarksMarkersListener());

        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        IBookmarksMarkers bookmarksMarkers = project.getService(IBookmarksService.class).getBookmarksMarkers();

        this.documentListener = new BookmarksHighlightersDocumentListener(project, bookmarksMarkers);
        multicaster.addDocumentListener(documentListener, this);

        createHighlightersForOpenFiles();
    }

    @Override
    public void dispose() {
        // MessageBus connections and document listener will be automatically removed
        // But we still need to dispose the document listener to stop pending timers
        if (documentListener != null) {
            documentListener.dispose();
        }
    }

    public static List<RangeHighlighterEx> getBookmarksHighlighters(Project project, Document document) {
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

    private BookmarksMarkersListener getBookmarksMarkersListener() {
        return new BookmarksMarkersListener() {
            @Override
            public void bookmarkMarkerDeleted(BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
                    RangeHighlighterEx highlighter = getHighlighter(project, bookmarkMarker);
                    if (highlighter != null) {
                        highlighter.dispose();
                        project.getMessageBus().syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterDeleted(bookmarkMarker.getBookmarkId());
                    }
                });
            }

            @Override
            public void bookmarkMarkerAdded(BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () -> createOrUpdateHighlighter(bookmarkMarker));
            }

            @Override
            public void bookmarkMarkerUpdated(BookmarkMarker previous, BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () -> createOrUpdateHighlighter(bookmarkMarker));
            }
        };
    }

    private void createOrUpdateHighlighter(BookmarkMarker bookmarkMarker) {
        RangeHighlighterEx previous = getHighlighter(project, bookmarkMarker);
        RangeHighlighterEx highlighter = createHighlighter(project, bookmarkMarker);
        if (highlighter == null) {
            if (previous != null) {
                previous.dispose();
                project.getMessageBus().syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterDeleted(bookmarkMarker.getBookmarkId());
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
        ReadAction.run(() -> {
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

            for (VirtualFile file : fileEditorManager.getOpenFiles()) {
                Document document = fileDocumentManager.getDocument(file);
                if (document != null) {
                    createHighlightersForFile(file, document);
                }
            }
        });
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
            for (BookmarkMarker marker : markers) {
                createHighlighter(markupModel, marker);
            }
        });
    }

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
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
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

    private static class BookmarksHighlightersDocumentListener implements BulkAwareDocumentListener.Simple {
        private static final int DEBOUNCE_DELAY_MS = 1000;

        private final Map<Document, Timer> pendingProcessing = new ConcurrentHashMap<>();
        private final Project project;
        private final IBookmarksMarkers bookmarksMarkers;

        public BookmarksHighlightersDocumentListener(Project project, IBookmarksMarkers bookmarksMarkers) {
            this.project = project;
            this.bookmarksMarkers = bookmarksMarkers;
        }

        /**
         * Cleanup method to stop all pending timers. Should be called when disposing the listener.
         */
        public void dispose() {
            pendingProcessing.values().forEach(Timer::stop);
            pendingProcessing.clear();
        }

        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent e) {
        }

        private boolean isDocumentInProject(Document document) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file == null) {
                return false;
            }
            ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
            return projectFileIndex.isInProject(file);
        }


        @Override
        public void documentChanged(@NotNull DocumentEvent e) {
            Document document = e.getDocument();

            // Only check if document belongs to project - let processDocumentChange handle bookmark check
            if (!isDocumentInProject(document)) {
                return;
            }

            // Cancel any pending processing for this document
            Timer existingTimer = pendingProcessing.get(document);
            if (existingTimer != null) {
                existingTimer.stop();
            }

            // Schedule debounced processing
            Timer debounceTimer = new Timer(DEBOUNCE_DELAY_MS, actionEvent -> {
                try {
                    processDocumentChange(document);
                } finally {
                    pendingProcessing.remove(document);
                }
            });
            debounceTimer.setRepeats(false);
            pendingProcessing.put(document, debounceTimer);
            debounceTimer.start();
        }

        private void processDocumentChange(Document document) {
            // Get current highlighters and markers for this document
            List<RangeHighlighterEx> currentHighlighters = BookmarksHighlighters.getBookmarksHighlighters(project, document);

            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file == null) {
                return;
            }
            List<BookmarkMarker> documentMarkers = bookmarksMarkers.getMarkers(file);

            // Early exit if no bookmarks in either direction
            if (currentHighlighters.isEmpty() && documentMarkers.isEmpty()) {
                return;
            }

            MessageBus messageBus = project.getMessageBus();

            // 1. Check existing highlighters for position changes
            Set<BookmarkId> processedMarkerIds = new HashSet<>();
            for (RangeHighlighterEx highlighter : currentHighlighters) {
                BookmarkMarker bookmarkMarker = getBookmarkMarker(highlighter);
                if (bookmarkMarker != null) {
                    processedMarkerIds.add(bookmarkMarker.getBookmarkId());
                    if (hasPositionChanged(highlighter, bookmarkMarker)) {
                        messageBus.syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterUpdated(highlighter);
                    }
                }
            }

            // 2. Check for markers without corresponding highlighters (deleted highlighters)
            for (BookmarkMarker marker : documentMarkers) {
                if (!processedMarkerIds.contains(marker.getBookmarkId())) {
                    messageBus.syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterDeleted(marker.getBookmarkId());
                }
            }
        }

        private BookmarkMarker getBookmarkMarker(RangeHighlighterEx highlighter) {
            BookmarkId bookmarkId = highlighter.getUserData(BOOKMARK_ID_KEY);
            if (bookmarkId == null) {
                return null;
            }
            return bookmarksMarkers.getMarker(bookmarkId);
        }

        private boolean hasPositionChanged(RangeHighlighterEx highlighter, BookmarkMarker marker) {
            // Compare current highlighter position with stored bookmark position
            int expectedLine = marker.getLineNumber();
            int actualLine = highlighter.getDocument().getLineNumber(highlighter.getStartOffset());

            return expectedLine != actualLine;
        }

    }


    public static class BookmarksHighlightersStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            BookmarksHighlighters bookmarksHighlighters = new BookmarksHighlighters(project);
            bookmarksHighlighters.init();

            // Register with BookmarksService as disposable parent to avoid Project warning
            BookmarksService bookmarksService = project.getService(BookmarksService.class);
            Disposer.register(bookmarksService, bookmarksHighlighters);
        }
    }


}
