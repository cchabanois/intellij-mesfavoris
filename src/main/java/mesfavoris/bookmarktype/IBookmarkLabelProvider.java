package mesfavoris.bookmarktype;

import com.intellij.openapi.project.Project;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface IBookmarkLabelProvider {

	StyledString getStyledText(@Nullable Project project, @NotNull Bookmark bookmark);

	Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark);

	boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark);

}
