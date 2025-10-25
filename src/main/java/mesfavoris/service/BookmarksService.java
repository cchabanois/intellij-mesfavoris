package mesfavoris.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.BookmarksException;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.internal.bookmarktypes.BookmarkMarkerAttributesProvider;
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkLabelProvider;
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkLocationProvider;
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkPropertiesProvider;
import mesfavoris.internal.bookmarktypes.extension.ExtensionGotoBookmark;
import mesfavoris.internal.markers.BookmarksMarkers;
import mesfavoris.internal.persistence.BookmarksAutoSaver;
import mesfavoris.internal.persistence.LocalBookmarksSaver;
import mesfavoris.internal.service.operations.*;
import mesfavoris.internal.service.operations.utils.INewBookmarkPositionProvider;
import mesfavoris.internal.service.operations.utils.NewBookmarkPositionProvider;
import mesfavoris.internal.validation.AcceptAllBookmarksModificationValidator;
import mesfavoris.internal.workspace.BookmarksWorkspaceFactory;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.IBookmarksModificationValidator;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.texteditor.internal.WorkspaceFileBookmarkMarkerAttributesProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
@State(name = "BookmarksService", storages = @Storage(value = "mesfavoris.xml", roamingType = RoamingType.PER_OS))
public final class BookmarksService implements Disposable, PersistentStateComponent<Element> {
    private final Project project;
    private BookmarkDatabase bookmarkDatabase;
    private IBookmarkLocationProvider bookmarkLocationProvider;
    private IGotoBookmark gotoBookmark;
    private BookmarksMarkers bookmarksMarkers;
    private IBookmarkPropertiesProvider bookmarkPropertiesProvider;
    private INewBookmarkPositionProvider newBookmarkPositionProvider;
    private BookmarksAutoSaver bookmarksSaver;
    private IBookmarkLabelProvider bookmarkLabelProvider;

    public BookmarksService(Project project) throws IOException {
        this.project = project;
        init();
    }

    private void init() throws IOException {
        IBookmarksModificationValidator bookmarksModificationValidator = new AcceptAllBookmarksModificationValidator();

        this.bookmarkDatabase = loadBookmarkDatabase(bookmarksModificationValidator);
        this.bookmarkLabelProvider = new ExtensionBookmarkLabelProvider();
        this.bookmarkLocationProvider = new ExtensionBookmarkLocationProvider();
        this.gotoBookmark = new ExtensionGotoBookmark();
        this.bookmarkPropertiesProvider = new ExtensionBookmarkPropertiesProvider();
        this.newBookmarkPositionProvider = new NewBookmarkPositionProvider(project, bookmarkDatabase);
        this.bookmarksMarkers = new BookmarksMarkers(project, bookmarkDatabase, new BookmarkMarkerAttributesProvider(Arrays.asList(new WorkspaceFileBookmarkMarkerAttributesProvider())));
        this.bookmarksMarkers.init();
        LocalBookmarksSaver localBookmarksSaver = new LocalBookmarksSaver(getBookmarksFilePath(project).toFile(),
                new BookmarksTreeJsonSerializer(true));
        bookmarksSaver = new BookmarksAutoSaver(bookmarkDatabase, localBookmarksSaver);
        bookmarksSaver.init();
    }



    private Path getBookmarksFilePath(Project project) throws IOException {
        if (project.isDefault()) {
            throw new IOException("Bookmarks are not supported for default project");
        }
        String projectFilePath = project.getProjectFilePath();
        Path projectFile = Paths.get(projectFilePath);
        Path projectConfigDir;

        if (projectFilePath.endsWith(".ipr")) {
            // File-based project: .ipr file is in project root, so get parent and add .idea
            projectConfigDir = projectFile.getParent().resolve(".idea").resolve("mesfavoris");
        } else {
            // Directory-based project: projectFilePath points to a file in .idea directory
            // Get the parent directory (.idea) and add mesfavoris
            projectConfigDir = projectFile.getParent().resolve("mesfavoris");
        }
        if (!Files.exists(projectConfigDir)) {
            Files.createDirectories(projectConfigDir);
        }
        return projectConfigDir.resolve("bookmarks.json");
    }

    private BookmarkDatabase loadBookmarkDatabase(IBookmarksModificationValidator bookmarksModificationValidator) throws IOException {
        File bookmarksFile = getBookmarksFilePath(project).toFile();
        BookmarksWorkspaceFactory bookmarksWorkspaceFactory = new BookmarksWorkspaceFactory(
                new BookmarksTreeJsonDeserializer(), bookmarksModificationValidator);
        if (bookmarksFile.exists()) {
            try {
                return bookmarksWorkspaceFactory.load(bookmarksFile);
            } catch (FileNotFoundException e) {
                return bookmarksWorkspaceFactory.create();
            }
        } else {
            return bookmarksWorkspaceFactory.create();
        }
    }

    public BookmarkDatabase getBookmarkDatabase() {
        return bookmarkDatabase;
    }

    public BookmarksTree getBookmarksTree() {
        return bookmarkDatabase.getBookmarksTree();
    }

    public IBookmarksMarkers getBookmarksMarkers() {
        return bookmarksMarkers;
    }

    public IBookmarksDirtyStateTracker getBookmarksDirtyStateTracker() { return bookmarksSaver; }

    public IBookmarkLabelProvider getBookmarkLabelProvider() {
        return bookmarkLabelProvider;
    }

    private GotoBookmarkOperation getGotoBookmarkOperation() {
        return new GotoBookmarkOperation(project, bookmarkDatabase, bookmarkLocationProvider, gotoBookmark, bookmarksMarkers);
    }

    public void gotoBookmark(BookmarkId bookmarkId, ProgressIndicator progress) throws BookmarksException {
        GotoBookmarkOperation gotoBookmarkOperation = getGotoBookmarkOperation();
        gotoBookmarkOperation.gotoBookmark(bookmarkId, progress);
    }

    public BookmarkId addBookmark(DataContext dataContext, ProgressIndicator progress)
            throws BookmarksException {
        AddBookmarkOperation operation = new AddBookmarkOperation(bookmarkDatabase, bookmarkPropertiesProvider,
                newBookmarkPositionProvider);
        return operation.addBookmark(dataContext, progress);
    }

    public void addBookmarkFolder(BookmarkId parentFolderId, String folderName) throws BookmarksException {
        AddBookmarkFolderOperation operation = new AddBookmarkFolderOperation(bookmarkDatabase);
		operation.addBookmarkFolder(parentFolderId, folderName);
}

    public void deleteBookmarks(List<BookmarkId> selection, boolean recurse) throws BookmarksException {
        DeleteBookmarksOperation operation = new DeleteBookmarksOperation(bookmarkDatabase);
        operation.deleteBookmarks(selection, recurse);
    }

    public void renameBookmark(BookmarkId bookmarkId, String newName) throws BookmarksException {
        RenameBookmarkOperation operation = new RenameBookmarkOperation(bookmarkDatabase);
        operation.renameBookmark(bookmarkId, newName);
    }

    public void setBookmarkProperties(BookmarkId bookmarkId, Map<String, String> properties) throws BookmarksException {
        SetBookmarkPropertiesOperation operation = new SetBookmarkPropertiesOperation(bookmarkDatabase);
        operation.setProperties(bookmarkId, properties);
    }

    public void copyToClipboard(List<BookmarkId> selection) {
        CopyBookmarkOperation operation = new CopyBookmarkOperation();
        operation.copyToClipboard(bookmarkDatabase.getBookmarksTree(), selection);
    }

    public void paste(BookmarkId parentBookmarkId, ProgressIndicator progress) throws BookmarksException {
        PasteBookmarkOperation operation = new PasteBookmarkOperation(project, bookmarkDatabase, bookmarkPropertiesProvider);
        operation.paste(parentBookmarkId, progress);
    }

    public void pasteAfter(BookmarkId parentBookmarkId, BookmarkId bookmarkId, ProgressIndicator progress) throws BookmarksException {
        PasteBookmarkOperation operation = new PasteBookmarkOperation(project, bookmarkDatabase, bookmarkPropertiesProvider);
        operation.pasteAfter(parentBookmarkId, bookmarkId, progress);
    }

    public void cutToClipboard(List<BookmarkId> selection) throws BookmarksException {
        CutBookmarkOperation operation = new CutBookmarkOperation(bookmarkDatabase);
        operation.cutToClipboard(selection);
    }

    @Override
    public void dispose() {
        bookmarksSaver.close();
    }

    @Override
    public @Nullable Element getState() {
        return bookmarksMarkers.getState();
    }

    @Override
    public void loadState(@NotNull Element state) {
        bookmarksMarkers.loadState(state);
    }

    @Override
    public void noStateLoaded() {
    }

    @Override
    public void initializeComponent() {
    }
}
