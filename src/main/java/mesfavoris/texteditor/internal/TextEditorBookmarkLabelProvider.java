package mesfavoris.texteditor.internal;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile;
import com.intellij.util.IconUtil;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.model.Bookmark;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_FILE_PATH;

public class TextEditorBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

	@Override
	public Icon getIcon(Context context, Bookmark bookmark) {
    	Path path = Paths.get(bookmark.getPropertyValue(PROP_FILE_PATH));
        VirtualFile file = new CoreLocalVirtualFile(new CoreLocalFileSystem(), new File(path.getFileName().toString()));
        return IconUtil.getIcon(file, 0, context.get(Context.PROJECT));
    }

    @Override
    public boolean canHandle(Context context, Bookmark bookmark) {
        return bookmark.getPropertyValue(PROP_FILE_PATH) != null;
    }

}
