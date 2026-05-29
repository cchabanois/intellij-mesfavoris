package mesfavoris.github.dialogs;

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

public class AddGistLinkDialog extends DialogWrapper {
    private static final Pattern GIST_ID_PATTERN = Pattern.compile("[0-9a-f]{20,}");
    private static final Pattern GIST_URL_PATTERN = Pattern.compile("https://gist\\.github\\.com/[^/]+/([0-9a-f]{20,})");

    private JTextField inputTextField;
    private String gistId;

    public AddGistLinkDialog(@Nullable Project project) {
        super(project);
        setTitle("Add Gist by URL or ID");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(10), JBUI.scale(10)));
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(JBUI.size(450, 100));

        panel.add(new JLabel("Enter GitHub Gist URL or Gist ID:"), BorderLayout.NORTH);

        inputTextField = new JTextField();
        panel.add(inputTextField, BorderLayout.CENTER);

        JBLabel helpLabel = new JBLabel(
                "e.g. https://gist.github.com/user/abc123... or just the ID",
                UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
        panel.add(helpLabel, BorderLayout.SOUTH);

        return panel;
    }

    public static @Nullable String extractGistId(String input) {
        if (input == null || input.isBlank()) return null;
        String text = input.trim();
        Matcher urlMatcher = GIST_URL_PATTERN.matcher(text);
        if (urlMatcher.find()) return urlMatcher.group(1);
        Matcher idMatcher = GIST_ID_PATTERN.matcher(text);
        if (idMatcher.matches()) return text;
        return null;
    }

    @Override
    protected ValidationInfo doValidate() {
        String text = inputTextField.getText().trim();
        if (text.isEmpty()) {
            return new ValidationInfo("Please enter a URL or Gist ID", inputTextField);
        }
        gistId = extractGistId(text);
        if (gistId != null) return null;
        return new ValidationInfo("Invalid Gist URL or ID", inputTextField);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return inputTextField;
    }

    public String getGistId() {
        return gistId;
    }
}
