package mesfavoris.internal.markers;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import mesfavoris.markers.IBookmarksHighlighters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * Provides inlay hints that display bookmark comments above the bookmarked lines
 */
@SuppressWarnings("UnstableApiUsage")
public class BookmarkCommentInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return new SettingsKey<>("bookmark.comments.hints");
    }

    @NotNull
    @Override
    public String getName() {
        return "Bookmark comments";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return """
                // Example code with bookmark
                public void myMethod() {
                    System.out.println("Hello");
                }
                """;
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener listener) {
                return new JPanel();
            }

            @NotNull
            @Override
            public String getMainCheckboxText() {
                return "Show bookmark comments above bookmarked lines";
            }
        };
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                                @NotNull Editor editor,
                                                @NotNull NoSettings settings,
                                                @NotNull InlayHintsSink sink) {
        Project project = file.getProject();
        Document document = editor.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

        if (virtualFile == null) {
            return null;
        }

        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        if (bookmarksService == null) {
            return null;
        }

        IBookmarksHighlighters bookmarksHighlighters = project.getService(IBookmarksHighlighters.class);
        if (bookmarksHighlighters == null) {
            return null;
        }

        BookmarksTree bookmarksTree = bookmarksService.getBookmarksTree();

        return new BookmarkCommentCollector(editor, bookmarksHighlighters, bookmarksTree);
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }

    private static class BookmarkCommentCollector extends FactoryInlayHintsCollector {
        private final IBookmarksHighlighters bookmarksHighlighters;
        private final BookmarksTree bookmarksTree;

        public BookmarkCommentCollector(@NotNull Editor editor,
                                        @NotNull IBookmarksHighlighters bookmarksHighlighters,
                                        @NotNull BookmarksTree bookmarksTree) {
            super(editor);
            this.bookmarksHighlighters = bookmarksHighlighters;
            this.bookmarksTree = bookmarksTree;
        }

        @Override
        public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
            // Only process the root element (PsiFile) to avoid processing highlighters multiple times
            if (!(element instanceof PsiFile)) {
                return true;
            }

            Document document = editor.getDocument();
            PresentationFactory factory = getFactory();

            List<RangeHighlighterEx> highlighters = bookmarksHighlighters.getBookmarksHighlighters(document);
            for (RangeHighlighterEx highlighter : highlighters) {
                List<BookmarkId> bookmarkIds = highlighter.getUserData(IBookmarksHighlighters.BOOKMARK_IDS_KEY);
                if (bookmarkIds == null || bookmarkIds.isEmpty()) {
                    continue;
                }

                int lineNumber = document.getLineNumber(highlighter.getStartOffset());
                if (lineNumber < 0 || lineNumber >= document.getLineCount()) {
                    continue;
                }

                int lineStartOffset = document.getLineStartOffset(lineNumber);

                // Process all bookmarks on this line
                for (BookmarkId bookmarkId : bookmarkIds) {
                    Bookmark bookmark = bookmarksTree.getBookmark(bookmarkId);

                    if (bookmark == null) {
                        continue;
                    }

                    String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
                    if (comment == null || comment.trim().isEmpty()) {
                        continue;
                    }

                    addCommentInlayHints(factory, sink, lineStartOffset, comment);
                }
            }

            return true;
        }

        private void addCommentInlayHints(PresentationFactory factory, InlayHintsSink sink,
                                          int lineStartOffset, String comment) {
            String[] commentLines = comment.split("\\n");

            // Add a block inlay hint for each line of the comment
            for (int i = 0; i < commentLines.length; i++) {
                String line = commentLines[i].trim();
                String prefix = i == 0 ? "🔖 " : "   ";
                InlayPresentation presentation = factory.smallText(prefix + line);
                // All lines are added at the same offset, they will stack vertically
                sink.addBlockElement(lineStartOffset, true, true, i, presentation);
            }
        }
    }
}

