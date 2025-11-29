package mesfavoris.gdrive.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dialog to add a Google Drive link URL
 */
public class AddGDriveLinkUrlDialog extends DialogWrapper {
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("[-\\w]{25,}");
    private static final Pattern URL_PATTERN = Pattern.compile("https://drive\\.google\\.com/.*[?&]id=([-\\w]{25,})");

    private JTextField urlTextField;
    private String fileId;

    public AddGDriveLinkUrlDialog(@Nullable Project project) {
        super(project);
        setTitle("Add Google Drive Link");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(10), JBUI.scale(10)));
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(JBUI.size(400, 100));

        JLabel label = new JLabel("Enter Google Drive file URL or file ID:");
        panel.add(label, BorderLayout.NORTH);

        urlTextField = new JTextField();
        panel.add(urlTextField, BorderLayout.CENTER);

        JBLabel helpLabel = new JBLabel(
                "You can paste a Google Drive sharing link (e.g., https://drive.google.com/file/d/FILE_ID/view) " +
                "or just the file ID.", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        helpLabel.setCopyable(true);
        panel.add(helpLabel, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    protected ValidationInfo doValidate() {
        String text = urlTextField.getText().trim();
        if (text.isEmpty()) {
            return new ValidationInfo("Please enter a URL or file ID", urlTextField);
        }

        // Try to extract file ID from URL
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        if (urlMatcher.find()) {
            fileId = urlMatcher.group(1);
            return null;
        }

        // Check if it's a direct file ID
        Matcher idMatcher = FILE_ID_PATTERN.matcher(text);
        if (idMatcher.matches()) {
            fileId = text;
            return null;
        }

        return new ValidationInfo("Invalid Google Drive URL or file ID", urlTextField);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return urlTextField;
    }

    public String getFileId() {
        return fileId;
    }
}

