package mesfavoris.internal.markers;

import com.intellij.codeInsight.hints.*;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import mesfavoris.markers.IBookmarksHighlighters;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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

        return new BookmarkCommentInlayHintsCollector(project, editor, bookmarksHighlighters, bookmarksTree);
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }

}

