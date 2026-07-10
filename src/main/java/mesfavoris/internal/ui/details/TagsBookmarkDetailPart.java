package mesfavoris.internal.ui.details;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckBoxListListener;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.service.IBookmarksService;
import mesfavoris.tags.Tags;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;

/**
 * Detail part to display and edit the tags of a bookmark. Tags are entered as a comma-separated list in a
 * text field, and the tags already in use across all bookmarks are shown as a checklist below (checking a
 * box adds the tag to the current bookmark, unchecking removes it). Only leaf bookmarks can be tagged.
 */
public class TagsBookmarkDetailPart extends AbstractBookmarkDetailPart {
    private JPanel component;
    private JBTextField tagsField;
    private CheckBoxList<String> tagsList;
    private boolean updating = false;

    public TagsBookmarkDetailPart(Project project) {
        this(project, project.getService(IBookmarksService.class).getBookmarkDatabase());
    }

    public TagsBookmarkDetailPart(Project project, BookmarkDatabase bookmarkDatabase) {
        super(project, bookmarkDatabase);
    }

    @Override
    public JComponent createComponent() {
        component = new JPanel(new BorderLayout());
        component.setBorder(JBUI.Borders.empty(5));

        tagsField = new JBTextField();
        tagsField.getEmptyText().setText("Tags separated by commas");
        // apply tags live as the user types, without reformatting the field mid-typing
        tagsField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                liveUpdateFromField();
            }
        });
        // normalize the field content once editing is done
        tagsField.addActionListener(e -> normalizeField());
        tagsField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                normalizeField();
            }
        });

        JBLabel hint = new JBLabel("Use tags to organize and find your bookmarks");
        hint.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        hint.setForeground(UIUtil.getContextHelpForeground());
        hint.setBorder(JBUI.Borders.emptyTop(3));

        JPanel header = new JPanel(new BorderLayout());
        header.add(tagsField, BorderLayout.NORTH);
        header.add(hint, BorderLayout.CENTER);
        header.setBorder(JBUI.Borders.emptyBottom(5));

        tagsList = new CheckBoxList<>();
        tagsList.setCheckBoxListListener(new CheckBoxListListener() {
            @Override
            public void checkBoxSelectionChanged(int index, boolean value) {
                if (updating) {
                    return;
                }
                setTagChecked(tagsList.getItemAt(index), value);
            }
        });

        component.add(header, BorderLayout.NORTH);
        component.add(new JBScrollPane(tagsList), BorderLayout.CENTER);
        return component;
    }

    /**
     * Called on every keystroke: persists the typed tags but leaves the field text untouched so typing (and
     * the caret) is not disturbed. The checklist is intentionally NOT rebuilt here — it is an O(N) tree scan
     * and the set of known tags does not meaningfully change mid-word; it is refreshed on commit
     * ({@link #normalizeField()}) and on external changes instead.
     */
    private void liveUpdateFromField() {
        if (updating || bookmark == null) {
            return;
        }
        persistTags(tagsField.getText(), false);
    }

    /**
     * Called when editing is done (Enter / focus lost): reformats the field to the canonical tags form.
     */
    private void normalizeField() {
        if (updating || bookmark == null) {
            return;
        }
        persistTags(tagsField.getText(), true);
        rebuildTagsList();
    }

    /**
     * Adds or removes the given tag on the current bookmark. Package-private so the checklist wiring can be
     * exercised in tests (the {@link CheckBoxList} click path is not publicly triggerable).
     */
    void setTagChecked(String tag, boolean checked) {
        if (bookmark == null || tag == null) {
            return;
        }
        String current = bookmark.getPropertyValue(PROP_TAGS);
        persistTags(checked ? Tags.addTag(current, tag) : Tags.removeTag(current, tag), true);
    }

    private void persistTags(String rawTags, boolean updateField) {
        if (bookmark == null) {
            return;
        }
        String normalized = Tags.format(Tags.parse(rawTags));
        try {
            bookmarkDatabase.modify(
                    modifier -> modifier.setPropertyValue(bookmark.getId(), PROP_TAGS, normalized),
                    bookmarksTree -> bookmark = bookmarksTree.getBookmark(bookmark.getId()));
        } catch (BookmarksException e) {
            // never happens
        }
        if (updateField) {
            setFieldText(normalized);
        }
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        boolean editable = this.bookmark != null && bookmarkDatabase.getBookmarksModificationValidator()
                .validateModification(bookmarkDatabase.getBookmarksTree(), this.bookmark.getId())
                .isOk();
        setFieldText(this.bookmark != null ? Tags.format(Tags.getTags(this.bookmark)) : null);
        tagsField.setEnabled(editable);
        tagsList.setEnabled(editable);
        rebuildTagsList();
    }

    @Override
    public boolean canHandle(Bookmark bookmark) {
        // tags apply to leaf bookmarks only, not folders
        return !(bookmark instanceof BookmarkFolder);
    }

    @Override
    public String getTitle() {
        return "Tags";
    }

    private void setFieldText(String text) {
        updating = true;
        try {
            tagsField.setText(text == null ? "" : text);
        } finally {
            updating = false;
        }
    }

    /**
     * Repopulates the checklist with every tag in use across the tree, checking those the current bookmark
     * carries.
     */
    private void rebuildTagsList() {
        updating = true;
        try {
            Map<String, Boolean> items = new LinkedHashMap<>();
            if (bookmark != null) {
                Set<String> current = new HashSet<>();
                for (String tag : Tags.getTags(bookmark)) {
                    current.add(tag.toLowerCase());
                }
                for (String tag : Tags.collectAllTags(bookmarkDatabase.getBookmarksTree())) {
                    items.put(tag, current.contains(tag.toLowerCase()));
                }
            }
            tagsList.setStringItems(items);
        } finally {
            updating = false;
        }
    }

    @Override
    protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        String oldTags = oldBookmark == null ? null : oldBookmark.getPropertyValue(PROP_TAGS);
        String newTags = newBookmark == null ? null : newBookmark.getPropertyValue(PROP_TAGS);
        if (!Objects.equals(newTags, oldTags)) {
            setFieldText(newTags);
            rebuildTagsList();
        }
    }
}
