package mesfavoris.internal.markers;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import mesfavoris.model.BookmarkId;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static mesfavoris.internal.markers.BookmarksHighlighters.BOOKMARK_ID_KEY;

public class BookmarksHighlightersDocumentListener implements BulkAwareDocumentListener.Simple {
    private final Map<Document, List<BookmarkHighlighterRangeSnapshot>> beforeChangeHighlighters = Collections.synchronizedMap(new WeakHashMap<>());
    private final Project project;

    public BookmarksHighlightersDocumentListener(Project project) {
        this.project = project;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent e) {
        Document document = e.getDocument();

        // Only process documents that belong to this project
        if (!isDocumentInProject(document)) {
            return;
        }

        beforeChangeHighlighters.put(document, takeHighlightersSnapshot(document));
    }

    private boolean isDocumentInProject(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return false;
        }
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
        return projectFileIndex.isInProject(file);
    }

    private List<BookmarkHighlighterRangeSnapshot> takeHighlightersSnapshot(Document document) {
        return BookmarksHighlighters.getBookmarksHighlighters(project, document).stream().map(BookmarkHighlighterRangeSnapshot::new).collect(Collectors.toList());
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
        Document document = e.getDocument();
        // Only process documents that belong to this project
        if (!isDocumentInProject(document)) {
            return;
        }
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
}
