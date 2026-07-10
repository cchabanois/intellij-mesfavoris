package mesfavoris.internal.ui.details;

import com.google.common.collect.ImmutableMap;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBTextField;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.ui.ComponentFinder;
import mesfavoris.tests.commons.waits.Waiter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static mesfavoris.model.Bookmark.PROPERTY_NAME;
import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;

public class TagsBookmarkDetailPartTest extends BasePlatformTestCase {
    private BookmarkDatabase bookmarkDatabase;
    private TagsBookmarkDetailPart detailPart;
    private JComponent component;

    private final BookmarkId b1 = new BookmarkId("b1");
    private final BookmarkId b2 = new BookmarkId("b2");

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        BookmarksTree tree = bookmarksTree("root").build();
        bookmarkDatabase = new BookmarkDatabase("test", tree);
        // b1 tagged bug,perf ; b2 tagged ui
        addBookmark(new Bookmark(b1, ImmutableMap.of(PROPERTY_NAME, "B1", PROP_TAGS, "bug,perf")));
        addBookmark(new Bookmark(b2, ImmutableMap.of(PROPERTY_NAME, "B2", PROP_TAGS, "ui")));

        detailPart = new TagsBookmarkDetailPart(getProject(), bookmarkDatabase);
        detailPart.init();
        component = (JComponent) detailPart.createComponent();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            detailPart.dispose();
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testTitleAndCanHandle() {
        assertThat(detailPart.getTitle()).isEqualTo("Tags");
        assertThat(detailPart.canHandle(getBookmark(b1))).isTrue();
        assertThat(detailPart.canHandle(new BookmarkFolder(new BookmarkId("f"), "folder"))).isFalse();
    }

    @Test
    public void testFieldShowsNormalizedTags() {
        // stored unsorted -> field shows canonical (sorted) form
        modifyTags(b1, "perf, bug");
        detailPart.setBookmark(getBookmark(b1));
        assertThat(field().getText()).isEqualTo("bug,perf");
    }

    @Test
    public void testChecklistReflectsAllTreeTagsWithCurrentChecked() {
        detailPart.setBookmark(getBookmark(b1));

        assertThat(checklistTags()).containsExactly("bug", "perf", "ui");
        assertThat(checkedTags()).containsExactlyInAnyOrder("bug", "perf");
    }

    @Test
    public void testTypingInFieldPersistsTagsLive() throws TimeoutException, BookmarksException {
        Bookmark untagged = new Bookmark(new BookmarkId("b3"), ImmutableMap.of(PROPERTY_NAME, "B3"));
        addBookmark(untagged);
        detailPart.setBookmark(getBookmark(untagged.getId()));

        field().setText("zeta, alpha");

        Waiter.waitUntil("tags should be persisted while typing", () -> {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            return "alpha,zeta".equals(getBookmark(untagged.getId()).getPropertyValue(PROP_TAGS));
        });
    }

    @Test
    public void testCheckingBoxAddsTagUncheckingRemovesIt() {
        detailPart.setBookmark(getBookmark(b1));

        // b1 does not have "ui" yet -> checking it adds it
        detailPart.setTagChecked("ui", true);
        assertThat(tagsOf(b1)).contains("ui");

        // unchecking "bug" removes it
        detailPart.setTagChecked("bug", false);
        assertThat(tagsOf(b1)).doesNotContain("bug");
    }

    @Test
    public void testExternalModificationUpdatesFieldAndChecklist() throws TimeoutException {
        detailPart.setBookmark(getBookmark(b1));

        modifyTags(b1, "zzz");

        Waiter.waitUntil("field and checklist reflect external change", () -> {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            return "zzz".equals(field().getText()) && checkedTags().contains("zzz");
        });
    }

    // --- helpers ---

    private JBTextField field() {
        return ComponentFinder.findChildComponent(component, JBTextField.class);
    }

    @SuppressWarnings("unchecked")
    private CheckBoxList<String> checklist() {
        return ComponentFinder.findChildComponent(component, CheckBoxList.class);
    }

    private List<String> checklistTags() {
        CheckBoxList<String> list = checklist();
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < list.getModel().getSize(); i++) {
            tags.add(list.getItemAt(i));
        }
        return tags;
    }

    private List<String> checkedTags() {
        CheckBoxList<String> list = checklist();
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < list.getModel().getSize(); i++) {
            if (list.isItemSelected(i)) {
                tags.add(list.getItemAt(i));
            }
        }
        return tags;
    }

    private List<String> tagsOf(BookmarkId id) {
        return mesfavoris.tags.Tags.getTags(getBookmark(id));
    }

    private Bookmark getBookmark(BookmarkId id) {
        return bookmarkDatabase.getBookmarksTree().getBookmark(id);
    }

    private void addBookmark(Bookmark bookmark) throws BookmarksException {
        bookmarkDatabase.modify(modifier ->
                modifier.addBookmarks(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(), List.of(bookmark)));
    }

    private void modifyTags(BookmarkId id, String tags) {
        try {
            bookmarkDatabase.modify(modifier -> modifier.setPropertyValue(id, PROP_TAGS, tags));
        } catch (BookmarksException e) {
            throw new RuntimeException(e);
        }
    }
}
