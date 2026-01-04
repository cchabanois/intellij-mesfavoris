package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.internal.settings.MesFavorisSettingsStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for KeymapConfigurator.
 *
 * Note: These tests verify that KeymapConfigurator correctly responds to settings changes
 * by replacing/restoring IntelliJ bookmark actions.
 */
public class KeymapConfiguratorTest extends BasePlatformTestCase {

    private static final String INTELLIJ_TOGGLE_BOOKMARK = "ToggleBookmark";
    private static final String INTELLIJ_SHOW_BOOKMARKS = "ShowBookmarks";

    private MesFavorisSettingsStore settingsStore;
    private KeymapConfigurator configurator;
    private boolean initialSetting;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        settingsStore = MesFavorisSettingsStore.getInstance();
        // Save initial setting
        initialSetting = settingsStore.isUseIntellijBookmarkShortcuts();

        // Initialize KeymapConfigurator
        configurator = KeymapConfigurator.getInstance();
        configurator.init();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            // Restore initial setting
            settingsStore.setUseIntellijBookmarkShortcuts(initialSetting);

            // Dispose configurator to clean up resources
            if (configurator != null) {
                configurator.dispose();
            }
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testActionsReplacedWhenUseIntellijShortcutsIsTrue() {
        // Given
        ActionManager actionManager = ActionManager.getInstance();

        // When - set to use IntelliJ shortcuts (true)
        // This should replace IntelliJ actions with Mesfavoris actions
        settingsStore.setUseIntellijBookmarkShortcuts(true);

        // Then - verify actions are replaced with Mesfavoris actions
        AnAction toggleAction = actionManager.getAction(INTELLIJ_TOGGLE_BOOKMARK);
        AnAction showAction = actionManager.getAction(INTELLIJ_SHOW_BOOKMARKS);

        assertThat(toggleAction).isNotNull();
        assertThat(showAction).isNotNull();
        assertThat(toggleAction).isInstanceOf(AddBookmarkAction.class);
        assertThat(showAction).isInstanceOf(ShowBookmarksAction.class);
    }

}


