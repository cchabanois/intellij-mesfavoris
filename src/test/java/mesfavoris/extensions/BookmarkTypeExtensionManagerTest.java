package mesfavoris.extensions;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.bookmarktype.*;
import mesfavoris.ui.details.IBookmarkDetailPart;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for BookmarkTypeExtensionManager
 */
public class BookmarkTypeExtensionManagerTest extends BasePlatformTestCase {

    private BookmarkTypeExtensionManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new BookmarkTypeExtensionManager();
    }

    public void testGetAllBookmarkTypes() {
        // Given - extensions are loaded automatically

        // When
        var bookmarkTypes = manager.getAllBookmarkTypes();

        // Then
        assertThat(bookmarkTypes)
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(3)
            .extracting(BookmarkTypeExtension::getName)
            .contains("textEditor", "url", "snippet");
    }

    public void testGetBookmarkType() {
        // When
        Optional<BookmarkTypeExtension> textEditorType = manager.getBookmarkType("textEditor");
        Optional<BookmarkTypeExtension> nonExistentType = manager.getBookmarkType("nonExistent");

        // Then
        assertThat(textEditorType)
            .isPresent()
            .get()
            .extracting(BookmarkTypeExtension::getName)
            .isEqualTo("textEditor");

        assertThat(nonExistentType)
            .isEmpty();
    }

    public void testGetAllPropertiesProviders() {
        // When
        List<IBookmarkPropertiesProvider> providers = manager.getAllPropertiesProviders();

        // Then
        assertThat(providers)
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(3) // At least TextEditor, URL, and Snippet providers
            .allSatisfy(provider -> assertThat(provider).isNotNull());

        // Verify that we have the expected built-in providers by checking their class names
        List<String> providerClassNames = providers.stream()
            .map(provider -> provider.getClass().getSimpleName())
            .toList();

        assertThat(providerClassNames)
            .contains(
                "TextEditorBookmarkPropertiesProvider",
                "UrlBookmarkPropertiesProvider",
                "SnippetBookmarkPropertiesProvider"
            );
    }

    public void testGetAllLabelProviders() {
        // When
        List<IBookmarkLabelProvider> providers = manager.getAllLabelProviders();

        // Then
        assertThat(providers)
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(4) // TextEditor, URL, Snippet, and BookmarkFolder label providers
            .allSatisfy(provider -> assertThat(provider).isNotNull());

        // Verify that we have the expected built-in providers by checking their class names
        List<String> providerClassNames = providers.stream()
            .map(provider -> provider.getClass().getSimpleName())
            .toList();

        assertThat(providerClassNames)
            .contains(
                "TextEditorBookmarkLabelProvider",
                "UrlBookmarkLabelProvider",
                "SnippetBookmarkLabelProvider",
                "BookmarkFolderLabelProvider"
            );
    }

    public void testGetAllGotoBookmarkHandlers() {
        // When
        List<IGotoBookmark> handlers = manager.getAllGotoBookmarkHandlers();

        // Then
        assertThat(handlers)
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(4) // TextEditor (2), URL (1), Snippet (1)
            .allSatisfy(handler -> assertThat(handler).isNotNull());

        // Verify that we have the expected built-in handlers by checking their class names
        List<String> handlerClassNames = handlers.stream()
            .map(handler -> handler.getClass().getSimpleName())
            .toList();

        assertThat(handlerClassNames)
            .contains(
                "GotoWorkspaceFileBookmark",
                "GotoExternalFileBookmark",
                "GotoUrlBookmark",
                "GotoSnippetBookmark"
            );
    }

    public void testGetAllLocationProviders() {
        // When
        List<IBookmarkLocationProvider> providers = manager.getAllLocationProviders();

        // Then
        assertThat(providers)
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(4) // TextEditor (2), URL (1), Snippet (1)
            .allSatisfy(provider -> assertThat(provider).isNotNull());

        // Verify that we have the expected built-in providers by checking their class names
        List<String> providerClassNames = providers.stream()
            .map(provider -> provider.getClass().getSimpleName())
            .toList();

        assertThat(providerClassNames)
            .contains(
                "WorkspaceFileBookmarkLocationProvider",
                "ExternalFileBookmarkLocationProvider",
                "UrlBookmarkLocationProvider",
                "SnippetBookmarkLocationProvider"
            );
    }

    public void testGetAllMarkerAttributesProviders() {
        // When
        List<IBookmarkMarkerAttributesProvider> providers = manager.getAllMarkerAttributesProviders();

        // Then
        assertThat(providers).isNotNull();
    }

    public void testGetAllDetailParts() {
        // When
        List<IBookmarkDetailPart> parts = manager.getAllDetailParts();

        // Then
        assertThat(parts).isNotNull();
    }

    public void testAllExtensionsHaveValidNames() {
        // When
        var bookmarkTypes = manager.getAllBookmarkTypes();

        // Then
        assertThat(bookmarkTypes)
            .isNotEmpty()
            .allSatisfy(extension -> {
                assertThat(extension.getName())
                    .isNotNull()
                    .isNotBlank();
                assertThat(extension.getPropertyDescriptors()).isNotNull();
                assertThat(extension.getPropertiesProviders()).isNotNull();
                assertThat(extension.getLabelProviders()).isNotNull();
                assertThat(extension.getGotoBookmarkHandlers()).isNotNull();
            });
    }

    public void testGetPropertyDescriptor() {
        // Given
        Optional<BookmarkTypeExtension> textEditorType = manager.getBookmarkType("textEditor");

        // When & Then
        assertThat(textEditorType)
            .isPresent()
            .get()
            .satisfies(extension -> {
                // Test existing property
                BookmarkPropertyDescriptor filePathDescriptor = extension.getPropertyDescriptor("filePath");
                assertThat(filePathDescriptor)
                    .isNotNull()
                    .satisfies(descriptor -> {
                        assertThat(descriptor.getName()).isEqualTo("filePath");
                        assertThat(descriptor.getType()).isEqualTo(BookmarkPropertyDescriptor.BookmarkPropertyType.PATH);
                    });

                // Test non-existing property
                BookmarkPropertyDescriptor nonExistentDescriptor = extension.getPropertyDescriptor("nonExistent");
                assertThat(nonExistentDescriptor).isNull();

                // Test all expected properties exist
                assertThat(extension.getPropertyDescriptor("lineNumber")).isNotNull();
                assertThat(extension.getPropertyDescriptor("lineContent")).isNotNull();
                assertThat(extension.getPropertyDescriptor("workspacePath")).isNotNull();
            });
    }




}
