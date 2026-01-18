package mesfavoris.internal.markers;

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.InlayHintsUtils;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import mesfavoris.markers.IBookmarksHighlighters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static com.intellij.codeInsight.hints.presentation.MouseButton.Left;

class BookmarkCommentInlayHintsCollector extends FactoryInlayHintsCollector {
    private final Project project;
    private final IBookmarksHighlighters bookmarksHighlighters;
    private final BookmarksTree bookmarksTree;

    public BookmarkCommentInlayHintsCollector(@NotNull Project project,
                                              @NotNull Editor editor,
                                              @NotNull IBookmarksHighlighters bookmarksHighlighters,
                                              @NotNull BookmarksTree bookmarksTree) {
        super(editor);
        this.project = project;
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
            int priority = 0;
            for (int i = 0; i < bookmarkIds.size(); i++) {
                BookmarkId bookmarkId = bookmarkIds.get(i);
                Bookmark bookmark = bookmarksTree.getBookmark(bookmarkId);

                if (bookmark == null) {
                    continue;
                }

                String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
                if (comment == null || comment.trim().isEmpty()) {
                    continue;
                }

                boolean isLastBookmark = (i == bookmarkIds.size() - 1);
                addCommentInlayHints(editor, sink, lineStartOffset, bookmarkId, comment, indentWidth, priority, isLastBookmark);
                priority++;
            }
        }

        return true;
    }

    private void addCommentInlayHints(Editor editor, InlayHintsSink sink,
                                      int offset, BookmarkId bookmarkId, String comment, int indentWidth, int priority, boolean isLastBookmark) {
        String[] commentLines = Arrays.stream(comment.split("\\n"))
                .map(String::trim)
                .toArray(String[]::new);
        var metricsStorage = InlayHintsUtils.INSTANCE.getTextMetricStorage(editor);
        InlayPresentation commentPresentation = new BookmarkCommentInlayPresentation(
                metricsStorage,
                editor.getColorsScheme(),
                commentLines
        );

        // Wrap with click handler for double-click to select bookmark in tree
        InlayPresentation clickablePresentation = getFactory().onClick(commentPresentation, Left, (event, point) -> {
            if (event.getClickCount() == 2) {
                selectBookmarkInTree(bookmarkId);
            }
            return null;
        });

        // Add cursor on hover to show pointer instead of text cursor
        InlayPresentation withCursor = getFactory().withCursorOnHover(clickablePresentation, java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));

        // Add left padding for indentation and bottom padding to separate multiple bookmarks on the same line
        // Only add bottom padding if this is not the last bookmark (to keep it close to the bookmarked line)
        int bottomPadding = isLastBookmark ? 0 : 4;
        InlayPresentation presentation = getFactory().inset(withCursor, indentWidth, 0, 0, bottomPadding);

        // Add a single block element for all comment lines
        sink.addBlockElement(offset, true, true, priority, presentation);
    }

    private void selectBookmarkInTree(BookmarkId bookmarkId) {
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        if (bookmarksService != null) {
            bookmarksService.selectBookmarkInTree(bookmarkId);
        }
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
