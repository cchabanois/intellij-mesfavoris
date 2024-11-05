package mesfavoris.internal.ui.details;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import mesfavoris.service.BookmarksService;

import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.ui.details.IBookmarkDetailPart;

import javax.swing.*;

/**
 * Tab folder for bookmark details (comments ...)
 * 
 * @author cchabanois
 */
public class BookmarkDetailsPart implements IBookmarkDetailPart {
	private static final Logger LOG = Logger.getInstance(BookmarkDetailsPart.class);
	private final List<IBookmarkDetailPart> bookmarkDetailParts;
	private final BookmarkDatabase bookmarkDatabase;
	private final Project project;
	private JBTabs tabFolder;
	private final IdentityHashMap<TabInfo, IBookmarkDetailPart> tabItem2BookmarkDetailPart = new IdentityHashMap<>();
	
	public BookmarkDetailsPart(Project project, List<IBookmarkDetailPart> bookmarkDetailParts) {
		this.project = project;
		this.bookmarkDetailParts = bookmarkDetailParts;
		BookmarksService bookmarksService = project.getService(BookmarksService.class);
		this.bookmarkDatabase = bookmarksService.getBookmarkDatabase();
	}
	
	@Override
	public JComponent createComponent() {
		this.tabFolder = new JBTabsImpl(project);
		return tabFolder.getComponent();
	}

	private TabInfo createTabItem(IBookmarkDetailPart bookmarkDetailPart) {
		TabInfo tabInfo = new TabInfo(bookmarkDetailPart.createComponent());
		tabInfo.setText(bookmarkDetailPart.getTitle());
		return tabInfo;
	}
	
	@Override
	public void setBookmark(Bookmark bookmark) {
		if (bookmark != null && bookmarkDatabase.getBookmarksTree().getBookmark(bookmark.getId()) == null) {
			bookmark = null;
		}
		IBookmarkDetailPart previouslySelectedBookmarkDetailsPart = tabItem2BookmarkDetailPart.get(tabFolder.getSelectedInfo());
		tabItem2BookmarkDetailPart.clear();
		tabFolder.removeAllTabs();
		TabInfo selectedTabItem = null;
		for (IBookmarkDetailPart bookmarkDetailPart : bookmarkDetailParts) {
			if (bookmarkDetailPart.canHandle(bookmark)) {
				TabInfo tabItem = createTabItem(bookmarkDetailPart);
				tabFolder.addTab(tabItem);
				tabItem2BookmarkDetailPart.put(tabItem, bookmarkDetailPart);
				bookmarkDetailPart.setBookmark(bookmark);
				if (previouslySelectedBookmarkDetailsPart == bookmarkDetailPart) {
					selectedTabItem = tabItem;
				}
			}
		}
        tabFolder.select(Objects.requireNonNullElseGet(selectedTabItem, () -> tabFolder.getTabAt(0)), false);
	}
	
	@Override
	public boolean canHandle(Bookmark bookmark) {
		return true;
	}

	@Override
	public void dispose() {
		for (IBookmarkDetailPart bookmarkDetailPart : bookmarkDetailParts) {
			try {
				bookmarkDetailPart.dispose();
			} catch (Exception e) {
				LOG.error("Error while disposing bookmarkDetailPart", e);
			}
		}
	}

	@Override
	public String getTitle() {
		return "Bookmark Details";
	}
	
}
