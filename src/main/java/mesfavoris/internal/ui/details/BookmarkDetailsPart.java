package mesfavoris.internal.ui.details;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import mesfavoris.extensions.BookmarkTypeExtensionManager;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.IBookmarksService;
import mesfavoris.ui.details.IBookmarkDetailPart;

import javax.swing.*;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

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

	/**
	 * Constructor that creates detail parts from extensions
	 */
	public BookmarkDetailsPart(Project project, Disposable parentDisposable) {
		this(project, project.getService(IBookmarksService.class).getBookmarkDatabase(), BookmarkTypeExtensionManager.getInstance().createDetailParts(project), parentDisposable);
	}

    public BookmarkDetailsPart(Project project, BookmarkDatabase bookmarkDatabase, List<IBookmarkDetailPart> bookmarkDetailParts, Disposable parentDisposable) {
        this.project = project;
        this.bookmarkDetailParts = bookmarkDetailParts;
        this.bookmarkDatabase = bookmarkDatabase;
        Disposer.register(parentDisposable, this);
    }

	@Override
	public void init() {
		for (IBookmarkDetailPart bookmarkDetailPart : bookmarkDetailParts) {
			try {
				bookmarkDetailPart.init();
				Disposer.register(this, bookmarkDetailPart);
			} catch (Exception e) {
				LOG.error("Error while initializing bookmarkDetailPart", e);
			}
		}
	}

	@Override
	public JComponent createComponent() {
		this.tabs = JBTabsFactory.createTabs(project);
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
        if (tabs.getTabCount() > 0) {
            tabs.select(Objects.requireNonNullElseGet(selectedTabItem, () -> tabs.getTabAt(0)), false);
        }
	}
	
	@Override
	public boolean canHandle(Bookmark bookmark) {
		return true;
	}

	@Override
	public void dispose() {
		// Disposer will automatically dispose all registered child disposables
	}

	@Override
	public String getTitle() {
		return "Bookmark Details";
	}
	
}
