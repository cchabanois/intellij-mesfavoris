package mesfavoris.gdrive.dialogs;

import com.google.api.services.drive.model.File;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * List component for displaying Google Drive files
 */
public class FileTableViewer extends JBList<File> {
    private final CollectionListModel<File> listModel;

    public FileTableViewer() {
        super();
        this.listModel = new CollectionListModel<>();
        setModel(listModel);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellRenderer(new FileCellRenderer());
    }

    public void setFiles(List<File> files) {
        listModel.replaceAll(files);
    }

    public List<File> getSelectedFiles() {
        return getSelectedValuesList();
    }

    private static class FileCellRenderer extends ColoredListCellRenderer<File> {
        private static final JBColor WARNING_COLOR = new JBColor(
                new Color(184, 134, 11),  // Dark goldenrod for light theme
                new Color(255, 215, 0)     // Gold for dark theme
        );

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends File> list, File file, int index, boolean selected, boolean hasFocus) {
            setIcon(MesFavorisIcons.bookmarks);

            StyledString styledString = getStyledText(file);
            styledString.appendTo(this);
        }

        private StyledString getStyledText(File file) {
            StyledString styledString = new StyledString(file.getTitle());

            if (Boolean.FALSE.equals(file.getEditable())) {
                styledString = styledString.append(" [readonly]",
                        new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, WARNING_COLOR));
            }

            if (file.getSharingUser() != null) {
                styledString = styledString.append(
                        String.format(" [Shared by %s]", file.getSharingUser().getDisplayName()),
                        new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, WARNING_COLOR));
            }

            return styledString;
        }
    }
}
