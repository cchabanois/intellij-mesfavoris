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
	private JBTabs tabs;
	private final IdentityHashMap<TabInfo, IBookmarkDetailPart> tabItem2BookmarkDetailPart = new IdentityHashMap<>();
	
	public BookmarkDetailsPart(Project project, List<IBookmarkDetailPart> bookmarkDetailParts) {
		this.project = project;
		this.bookmarkDetailParts = bookmarkDetailParts;
		BookmarksService bookmarksService = project.getService(BookmarksService.class);
		this.bookmarkDatabase = bookmarksService.getBookmarkDatabase();
	}

	@Override
	public void init() {
		for (IBookmarkDetailPart bookmarkDetailPart : bookmarkDetailParts) {
			try {
				bookmarkDetailPart.init();
			} catch (Exception e) {
				LOG.error("Error while initializing bookmarkDetailPart", e);
			}
		}
	}

	@Override
	public JComponent createComponent() {
		this.tabs = new JBTabsImpl(project);
		return tabs.getComponent();
	}

	private TabInfo createTabInfo(IBookmarkDetailPart bookmarkDetailPart) {
		TabInfo tabInfo = new TabInfo(bookmarkDetailPart.createComponent());
		tabInfo.setText(bookmarkDetailPart.getTitle());
		return tabInfo;
	}
	
	@Override
	public void setBookmark(Bookmark bookmark) {
		if (bookmark != null && bookmarkDatabase.getBookmarksTree().getBookmark(bookmark.getId()) == null) {
			bookmark = null;
		}
		IBookmarkDetailPart previouslySelectedBookmarkDetailsPart = tabItem2BookmarkDetailPart.get(tabs.getSelectedInfo());
		tabItem2BookmarkDetailPart.clear();
		tabs.removeAllTabs();
		TabInfo selectedTabItem = null;
		for (IBookmarkDetailPart bookmarkDetailPart : bookmarkDetailParts) {
			if (bookmarkDetailPart.canHandle(bookmark)) {
				TabInfo tabInfo = createTabInfo(bookmarkDetailPart);
				tabs.addTab(tabInfo);
				tabItem2BookmarkDetailPart.put(tabInfo, bookmarkDetailPart);
				bookmarkDetailPart.setBookmark(bookmark);
				if (previouslySelectedBookmarkDetailsPart == bookmarkDetailPart) {
					selectedTabItem = tabInfo;
				}
			}
		}
        tabs.select(Objects.requireNonNullElseGet(selectedTabItem, () -> tabs.getTabAt(0)), false);
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
