package mesfavoris.texteditor.internal;

import mesfavoris.bookmarktype.IBookmarkLocation;

import java.nio.file.Path;

public class ExternalFileBookmarkLocation implements IBookmarkLocation {
	private final Path fileSystemPath;
	private final Integer lineNumber;
	private final Integer lineOffset;
	
	public ExternalFileBookmarkLocation(Path fileSystemPath, Integer lineNumber, Integer lineOffset) {
		this.fileSystemPath = fileSystemPath;
		this.lineNumber = lineNumber;
		this.lineOffset = lineOffset;
	}

	public Path getFileSystemPath() {
		return fileSystemPath;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}
	
	public Integer getLineOffset() {
		return lineOffset;
	}
}
