package mesfavoris.snippets.internal;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.bookmarktype.AbstractBookmarkPropertiesProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.snippets.SnippetBookmarkProperties;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class SnippetBookmarkPropertiesProvider extends AbstractBookmarkPropertiesProvider {
	private static final int NAME_LENGTH_LIMIT = 80;

	@Override
	public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
		String bookmarkType = dataContext.getData(BookmarksDataKeys.BOOKMARK_TYPE_DATA_KEY);
		Transferable transferable = dataContext.getData(BookmarksDataKeys.TRANSFERABLE_DATA_KEY);
		if (!"snippet".equals(bookmarkType) || transferable == null) {
			return;
		}
		String snippet = getSnippet(transferable);
		if (snippet == null) {
			return;
		}
		putIfAbsent(bookmarkProperties, Bookmark.PROPERTY_NAME, getName(snippet));
		putIfAbsent(bookmarkProperties, SnippetBookmarkProperties.PROP_SNIPPET_CONTENT, snippet);
	}

	private String getSnippet(Transferable transferable) {
		try {
			return (String) transferable.getTransferData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException | IOException e) {
			return null;
		}
	}

	public static String getName(String snippet) {
		String firstLine = getFirstNonEmptyLine(snippet).orElse("Empty snippet");
		if (firstLine.length() > NAME_LENGTH_LIMIT) {
			String ellipsis = "...";
			return firstLine.substring(0, NAME_LENGTH_LIMIT - ellipsis.length() - 1) + ellipsis;
		} else {
			return firstLine;
		}

	}

	private static Optional<String> getFirstNonEmptyLine(String snippet) {
		String[] lines = snippet.split("\\r?\\n");
		for (String line : lines) {
			if (!line.trim().isEmpty()) {
				return Optional.of(line);
			}
		}
		return Optional.empty();
	}

}
