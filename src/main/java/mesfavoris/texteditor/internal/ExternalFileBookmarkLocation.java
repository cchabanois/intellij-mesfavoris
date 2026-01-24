package mesfavoris.texteditor.internal;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.bookmarktype.IFileBookmarkLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class ExternalFileBookmarkLocation implements IFileBookmarkLocation {
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

	@Override
	@Nullable
	public VirtualFile getFile() {
		return LocalFileSystem.getInstance().findFileByNioFile(fileSystemPath);
	}

	public Integer getLineNumber() {
		return lineNumber;
	}
	
	public Integer getLineOffset() {
		return lineOffset;
	}
}
