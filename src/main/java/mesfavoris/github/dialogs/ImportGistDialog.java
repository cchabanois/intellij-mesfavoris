package mesfavoris.github.dialogs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import mesfavoris.github.mappings.IGistMappings;
import mesfavoris.github.operations.GistApiClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportGistDialog extends DialogWrapper {
    private GistListViewer gistListViewer;
    private final Project project;
    private final GistApiClient apiClient;
    private final IGistMappings gistMappings;
    private final List<GistApiClient.GistResponse> selectedGists = new ArrayList<>();

    public ImportGistDialog(@Nullable Project project, GistApiClient apiClient, IGistMappings gistMappings) {
        super(project);
        this.project = project;
        this.apiClient = apiClient;
        this.gistMappings = gistMappings;
        setTitle("Import Bookmarks from GitHub Gist");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(JBUI.size(500, 300));

        JPanel contentPanel = new JPanel(new BorderLayout(JBUI.scale(10), JBUI.scale(10)));
        contentPanel.setBorder(JBUI.Borders.empty(10));

        gistListViewer = new GistListViewer();
        gistListViewer.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedGists.clear();
                selectedGists.addAll(gistListViewer.getSelectedGists());
                setOKActionEnabled(!selectedGists.isEmpty());
            }
        });
        setOKActionEnabled(false);

        JBScrollPane scrollPane = new JBScrollPane(gistListViewer);
        scrollPane.setPreferredSize(JBUI.size(400, 200));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton addByUrlButton = new JButton("Add by URL/ID...");
        addByUrlButton.addActionListener(e -> {
            AddGistLinkDialog dialog = new AddGistLinkDialog(project);
            if (dialog.showAndGet()) {
                addGistById(dialog.getGistId());
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addByUrlButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(contentPanel, BorderLayout.CENTER);

        ApplicationManager.getApplication().invokeLater(this::refreshGists, ModalityState.any());

        return panel;
    }

    private void refreshGists() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading Gists", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading gists from GitHub...");
                try {
                    List<GistApiClient.GistResponse> gists = apiClient.listGists();
                    List<GistApiClient.GistResponse> importable = gists.stream()
                            .filter(g -> g.files != null && g.files.containsKey("bookmarks.json"))
                            .filter(g -> gistMappings.getMapping(g.id).isEmpty())
                            .toList();
                    ApplicationManager.getApplication().invokeLater(
                            () -> gistListViewer.setGists(importable), ModalityState.any());
                } catch (IOException e) {
                    ApplicationManager.getApplication().invokeLater(
                            () -> Messages.showErrorDialog(project,
                                    "Could not load gists: " + e.getMessage(), "Error"),
                            ModalityState.any());
                }
            }
        });
    }

    private void addGistById(String gistId) {
        if (gistMappings.getMapping(gistId).isPresent()) {
            Messages.showInfoMessage(project, "This gist is already imported.", "Already Imported");
            return;
        }
        try {
            GistApiClient.GistResponse gist = apiClient.loadGist(gistId);
            if (gist.files == null || !gist.files.containsKey("bookmarks.json")) {
                Messages.showErrorDialog(project,
                        "Gist " + gistId + " does not contain a bookmarks.json file.", "Invalid Gist");
                return;
            }
            gistListViewer.addGist(gist);
            gistListViewer.setSelectedValue(gist, true);
        } catch (IOException e) {
            Messages.showErrorDialog(project, "Could not load gist: " + e.getMessage(), "Error");
        }
    }

    public List<GistApiClient.GistResponse> getSelectedGists() {
        return new ArrayList<>(selectedGists);
    }
}
