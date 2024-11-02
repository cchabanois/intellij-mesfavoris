package mesfavoris.texteditor.internal;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.texteditor.text.matching.DocumentFuzzySearcher;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_LINE_NUMBER;

public abstract class AbstractFileBookmarkLocationProvider implements IBookmarkLocationProvider {

	protected Integer getExpectedLineNumber(Bookmark bookmark) {
		String expectedLineNumberAsString = bookmark.getPropertyValue(PROP_LINE_NUMBER);
		if (expectedLineNumberAsString == null) {
			return null;
		}
		return Integer.parseInt(expectedLineNumberAsString);
	}	
	
	protected Integer getLineNumber(Document document, Integer expectedLineNumber, String lineContent,
									ProgressIndicator progress) {
		try {
			DocumentFuzzySearcher searcher = new DocumentFuzzySearcher(document);
			TextRange region;
			if (expectedLineNumber == null) {
				region = new TextRange(0, document.getTextLength());
			} else {
				region = getRegionAround(document, expectedLineNumber, 1000);
			}
			int lineNumber = searcher.findLineNumber(region, expectedLineNumber == null ? -1 : expectedLineNumber,
					lineContent, progress);
			if (lineNumber == -1) {
				return null;
			}
			return lineNumber;
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	private TextRange getRegionAround(Document document, int lineNumber, int linesAround) throws IndexOutOfBoundsException {
		int firstLine = lineNumber - linesAround;
		if (firstLine < 0) {
			firstLine = 0;
		}
		int lastLine = lineNumber + linesAround;
		if (lastLine >= document.getLineCount()) {
			lastLine = document.getLineCount() - 1;
		}
		int offset = document.getLineStartOffset(firstLine);
		int length = document.getLineEndOffset(lastLine) - offset;
		TextRange region = new TextRange(offset, length);
		return region;
	}	
	
}
