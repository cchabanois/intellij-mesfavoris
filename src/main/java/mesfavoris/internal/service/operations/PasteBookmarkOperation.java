package mesfavoris.internal.service.operations;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.BookmarksException;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.internal.model.copy.BookmarksCopier;
import mesfavoris.internal.model.copy.NonExistingBookmarkIdProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PasteBookmarkOperation {
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarkPropertiesProvider bookmarkPropertiesProvider;
	private final Project project;

	public PasteBookmarkOperation(Project project, BookmarkDatabase bookmarkDatabase,
			IBookmarkPropertiesProvider bookmarkPropertiesProvider) {
		this.project = project;
        this.bookmarkDatabase = bookmarkDatabase;
		this.bookmarkPropertiesProvider = bookmarkPropertiesProvider;
	}

	public void paste(BookmarkId parentBookmarkId, ProgressIndicator monitor) throws BookmarksException {
		String textClipboardContents = getTextClipboardContents();
		if (textClipboardContents != null) {
			BookmarksTree bookmarksTree = getBookmarksTree(textClipboardContents);
			if (bookmarksTree != null) {
				paste(parentBookmarkId, bookmarksTree, monitor);
				return;
			}
		}
		DataContext dataContext = getDataContextFromClipboard();
		if (dataContext != null) {
			paste(parentBookmarkId, dataContext, monitor);
		}
	}

	public void pasteAfter(BookmarkId parentBookmarkId, BookmarkId bookmarkId, ProgressIndicator monitor)
			throws BookmarksException {
		String clipboardContents = getTextClipboardContents();
		BookmarksTree bookmarksTree = getBookmarksTree(clipboardContents);
		if (bookmarksTree != null) {
			pasteAfter(parentBookmarkId, bookmarkId, bookmarksTree, monitor);
			return;
		}
		DataContext dataContext = getDataContextFromClipboard();
		if (dataContext != null) {
			pasteAfter(parentBookmarkId, bookmarkId, dataContext, monitor);
		}
	}

	private String getTextClipboardContents() {
		Transferable transferable = CopyPasteManager.getInstance().getContents();
		if (transferable == null) {
			return null;
		}
		try {
			return (String) transferable.getTransferData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException | IOException e) {
			return null;
		}
	}

	private DataContext getDataContextFromClipboard() {
		Transferable transferable = CopyPasteManager.getInstance().getContents();
		if (transferable == null) {
			return null;
		}
		return SimpleDataContext.builder()
				.add(CommonDataKeys.PROJECT, project)
				.add(BookmarksDataKeys.TRANSFERABLE_DATA_KEY, transferable)
				.build();
	}

	private BookmarksTree getBookmarksTree(String clipboardContents) {
		if (clipboardContents == null) {
			return null;
		}
		BookmarksTreeJsonDeserializer deserializer = new BookmarksTreeJsonDeserializer();
		try {
			return deserializer.deserialize(new StringReader(clipboardContents));
		} catch (IOException e) {
			// clipboard does not contain bookmarks
			return null;
		}
	}

	private void paste(BookmarkId parentBookmarkId, BookmarksTree sourceBookmarksTree, ProgressIndicator monitor)
			throws BookmarksException {
		monitor.setText("Pasting bookmarks");
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			BookmarksCopier bookmarksCopier = new BookmarksCopier(sourceBookmarksTree,
					new NonExistingBookmarkIdProvider(bookmarksTreeModifier.getCurrentTree()));
			List<BookmarkId> bookmarkIds = sourceBookmarksTree.getChildren(sourceBookmarksTree.getRootFolder().getId())
					.stream().map(Bookmark::getId).collect(Collectors.toList());
			bookmarksCopier.copy(bookmarksTreeModifier, parentBookmarkId, bookmarkIds);
		});
	}

	private void pasteAfter(BookmarkId parentBookmarkId, BookmarkId bookmarkId, BookmarksTree sourceBookmarksTree,
			ProgressIndicator monitor) throws BookmarksException {
		monitor.setText("Pasting bookmarks");
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			BookmarksCopier bookmarksCopier = new BookmarksCopier(sourceBookmarksTree,
					new NonExistingBookmarkIdProvider(bookmarksTreeModifier.getCurrentTree()));
			List<BookmarkId> bookmarkIds = sourceBookmarksTree.getChildren(sourceBookmarksTree.getRootFolder().getId())
					.stream().map(Bookmark::getId).collect(Collectors.toList());
			bookmarksCopier.copyAfter(bookmarksTreeModifier, parentBookmarkId, bookmarkId, bookmarkIds);
		});
	}

	private void paste(BookmarkId parentBookmarkId, DataContext dataContext, ProgressIndicator monitor)
			throws BookmarksException {
		Bookmark bookmark = createBookmark(dataContext, monitor);
		if (bookmark != null) {
			bookmarkDatabase.modify(bookmarksTreeModifier -> {
				bookmarksTreeModifier.addBookmarks(parentBookmarkId, Collections.singletonList(bookmark));
			});
		}
	}

	private void pasteAfter(BookmarkId parentBookmarkId, BookmarkId bookmarkId, DataContext dataContext,
			ProgressIndicator monitor) throws BookmarksException {
		Bookmark bookmark = createBookmark(dataContext, monitor);
		if (bookmark != null) {
			bookmarkDatabase.modify(bookmarksTreeModifier -> {
				bookmarksTreeModifier.addBookmarksAfter(parentBookmarkId, bookmarkId, Collections.singletonList(bookmark));
			});
		}
	}

	private Bookmark createBookmark(DataContext dataContext, ProgressIndicator monitor) {
		Map<String, String> bookmarkProperties = new HashMap<>();
		bookmarkPropertiesProvider.addBookmarkProperties(bookmarkProperties, dataContext, monitor);
		if (bookmarkProperties.isEmpty()) {
			return null;
		}
		return new Bookmark(new BookmarkId(), bookmarkProperties);
	}

}
