package mesfavoris.texteditor.internal;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import mesfavoris.model.Bookmark;
import mesfavoris.path.internal.resource.PathDistanceComputer;
import mesfavoris.path.resource.FuzzyResourceFinder;
import mesfavoris.path.resource.IPathDistanceComputer;
import mesfavoris.texteditor.TextEditorBookmarkProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_WORKSPACE_PATH;

public class WorkspaceFileBookmarkLocationProvider extends AbstractFileBookmarkLocationProvider {
    private final FileDocumentManager fileDocumentManager;

    public WorkspaceFileBookmarkLocationProvider() {
        this.fileDocumentManager = FileDocumentManager.getInstance();
    }


    @Override
    public WorkspaceFileBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator monitor) {
        Optional<VirtualFile> workspaceFile = getWorkspaceFile(project, bookmark, monitor);
        if (!workspaceFile.isPresent()) {
            return null;
        }
        String lineContent = bookmark.getPropertyValue(TextEditorBookmarkProperties.PROP_LINE_CONTENT);
        Integer lineNumber = getExpectedLineNumber(bookmark);
        Integer lineOffset = null;
        Document document = fileDocumentManager.getDocument(workspaceFile.get());
        if (lineContent != null && document != null) {
            lineNumber = getLineNumber(document, lineNumber, lineContent, monitor);
        }
        if (document != null && lineNumber != null) {
            lineOffset = getLineOffset(document, lineNumber);
        }
        return new WorkspaceFileBookmarkLocation(workspaceFile.get(), lineNumber, lineOffset);
    }

    private Integer getLineOffset(Document document, int lineNumber) {
        try {
            return document.getLineStartOffset(lineNumber);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private Optional<VirtualFile> getWorkspaceFile(Project project, Bookmark bookmark, ProgressIndicator progress) {
        String workspacePath = bookmark.getPropertyValue(PROP_WORKSPACE_PATH);
        if (workspacePath == null) {
            return Optional.empty();
        }
        Path expectedPath = Paths.get(workspacePath);
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
        FuzzyResourceFinder.FileSystemItemPathProvider fileSystemItemPathProvider = (item) -> {
            Module module = projectFileIndex.getModuleForFile(item.getVirtualFile());
            if (module == null) {
                return Optional.empty();
            }
            VirtualFile contentRoot = projectFileIndex.getContentRootForFile(item.getVirtualFile());
            Path contentRootPath = Paths.get(contentRoot.getPath());
            Path filePath = Paths.get(item.getVirtualFile().getPath());
            Path moduleRelativePath = Paths.get(module.getName()).resolve(contentRootPath.relativize(filePath));
            return Optional.of(moduleRelativePath);
        };
        final int MODULE_DIFFERENT_PENALTY = 4;
        IPathDistanceComputer pathDistanceComputer = new PathDistanceComputer() {
            public int distance(Path path1, Path path2) {
                int distance = super.distance(path1, path2);
                if (distance != Integer.MAX_VALUE && !path1.getName(0).equals(path2.getName(0))) {
                    distance += MODULE_DIFFERENT_PENALTY;
                }
                return distance;
            }
        };
        FuzzyResourceFinder fuzzyResourceFinder = new FuzzyResourceFinder(project, FuzzyResourceFinder.searchScopeFor(project, false), fileSystemItemPathProvider, pathDistanceComputer);
        Optional<PsiFileSystemItem> fileSystemItem = fuzzyResourceFinder.find(expectedPath, false, progress);
        return fileSystemItem.map(PsiFileSystemItem::getVirtualFile);
    }

}
