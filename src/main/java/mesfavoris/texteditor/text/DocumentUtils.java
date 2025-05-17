package mesfavoris.texteditor.text;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class DocumentUtils {

	public static @Nullable Document getDocument(Path filePath) {
		VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByNioFile(filePath);

		if (virtualFile == null) {
			return null;
		}
		Computable<Document> computable = () -> FileDocumentManager.getInstance().getDocument(virtualFile);
		return ApplicationManager.getApplication().runReadAction(computable);
	}

}
