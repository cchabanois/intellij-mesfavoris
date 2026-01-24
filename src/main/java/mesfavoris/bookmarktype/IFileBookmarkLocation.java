package mesfavoris.bookmarktype;

import com.intellij.openapi.vfs.VirtualFile;

public interface IFileBookmarkLocation extends IBookmarkLocation {

    VirtualFile getFile();

    Integer getLineNumber();

}
