package mesfavoris.texteditor.internal;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.placeholders.IPathPlaceholderResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_FILE_PATH;

public class TextEditorBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

	private final IPathPlaceholderResolver pathPlaceholderResolver;

	public TextEditorBookmarkLabelProvider(IPathPlaceholderResolver pathPlaceholderResolver) {
		this.pathPlaceholderResolver = pathPlaceholderResolver;
	}

	@Override
	public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
		String filePath = bookmark.getPropertyValue(PROP_FILE_PATH);
		// Try to expand placeholders first
		Path path = pathPlaceholderResolver.expand(filePath);
		if (path == null) {
			// Placeholder not resolved, use the path as-is to get icon from extension
			path = Paths.get(filePath);
		}
        VirtualFile file = LocalFileSystem.getInstance().findFileByNioFile(path);
        if (file != null) {
            return IconUtil.getIcon(file, 0, project);
        }
        // File doesn't exist, get icon based on file extension
        String fileName = path.getFileName().toString();
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        return fileType.getIcon();
    }

    @Override
    public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
        return bookmark.getPropertyValue(PROP_FILE_PATH) != null;
    }

}
