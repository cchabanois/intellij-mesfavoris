package mesfavoris.java.internal;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static mesfavoris.java.JavaBookmarkProperties.*;

public class JavaBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

	@Override
	public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
		String kind = bookmark.getPropertyValue(PROP_JAVA_ELEMENT_KIND);
		if (KIND_METHOD.equals(kind)) {
			return AllIcons.Nodes.Method;
		}
		if (KIND_FIELD.equals(kind)) {
			return AllIcons.Nodes.Field;
		}
		if (KIND_ANNOTATION.equals(kind)) {
			return AllIcons.Nodes.Annotationtype;
		}
		if (KIND_ENUM.equals(kind)) {
			return AllIcons.Nodes.Enum;
		}
		if (KIND_INTERFACE.equals(kind)) {
			return AllIcons.Nodes.Interface;
		}
		if (KIND_CLASS.equals(kind)) {
			return AllIcons.Nodes.Class;
		}
		if (KIND_TYPE.equals(kind)) {
			return AllIcons.Nodes.Class;
		}
		return null;
	}

	@Override
	public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
		return bookmark.getPropertyValue(PROP_JAVA_ELEMENT_KIND) != null;
	}

}
