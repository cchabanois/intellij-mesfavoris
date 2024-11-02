package mesfavoris.path.resource;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import mesfavoris.path.internal.resource.PathDistanceComputer;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Find a resource. Name has to be the same than the given one but path can be
 * different. A resource with same name and the most similar path will be returned.
 *
 * @author cchabanois
 */
public class FuzzyResourceFinder {
    private final Project project;
    private final GlobalSearchScope searchScope;
    private final FileSystemItemPathProvider fileSystemItemPathProvider;
    private final IPathDistanceComputer pathDistanceComputer;

    public FuzzyResourceFinder(Project project, GlobalSearchScope searchScope, FileSystemItemPathProvider fileSystemItemPathProvider, IPathDistanceComputer pathDistanceComputer) {
        this.project = project;
        this.searchScope = searchScope;
        this.fileSystemItemPathProvider = fileSystemItemPathProvider;
        this.pathDistanceComputer = pathDistanceComputer;
    }

    public static GlobalSearchScope searchScopeFor(Project project, boolean searchInLibraries) {
        return searchInLibraries ? ProjectScope.getAllScope(project) :
                ProjectScope.getProjectScope(project);
    }

    public Optional<PsiFileSystemItem> find(Path expectedPath, boolean directories, ProgressIndicator canceled) {
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
        FindResourceProcessor findResourceProcessor = new FindResourceProcessor(expectedPath, canceled);

        GlobalSearchScope searchScope = FindSymbolParameters.searchScopeFor(project, true);
        FilenameIndex.processFilesByName(
                expectedPath.getFileName().toString(), directories, findResourceProcessor, searchScope, project, null
        );
        return findResourceProcessor.getBestMatchingFileSystemItem();
    }

    @FunctionalInterface
    public static interface FileSystemItemPathProvider {
        Optional<Path> getPath(PsiFileSystemItem fileSystemItem);
    }

    private class FindResourceProcessor implements Processor<PsiFileSystemItem> {
        private final Path expectedPath;
        private final ProgressIndicator canceled;
        private int resourceCandidateDistance = Integer.MAX_VALUE;
        private PsiFileSystemItem fileSystemItemCandidate;

        public FindResourceProcessor(Path expectedPath, ProgressIndicator canceled) {
            this.expectedPath = expectedPath;
            this.canceled = canceled;
        }


        @Override
        public boolean process(PsiFileSystemItem item) {
            canceled.checkCanceled();
            Optional<Path> optionalPath = fileSystemItemPathProvider.getPath(item);
            if (!optionalPath.isPresent()) {
                return true;
            }
            Path path = optionalPath.get();
            int distance = pathDistanceComputer.distance(expectedPath, path);
            if (distance < resourceCandidateDistance) {
                fileSystemItemCandidate = item;
                resourceCandidateDistance = distance;
            }
            return true;
        }

        public Optional<PsiFileSystemItem> getBestMatchingFileSystemItem() {
            if (fileSystemItemCandidate == null) {
                return Optional.empty();
            }
            return Optional.of(fileSystemItemCandidate);
        }

    }

}
