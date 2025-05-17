package mesfavoris.texteditor.internal;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.model.Bookmark;
import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.texteditor.TextEditorBookmarkProperties;
import mesfavoris.texteditor.text.DocumentUtils;

import java.nio.file.Path;
import java.util.Optional;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_FILE_PATH;

public class ExternalFileBookmarkLocationProvider extends AbstractFileBookmarkLocationProvider {

	private final IPathPlaceholderResolver pathPlaceholderResolver;

	public ExternalFileBookmarkLocationProvider(IPathPlaceholderResolver pathPlaceholderResolver) {
		this.pathPlaceholderResolver = pathPlaceholderResolver;
	}


	@Override
	public IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress) {
		Path filePath = getFilePath(bookmark);
		if (filePath == null || !filePath.toFile().exists()) {
			return null;
		}
		String lineContent = bookmark.getPropertyValue(TextEditorBookmarkProperties.PROP_LINE_CONTENT);
		Integer lineNumber = getExpectedLineNumber(bookmark);
		Integer lineOffset = null;
		Optional<Document> document = getDocument(filePath);
		if (lineContent != null && document.isPresent()) {
			lineNumber = getLineNumber(document.get(), lineNumber, lineContent, progress);
		}
		if (document.isPresent() && lineNumber != null) {
			lineOffset = getLineOffset(document.get(), lineNumber);
		}
		return new ExternalFileBookmarkLocation(filePath, lineNumber, lineOffset);
	}

	private Path getFilePath(Bookmark bookmark) {
		String nonExpandedFilePath = bookmark.getPropertyValue(PROP_FILE_PATH);
        return nonExpandedFilePath != null ? pathPlaceholderResolver.expand(nonExpandedFilePath) : null;
	}
	
	private Optional<Document> getDocument(Path filePath) {
		return Optional.ofNullable(DocumentUtils.getDocument(filePath));
	}



}
