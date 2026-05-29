package mesfavoris.github.dialogs;

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import mesfavoris.github.operations.GistApiClient;
import mesfavoris.icons.MesFavorisIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class GistListViewer extends JBList<GistApiClient.GistResponse> {
    private final CollectionListModel<GistApiClient.GistResponse> listModel;

    public GistListViewer() {
        this.listModel = new CollectionListModel<>();
        setModel(listModel);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellRenderer(new GistCellRenderer());
    }

    public void setGists(List<GistApiClient.GistResponse> gists) {
        listModel.replaceAll(gists);
    }

    public void addGist(GistApiClient.GistResponse gist) {
        if (listModel.getElementIndex(gist) < 0) {
            listModel.add(listModel.getSize(), gist);
        }
    }

    public List<GistApiClient.GistResponse> getSelectedGists() {
        return getSelectedValuesList();
    }

    private static class GistCellRenderer extends ColoredListCellRenderer<GistApiClient.GistResponse> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends GistApiClient.GistResponse> list,
                                             GistApiClient.GistResponse gist, int index,
                                             boolean selected, boolean hasFocus) {
            setIcon(MesFavorisIcons.bookmarks);
            String label = (gist.description != null && !gist.description.isBlank())
                    ? gist.description : gist.id;
            append(label);
            if (gist.html_url != null) {
                append("  " + gist.html_url, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        }
    }
}
