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
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentListener;
import com.intellij.psi.PsiFile;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.messages.MessageBus;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.markers.IBookmarksHighlighters;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static mesfavoris.internal.markers.BookmarksMarkers.BookmarksMarkersListener;

public class BookmarksHighlighters implements Disposable, IBookmarksHighlighters {
    private final Project project;
    private IBookmarksMarkers bookmarksMarkers;
    private BookmarksHighlightersDocumentListener documentListener;

    public BookmarksHighlighters(Project project) {
        this.project = project;
        this.bookmarksMarkers = project.getService(IBookmarksService.class).getBookmarksMarkers();

        project.getMessageBus().connect(this).subscribe(PsiDocumentListener.TOPIC, this::documentCreated);
        project.getMessageBus().connect(this).subscribe(BookmarksMarkers.BookmarksMarkersListener.TOPIC, getBookmarksMarkersListener());

        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        this.documentListener = new BookmarksHighlightersDocumentListener(project, this, bookmarksMarkers);
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

    @Override
    public List<RangeHighlighterEx> getBookmarksHighlighters(Document document) {
        MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, false);
        if (markupModel == null) {
            return Collections.emptyList();
        }
        List<RangeHighlighterEx> highlighters = new ArrayList<>();
        markupModel.processRangeHighlightersOverlappingWith(0, document.getTextLength(), highlighter -> {
            if (isBookmarkHighlighter(highlighter)) {
                highlighters.add(highlighter);
            }
            return true;
        });
        return highlighters;
    }

    private static boolean isBookmarkHighlighter(RangeHighlighterEx highlighter) {
        return highlighter.getUserData(BOOKMARK_IDS_KEY) != null;
    }

    private BookmarksMarkersListener getBookmarksMarkersListener() {
        return new BookmarksMarkersListener() {
            @Override
            public void bookmarkMarkerDeleted(BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () ->
                    ReadAction.run(() -> removeBookmarkFromHighlighter(bookmarkMarker)));
            }

            @Override
            public void bookmarkMarkerAdded(BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () ->
                    ReadAction.run(() -> addBookmarkToHighlighter(bookmarkMarker)));
            }

            @Override
            public void bookmarkMarkerUpdated(BookmarkMarker previous, BookmarkMarker bookmarkMarker) {
                AppUIUtil.invokeLaterIfProjectAlive(project, () ->
                    ReadAction.run(() -> updateHighlighterFromBookmark(previous, bookmarkMarker)));
            }
        };
    }

    /**
     * Removes a bookmark from its highlighter, or updates the highlighter if multiple bookmarks remain
     */
    private void removeBookmarkFromHighlighter(BookmarkMarker bookmarkMarker) {
        VirtualFile file = bookmarkMarker.getResource();
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }

        RangeHighlighterEx highlighter = getHighlighter(project, bookmarkMarker);
        if (highlighter == null) {
            return;
        }

        List<BookmarkId> bookmarkIds = highlighter.getUserData(BOOKMARK_IDS_KEY);
        // Remove the bookmark ID from the list
        List<BookmarkId> updatedIds = new ArrayList<>(bookmarkIds);
        updatedIds.remove(bookmarkMarker.getBookmarkId());

        if (updatedIds.isEmpty()) {
            // No more bookmarks on this line, dispose the highlighter
            highlighter.dispose();
        } else {
            updateHighlighter(highlighter, updatedIds);
        }
    }

    private void updateHighlighter(RangeHighlighterEx highlighter, List<BookmarkId> bookmarkIds) {
        List<BookmarkMarker> remainingMarkers = bookmarkIds.stream()
                .map(bookmarksMarkers::getMarker)
                .toList();
        if (remainingMarkers.size() > 1) {
            highlighter.setGutterIconRenderer(new GroupedBookmarkGutterIconRenderer(remainingMarkers));
        } else {
            highlighter.setGutterIconRenderer(new BookmarkGutterIconRenderer(remainingMarkers.get(0)));
        }
        highlighter.putUserData(BOOKMARK_IDS_KEY, bookmarkIds);
    }

    /**
     * Adds a bookmark to an existing highlighter or creates a new one
     */
    private void addBookmarkToHighlighter(BookmarkMarker bookmarkMarker) {
        VirtualFile file = bookmarkMarker.getResource();
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }

        int lineNumber = bookmarkMarker.getLineNumber();

        // Check if there's already a highlighter on this line
        RangeHighlighterEx existingHighlighter = findBookmarkHighlighterAtLine(document, lineNumber);

        if (existingHighlighter != null) {
            // Add to existing highlighter
            List<BookmarkId> bookmarkIds = existingHighlighter.getUserData(BOOKMARK_IDS_KEY);
            if (bookmarkIds != null && !bookmarkIds.contains(bookmarkMarker.getBookmarkId())) {
                List<BookmarkId> updatedIds = new ArrayList<>(bookmarkIds);
                updatedIds.add(bookmarkMarker.getBookmarkId());

                updateHighlighter(existingHighlighter, updatedIds);
            }
        } else {
            // Create new highlighter
            MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
            RangeHighlighterEx newHighlighter = createHighlighter(markupModel, Arrays.asList(bookmarkMarker));
        }
    }

    /**
     * Updates a highlighter when a bookmark marker is updated (e.g., line number changed)
     */
    private void updateHighlighterFromBookmark(BookmarkMarker previous, BookmarkMarker updated) {
        if (previous.getLineNumber() == updated.getLineNumber()) {
            // Same line, just update the marker reference
            VirtualFile file = updated.getResource();
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                return;
            }

            RangeHighlighterEx highlighter = getHighlighter(project, updated);
        } else {
            // Line changed, remove from old line and add to new line
            removeBookmarkFromHighlighter(previous);
            addBookmarkToHighlighter(updated);
        }
    }

    /**
     * Finds a highlighter at the specified line number
     */
    @Override
    public RangeHighlighterEx findBookmarkHighlighterAtLine(Document document, int lineNumber) {
        MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, false);
        if (markupModel == null) {
            return null;
        }

        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int lineEndOffset = document.getLineEndOffset(lineNumber);

        Ref<RangeHighlighterEx> found = new Ref<>();
        markupModel.processRangeHighlightersOverlappingWith(lineStartOffset, lineEndOffset, highlighter -> {
            if (isBookmarkHighlighter(highlighter)) {
                found.set(highlighter);
                return false;
            }
            return true;
        });
        return found.get();
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

            // Group markers by line number
            Map<Integer, List<BookmarkMarker>> markersByLine = markers.stream()
                    .collect(Collectors.groupingBy(BookmarkMarker::getLineNumber));

            // Create highlighters
            for (Map.Entry<Integer, List<BookmarkMarker>> entry : markersByLine.entrySet()) {
                List<BookmarkMarker> lineMarkers = entry.getValue();
                createHighlighter(markupModel, lineMarkers);
            }
        });
    }

    private RangeHighlighterEx createHighlighter(Project project, BookmarkMarker bookmarkMarker) {
        VirtualFile file = bookmarkMarker.getResource();
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getDocument(file);
        if (document != null) {
            MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
            return createHighlighter(markupModel, List.of(bookmarkMarker));
        } else {
            return null;
        }
    }

    /**
     * Creates a highlighter for one or more bookmarks on the same line
     */
    private RangeHighlighterEx createHighlighter(MarkupModelEx markupModel, List<BookmarkMarker> bookmarkMarkers) {
        if (bookmarkMarkers.isEmpty()) {
            return null;
        }

        BookmarkMarker firstMarker = bookmarkMarkers.get(0);
        int line = firstMarker.getLineNumber();

        RangeHighlighterEx highlighter = markupModel.addPersistentLineHighlighter(
                CodeInsightColors.BOOKMARKS_ATTRIBUTES, line, HighlighterLayer.ERROR + 1);

        if (highlighter != null) {
            List<BookmarkId> bookmarkIds = bookmarkMarkers.stream()
                    .map(BookmarkMarker::getBookmarkId)
                    .collect(Collectors.toList());
            updateHighlighter(highlighter, bookmarkIds);
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
            List<BookmarkId> bookmarkIds = highlighter.getUserData(BOOKMARK_IDS_KEY);
            if (bookmarkIds != null && bookmarkIds.contains(bookmarkMarker.getBookmarkId())) {
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
        private final IBookmarksHighlighters bookmarksHighlighters;

        public BookmarksHighlightersDocumentListener(Project project, IBookmarksHighlighters bookmarksHighlighters, IBookmarksMarkers bookmarksMarkers) {
            this.project = project;
            this.bookmarksMarkers = bookmarksMarkers;
            this.bookmarksHighlighters = bookmarksHighlighters;
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
            List<RangeHighlighterEx> currentHighlighters = bookmarksHighlighters.getBookmarksHighlighters(document);

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
                List<BookmarkId> bookmarkIds = highlighter.getUserData(BOOKMARK_IDS_KEY);
                if (bookmarkIds != null) {
                    // Mark all bookmarks in this highlighter as processed
                    processedMarkerIds.addAll(bookmarkIds);

                    // Check position change using the first bookmark as reference
                    BookmarkMarker firstMarker = bookmarksMarkers.getMarker(bookmarkIds.get(0));
                    if (firstMarker != null && hasPositionChanged(highlighter, firstMarker)) {
                        messageBus.syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterMoved(highlighter);
                    }
                }
            }

            // 2. Check for markers without corresponding highlighters (deleted highlighters)
            for (BookmarkMarker marker : documentMarkers) {
                if (!processedMarkerIds.contains(marker.getBookmarkId())) {
                    messageBus.syncPublisher(BookmarksHighlightersListener.TOPIC).bookmarkHighlighterDeleted(List.of(marker.getBookmarkId()));
                }
            }
        }

        private boolean hasPositionChanged(RangeHighlighterEx highlighter, BookmarkMarker marker) {
            // Compare current highlighter position with stored bookmark position
            int expectedLine = marker.getLineNumber();
            int actualLine = highlighter.getDocument().getLineNumber(highlighter.getStartOffset());

            return expectedLine != actualLine;
        }

    }

    /**
     * StartupActivity to ensure the BookmarksHighlighters service is initialized at project startup.
     * This triggers the service instantiation, which in turn registers all necessary listeners
     * and creates highlighters for open files.
     */
    public static class BookmarksHighlightersStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            // Simply getting the service triggers its instantiation and initialization
            project.getService(BookmarksHighlighters.class);
        }
    }

}
