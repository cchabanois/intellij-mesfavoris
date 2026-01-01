package mesfavoris.texteditor.internal;

import com.google.common.collect.ImmutableMap;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.bookmarktype.BookmarkDatabaseLabelProviderContext;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.internal.settings.placeholders.PathPlaceholdersStore;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.placeholders.PathPlaceholder;

import javax.swing.*;
import java.io.File;
import java.nio.file.Paths;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_FILE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TextEditorBookmarkLabelProviderTest extends BasePlatformTestCase {
    private TextEditorBookmarkLabelProvider labelProvider;
    private IBookmarkLabelProvider.Context context;
    private PathPlaceholdersStore placeholdersStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        placeholdersStore = new PathPlaceholdersStore();
        PathPlaceholderResolver pathPlaceholderResolver = new PathPlaceholderResolver(placeholdersStore);
        labelProvider = new TextEditorBookmarkLabelProvider(pathPlaceholderResolver);
        context = new BookmarkDatabaseLabelProviderContext(getProject(), "testDb", () -> null);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    public void testGetIconFromBookmarkWithUndefinedPlaceholder() {
        // Given
        Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(PROP_FILE_PATH, "${PLACEHOLDER}/myFile.txt"));

        // When
        Icon icon = labelProvider.getIcon(context, bookmark);

        // Then
        assertThat(icon).isNotNull();
    }

    public void testGetIconFromBookmarkWithResolvedPlaceholder() throws Exception {
        // Given - copy test data to project (real files on disk)
        myFixture.copyDirectoryToProject("labelProviderTest", "labelProviderTest");

        // Get the base path of the project
        String basePath = getProject().getBasePath();
        assertThat(basePath).isNotNull();

        File testDir = new File(basePath, "labelProviderTest");
        placeholdersStore.setPlaceholders(java.util.List.of(
                new PathPlaceholder("TESTDIR", Paths.get(testDir.getAbsolutePath()))
        ));

        Bookmark classBookmark = new Bookmark(new BookmarkId(),
                ImmutableMap.of(PROP_FILE_PATH, "${TESTDIR}/MyClass.java"));
        Bookmark interfaceBookmark = new Bookmark(new BookmarkId(),
                ImmutableMap.of(PROP_FILE_PATH, "${TESTDIR}/MyInterface.java"));

        // When
        Icon classIcon = labelProvider.getIcon(context, classBookmark);
        Icon interfaceIcon = labelProvider.getIcon(context, interfaceBookmark);

        // Then - icons should be different (class vs interface), proving placeholder was resolved
        assertThat(classIcon).isNotNull();
        assertThat(interfaceIcon).isNotNull();
        assertThat(classIcon).isNotSameAs(interfaceIcon);
    }
}
