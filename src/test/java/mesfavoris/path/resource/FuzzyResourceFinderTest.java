package mesfavoris.path.resource;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.path.internal.resource.PathDistanceComputer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FuzzyResourceFinderTest extends BasePlatformTestCase {

    private FuzzyResourceFinder fuzzyResourceFinder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Copy test data to project
        myFixture.copyDirectoryToProject("commons-cli", "commons-cli");

        // Create the FileSystemItemPathProvider
        FuzzyResourceFinder.FileSystemItemPathProvider fileSystemItemPathProvider =
            fileSystemItem -> {
                VirtualFile virtualFile = fileSystemItem.getVirtualFile();
                if (virtualFile == null) {
                    return Optional.empty();
                }
                return Optional.of(Paths.get(virtualFile.getPath()));
            };

        // Create the FuzzyResourceFinder with required dependencies
        fuzzyResourceFinder = new FuzzyResourceFinder(
            getProject(),
            GlobalSearchScope.projectScope(getProject()),
            fileSystemItemPathProvider,
            new PathDistanceComputer()
        );
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    public void testExactPath() {
        // Given
        Path expectedPath = Paths.get("commons-cli/NOTICE.txt");

        // When
        Optional<PsiFileSystemItem> result = fuzzyResourceFinder.find(expectedPath, false, new EmptyProgressIndicator());

        // Then
        assertThat(result).isPresent();
        VirtualFile virtualFile = result.get().getVirtualFile();
        assertThat(virtualFile).isNotNull();
        assertThat(virtualFile.getPath()).endsWith("commons-cli/NOTICE.txt");
    }

    public void testFuzzyPath() {
        // Given - looking for CommandLine.java with a partial path
        Path expectedPath = Paths.get("org/apache/commons/cli/CommandLine.java");

        // When
        Optional<PsiFileSystemItem> result = fuzzyResourceFinder.find(expectedPath, false, new EmptyProgressIndicator());

        // Then
        assertThat(result).isPresent();
        VirtualFile virtualFile = result.get().getVirtualFile();
        assertThat(virtualFile).isNotNull();
        assertThat(virtualFile.getPath()).endsWith("src/main/java/org/apache/commons/cli/CommandLine.java");
    }

    public void testFileNotFound() {
        // Given
        Path expectedPath = Paths.get("nonexistent/file.txt");

        // When
        Optional<PsiFileSystemItem> result = fuzzyResourceFinder.find(expectedPath, false, new EmptyProgressIndicator());

        // Then
        assertThat(result).isEmpty();
    }
}
