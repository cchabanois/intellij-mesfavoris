package mesfavoris.gdrive.dialogs;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
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
import mesfavoris.gdrive.mappings.IBookmarkMappings;
import mesfavoris.gdrive.operations.AddFileToFolderOperation;
import mesfavoris.gdrive.operations.GetBookmarkFilesOperation;
import mesfavoris.gdrive.operations.IBookmarkFilesProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImportBookmarksFileDialog extends DialogWrapper {
    private FileTableViewer fileTableViewer;
    private final Project project;
    private final Drive drive;
    private final String applicationFolderId;
    private final IBookmarkMappings bookmarkMappings;
    private final List<File> selectedFiles = new ArrayList<>();
    private final IBookmarkFilesProvider bookmarkFilesProvider;

    public ImportBookmarksFileDialog(@Nullable Project project, Drive drive, String applicationFolderId,
                                     IBookmarkMappings bookmarkMappings) {
        this(project, drive, applicationFolderId, bookmarkMappings, new GetBookmarkFilesOperation(drive));
    }

    public ImportBookmarksFileDialog(@Nullable Project project, Drive drive, String applicationFolderId,
                                     IBookmarkMappings bookmarkMappings, IBookmarkFilesProvider bookmarkFilesProvider) {
        super(project);
        this.project = project;
        this.drive = drive;
        this.applicationFolderId = applicationFolderId;
        this.bookmarkMappings = bookmarkMappings;
        this.bookmarkFilesProvider = bookmarkFilesProvider;
        setTitle("Import Bookmarks File");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(JBUI.size(500, 300));

        JPanel contentPanel = new JPanel(new BorderLayout(JBUI.scale(10), JBUI.scale(10)));
        contentPanel.setBorder(JBUI.Borders.empty(10));

        createFileTableViewer(contentPanel);
        createAddLinkButton(contentPanel);
        setOKActionEnabled(!selectedFiles.isEmpty());
        panel.add(contentPanel, BorderLayout.CENTER);

        // Load files after dialog is created
        ApplicationManager.getApplication().invokeLater(this::refreshFiles, ModalityState.any());

        return panel;
    }

    private void createFileTableViewer(JPanel parent) {
        fileTableViewer = new FileTableViewer();
        fileTableViewer.setFiles(new ArrayList<>());
        fileTableViewer.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedFiles.clear();
                selectedFiles.addAll(fileTableViewer.getSelectedFiles());
                setOKActionEnabled(!selectedFiles.isEmpty());
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(fileTableViewer);
        scrollPane.setPreferredSize(JBUI.size(400, 200));
        parent.add(scrollPane, BorderLayout.CENTER);
    }

    private void createAddLinkButton(JPanel parent) {
        JButton button = new JButton("Add link...");
        button.addActionListener(e -> {
            AddGDriveLinkUrlDialog dialog = new AddGDriveLinkUrlDialog(project);
            if (dialog.showAndGet()) {
                AddFileToFolderOperation operation = new AddFileToFolderOperation(drive);
                try {
                    operation.addToFolder(applicationFolderId, dialog.getFileId());
                    refreshFiles();
                } catch (IOException ex) {
                    Messages.showErrorDialog(project, "Could not add file to folder: " + ex.getMessage(), "Error");
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(button);
        parent.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshFiles() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading Bookmarks Files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading bookmarks files from Google Drive...");
                try {
                    List<File> bookmarkFiles = bookmarkFilesProvider.getBookmarkFiles();
                    // Filter out files that are already mapped
                    List<File> filteredFiles = bookmarkFiles.stream()
                            .filter(file -> bookmarkMappings.getMapping(file.getId()).isEmpty())
                            .collect(Collectors.toList());

                    ApplicationManager.getApplication().invokeLater(() -> fileTableViewer.setFiles(filteredFiles), ModalityState.any());
                } catch (IOException e) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                            "Could not get bookmarks files: " + e.getMessage(),
                            "Error Loading Files"), ModalityState.any());
                }
            }
        });
    }

    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }
}
