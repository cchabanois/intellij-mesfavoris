package mesfavoris.bookmarktype;

import com.intellij.openapi.project.Project;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AbstractBookmarkLabelProvider implements IBookmarkLabelProvider {

	@Override
	public StyledString getStyledText(@Nullable Project project, @NotNull Bookmark bookmark) {
		String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
		if (name == null) {
			name = "unnamed";
		}
		return new StyledString(name);
	}

	@Override
	public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
		return MesFavorisIcons.bookmark;
	}

}
