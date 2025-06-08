package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import mesfavoris.placeholders.PathPlaceholder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for editing a placeholder (name and path)
 */
public class PlaceholderEditDialog extends DialogWrapper {
    private JTextField nameField;
    private TextFieldWithBrowseButton pathField;
    private final PathPlaceholder originalPlaceholder;
    private final boolean isNewPlaceholder;
    private final List<PathPlaceholder> existingPlaceholders;

    /**
     * Constructor for adding a new placeholder
     */
    public PlaceholderEditDialog(@Nullable Project project, List<PathPlaceholder> existingPlaceholders) {
        super(project);
        this.originalPlaceholder = null;
        this.isNewPlaceholder = true;
        this.existingPlaceholders = existingPlaceholders != null ? existingPlaceholders : new ArrayList<>();

        setTitle("Add Placeholder");
        init();
    }

    /**
     * Constructor for editing an existing placeholder
     */
    public PlaceholderEditDialog(@Nullable Project project, PathPlaceholder placeholder, List<PathPlaceholder> existingPlaceholders) {
        super(project);
        if (placeholder == null) {
            throw new IllegalArgumentException("Placeholder cannot be null when editing");
        }
        this.originalPlaceholder = placeholder;
        this.isNewPlaceholder = false;
        this.existingPlaceholders = existingPlaceholders != null ? existingPlaceholders : new ArrayList<>();

        setTitle("Edit Placeholder");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 0, 5, 10);
        panel.add(new JLabel("Name:"), gbc);

        nameField = new JTextField(20);
        // Add document filter to convert to uppercase as user types
        AbstractDocument document = (AbstractDocument) nameField.getDocument();
        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                super.insertString(fb, offset, string.toUpperCase(), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                super.replace(fb, offset, length, text.toUpperCase(), attrs);
            }
        });

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insetsBottom(5);
        panel.add(nameField, gbc);

        // Path field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 0, 5, 10);
        panel.add(new JLabel("Path:"), gbc);

        pathField = new TextFieldWithBrowseButton();
        pathField.addActionListener(e -> {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            descriptor.setTitle("Select Folder");
            descriptor.setDescription("Select the folder for this placeholder");
            
            VirtualFile selectedFile = FileChooser.chooseFile(descriptor, getContentPanel(), null, null);
            if (selectedFile != null) {
                pathField.setText(selectedFile.getPath());
            }
        });

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insetsBottom(5);
        panel.add(pathField, gbc);

        // Help text (only for new placeholders)
        if (isNewPlaceholder) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = JBUI.insetsTop(10);
            JLabel helpLabel = new JLabel("<html><body style='width: 400px'>" +
                "Example: Use ${HOME}/Documents in bookmark paths" +
                "</body></html>");
            helpLabel.setFont(helpLabel.getFont().deriveFont(Font.ITALIC));
            panel.add(helpLabel, gbc);
        }

        // Initialize fields if editing existing placeholder
        if (originalPlaceholder != null) {
            nameField.setText(originalPlaceholder.getName());
            nameField.setEditable(false);
            nameField.setEnabled(false);
            pathField.setText(originalPlaceholder.getPath().toString());
        }

        panel.setBorder(JBUI.Borders.empty(10));
        return panel;
    }

    @Override
    protected ValidationInfo doValidate() {
        String name = nameField.getText().trim();
        String path = pathField.getText().trim();

        // Only validate name for new placeholders
        if (isNewPlaceholder) {
            if (name.isEmpty()) {
                return new ValidationInfo("Placeholder name cannot be empty", nameField);
            }

            if (name.contains("${") || name.contains("}")) {
                return new ValidationInfo("Placeholder name cannot contain '${' or '}'", nameField);
            }

            // Check for duplicate names (case-insensitive since names are converted to uppercase)
            String upperCaseName = name.toUpperCase();
            for (PathPlaceholder existing : existingPlaceholders) {
                if (existing.getName().equals(upperCaseName)) {
                    return new ValidationInfo("A placeholder with the name '" + upperCaseName + "' already exists", nameField);
                }
            }
        }

        if (path.isEmpty()) {
            return new ValidationInfo("Path cannot be empty", pathField);
        }

        try {
            Paths.get(path);
        } catch (Exception e) {
            return new ValidationInfo("Invalid path: " + e.getMessage(), pathField);
        }

        return null;
    }

    /**
     * Get the placeholder from the dialog fields
     */
    public PathPlaceholder getPlaceholder() {
        String name = nameField.getText().trim();
        String path = pathField.getText().trim();

        try {
            return new PathPlaceholder(name, Paths.get(path));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return isNewPlaceholder ? nameField : pathField;
    }
}
