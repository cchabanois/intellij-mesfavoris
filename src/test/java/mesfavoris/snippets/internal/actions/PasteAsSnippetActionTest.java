package mesfavoris.snippets.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.bookmarktype.IDisabledBookmarkTypesProvider;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.datatransfer.StringSelection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for PasteAsSnippetAction visibility based on snippet bookmark type enabled state
 */
public class PasteAsSnippetActionTest extends BasePlatformTestCase {

    @Mock
    private IDisabledBookmarkTypesProvider mockDisabledTypesProvider;

    private PasteAsSnippetAction action;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);

        action = new PasteAsSnippetAction(mockDisabledTypesProvider);
    }

    public void testUpdateWhenSnippetTypeDisabled() {
        // Given - snippet bookmark type is disabled
        when(mockDisabledTypesProvider.isBookmarkTypeEnabled("snippet")).thenReturn(false);

        // Set up clipboard with valid content
        CopyPasteManager.getInstance().setContents(new StringSelection("test content"));

        // Create a real AnActionEvent for testing
        AnActionEvent event = createAnActionEvent();

        // When
        action.update(event);

        // Then - action should not be visible
        assertThat(event.getPresentation().isEnabledAndVisible()).isFalse();
    }

    public void testUpdateWhenSnippetTypeEnabledAndValidContent() {
        // Given - snippet bookmark type is enabled
        when(mockDisabledTypesProvider.isBookmarkTypeEnabled("snippet")).thenReturn(true);

        // Set up clipboard with valid content
        CopyPasteManager.getInstance().setContents(new StringSelection("test content"));

        // Create a real AnActionEvent for testing
        AnActionEvent event = createAnActionEvent();

        // When
        action.update(event);

        // Then - action should be visible and enabled
        assertThat(event.getPresentation().isEnabledAndVisible()).isTrue();
    }

    public void testUpdateWhenNoValidContent() {
        // Given - snippet bookmark type is enabled
        when(mockDisabledTypesProvider.isBookmarkTypeEnabled("snippet")).thenReturn(true);

        // Set up clipboard with empty content (still valid string content)
        CopyPasteManager.getInstance().setContents(new StringSelection(""));

        // Create a real AnActionEvent for testing
        AnActionEvent event = createAnActionEvent();

        // When
        action.update(event);

        // Then - action should be visible and enabled (empty string is still valid)
        assertThat(event.getPresentation().isEnabledAndVisible()).isTrue();
    }

    private AnActionEvent createAnActionEvent() {
        return AnActionEvent.createFromDataContext("test", new Presentation(),
                com.intellij.openapi.actionSystem.impl.SimpleDataContext.builder()
                        .add(CommonDataKeys.PROJECT, getProject())
                        .build());
    }
}
