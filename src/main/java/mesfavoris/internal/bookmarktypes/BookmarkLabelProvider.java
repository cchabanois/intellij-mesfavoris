package mesfavoris.internal.bookmarktypes;

import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class BookmarkLabelProvider implements IBookmarkLabelProvider {
	private final List<IBookmarkLabelProvider> bookmarkLabelProviders;

	public BookmarkLabelProvider() {
		this.bookmarkLabelProviders = new ArrayList<>();
		this.bookmarkLabelProviders.add(new DefaultBookmarkLabelProvider());
	}

	public BookmarkLabelProvider(List<IBookmarkLabelProvider> bookmarkLabelProviders) {
		this.bookmarkLabelProviders = new ArrayList<>();
		this.bookmarkLabelProviders.addAll(bookmarkLabelProviders);
		this.bookmarkLabelProviders.add(new DefaultBookmarkLabelProvider());
	}

	@Override
	public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
		Icon icon = getBookmarkLabelProvider(project, bookmark).getIcon(project, bookmark);
		if (icon == null) {
			icon = MesFavorisIcons.bookmark;
		}
		return icon;
	}

	@Override
	public StyledString getStyledText(@Nullable Project project, @NotNull Bookmark bookmark) {
		return getBookmarkLabelProvider(project, bookmark).getStyledText(project, bookmark);
	}

	private IBookmarkLabelProvider getBookmarkLabelProvider(@Nullable Project project, @NotNull Bookmark bookmark) {
		for (IBookmarkLabelProvider bookmarkLabelProvider : bookmarkLabelProviders) {
			if (bookmarkLabelProvider.canHandle(project, bookmark)) {
				return bookmarkLabelProvider;
			}
		}
		// will never happen
		return null;
	}

	@Override
	public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
		return true;
	}

	private static class DefaultBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

		@Override
		public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
			return true;
		}

	}


}
