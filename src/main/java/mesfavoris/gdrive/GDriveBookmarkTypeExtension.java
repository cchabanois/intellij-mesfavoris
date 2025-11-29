package mesfavoris.gdrive;

import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.gdrive.decoration.GDriveBookmarkFolderLabelProvider;

public class GDriveBookmarkTypeExtension extends AbstractBookmarkTypeExtension  {
    public static final String BOOKMARK_TYPE_NAME = "gdrive";

    public GDriveBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, "Bookmarks for GDrive documents", GDriveIcons.GDRIVE);
        addLabelProvider(new GDriveBookmarkFolderLabelProvider(), 8);
    }
}
