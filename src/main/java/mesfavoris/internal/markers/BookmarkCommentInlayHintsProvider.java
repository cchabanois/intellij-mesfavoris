package mesfavoris.internal.markers;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.InsetPresentation;
import com.intellij.codeInsight.hints.presentation.TextInlayPresentation;
import com.intellij.codeInsight.hints.presentation.WithAttributesPresentation;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
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

    private static final TextAttributesKey BOOKMARK_COMMENT_KEY = TextAttributesKey.createTextAttributesKey(
            "BOOKMARK_COMMENT",
            DefaultLanguageHighlighterColors.INLAY_DEFAULT
    );

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
                int indentWidth = calculateIndentWidth(editor, document, lineNumber);

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

                    addCommentInlayHints(editor, sink, lineStartOffset, comment, indentWidth);
                }
            }

            return true;
        }

        private void addCommentInlayHints(Editor editor, InlayHintsSink sink,
                                          int offset, String comment, int indentWidth) {
            String[] commentLines = comment.split("\\n");

            // Add a block inlay hint for each line of the comment
            for (int i = 0; i < commentLines.length; i++) {
                String line = commentLines[i].trim();
                String prefix = i == 0 ? "🔖 " : "   ";

                // Create text presentation
                InlayPresentation textPresentation = bookmarkCommentText(editor, prefix + line);

                // Add vertical line on the left using folding presentation
                InlayPresentation withVerticalLine = getFactory().folding(
                        textPresentation,
                        () -> textPresentation  // Placeholder when collapsed (not used here)
                );

                // Add left padding to align with the code indentation
                InlayPresentation presentation = getFactory().inset(withVerticalLine, indentWidth, 0, 0, 0);

                // All lines are added at the same offset, they will stack vertically
                sink.addBlockElement(offset, true, true, i, presentation);
            }
        }

        private InlayPresentation bookmarkCommentText(Editor editor, String text) {
            // We don't use smallText() to avoid applying INLAY_DEFAULT
            // This allows us to use our custom BOOKMARK_COMMENT_KEY color
            var textMetricsStorage = InlayHintsUtils.INSTANCE.getTextMetricStorage(editor);
            InlayPresentation textPresentation = new InsetPresentation(
                    new TextInlayPresentation(textMetricsStorage, true, text),
                    1, 0, 1, 0
            );

            // Apply our custom orange color using BOOKMARK_COMMENT_KEY
            // The color is defined in colorSchemes/BookmarkCommentDefault.xml and BookmarkCommentDarcula.xml
            InlayPresentation coloredPresentation = new WithAttributesPresentation(
                    textPresentation,
                    BOOKMARK_COMMENT_KEY,
                    editor,
                    new WithAttributesPresentation.AttributesFlags()
            );
            return coloredPresentation;
        }

        private int calculateIndentWidth(Editor editor, Document document, int lineNumber) {
            int lineStartOffset = document.getLineStartOffset(lineNumber);
            int lineEndOffset = document.getLineEndOffset(lineNumber);
            String lineText = document.getText().substring(lineStartOffset, lineEndOffset);

            // Get the width of a single space character in the editor
            int spaceWidth = EditorUtil.getPlainSpaceWidth(editor);
            int tabSize = editor.getSettings().getTabSize(editor.getProject());

            int totalWidth = 0;
            int column = 0;

            // Calculate width accounting for both spaces and tabs
            for (char c : lineText.toCharArray()) {
                if (c == ' ') {
                    totalWidth += spaceWidth;
                    column++;
                } else if (c == '\t') {
                    // Tab advances to the next tab stop
                    int spacesToNextTabStop = tabSize - (column % tabSize);
                    totalWidth += spacesToNextTabStop * spaceWidth;
                    column += spacesToNextTabStop;
                } else {
                    // First non-whitespace character found
                    break;
                }
            }

            return totalWidth;
        }
    }
}

