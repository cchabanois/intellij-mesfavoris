package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import mesfavoris.placeholders.PathPlaceholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * Modal dialog for managing placeholders from the tool window
 */
public class PlaceholdersDialog extends DialogWrapper {
    private final Project project;
    private PlaceholdersListPanel placeholdersPanel;
    private final PathPlaceholdersStore store;

    public PlaceholdersDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        this.store = PathPlaceholdersStore.getInstance();
        
        setTitle("Manage Placeholders");
        setModal(true);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        placeholdersPanel = new PlaceholdersListPanel(project); // Pass project for statistics
        
        // Load current placeholders
        List<PathPlaceholder> placeholders = store.getPlaceholders();
        placeholdersPanel.setPlaceholders(placeholders);
        
        // Set preferred size
        placeholdersPanel.setPreferredSize(JBUI.size(600, 400));
        
        return placeholdersPanel;
    }

    @Override
    protected void doOKAction() {
        // Save placeholders when OK is clicked
        List<PathPlaceholder> placeholders = placeholdersPanel.getPlaceholders();
        store.setPlaceholders(placeholders);
        
        super.doOKAction();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return placeholdersPanel;
    }
}
