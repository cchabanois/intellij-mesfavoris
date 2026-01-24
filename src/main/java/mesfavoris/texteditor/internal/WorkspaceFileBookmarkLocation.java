package mesfavoris.texteditor.internal;

import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.bookmarktype.IFileBookmarkLocation;

public class WorkspaceFileBookmarkLocation implements IFileBookmarkLocation {
	private final VirtualFile file;
	private final Integer lineNumber;
	private final Integer lineOffset;
	
	public WorkspaceFileBookmarkLocation(VirtualFile file, Integer lineNumber, Integer lineOffset) {
		this.file = file;
		this.lineNumber = lineNumber;
		this.lineOffset = lineOffset;
	}

	@Override
	public VirtualFile getFile() {
		return file;
	}
	
	public Integer getLineNumber() {
		return lineNumber;
	}
	
	public Integer getLineOffset() {
		return lineOffset;
	}
}
