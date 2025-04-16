package mesfavoris.texteditor;

import com.intellij.openapi.editor.Document;

public class TextEditorUtils {


	public static String getLineContent(Document document, int lineNumber) {
		if (lineNumber < 0 || lineNumber >= document.getLineCount()) {
			return null;
		}
		int lineStartOffset = document.getLineStartOffset(lineNumber);
		int lineEndOffset = document.getLineEndOffset(lineNumber);
		return document.getText().substring(lineStartOffset, lineEndOffset);
	}

}
