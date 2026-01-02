package mesfavoris.internal.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
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
import mesfavoris.internal.recent.RecentBookmarksDatabase;
import mesfavoris.internal.service.operations.*;
import mesfavoris.internal.service.operations.utils.INewBookmarkPositionProvider;
import mesfavoris.internal.service.operations.utils.NewBookmarkPositionProvider;
import mesfavoris.internal.validation.BookmarksModificationValidator;
import mesfavoris.internal.workspace.BookmarksWorkspaceFactory;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.IBookmarksModificationValidator;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.recent.IRecentBookmarksProvider;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.service.IBookmarksService;
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
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@State(name = "BookmarksService", storages = @Storage(value = "mesfavoris.xml"))
public final class BookmarksService implements IBookmarksService, Disposable, PersistentStateComponent<Element> {
    private static final Duration DEFAULT_RECENT_DURATION = Duration.ofDays(5);

    private final Project project;
    private BookmarkDatabase bookmarkDatabase;
    private IBookmarkLocationProvider bookmarkLocationProvider;
    private IGotoBookmark gotoBookmark;
    private BookmarksMarkers bookmarksMarkers;
    private IBookmarkPropertiesProvider bookmarkPropertiesProvider;
    private INewBookmarkPositionProvider newBookmarkPositionProvider;
    private BookmarksAutoSaver bookmarksSaver;
    private IBookmarkLabelProvider bookmarkLabelProvider;
    private RemoteBookmarksStoreManager remoteBookmarksStoreManager;
    private RecentBookmarksDatabase recentBookmarksDatabase;

    public BookmarksService(Project project) throws IOException {
        this.project = project;
        init();
    }

    private void init() throws IOException {
        this.remoteBookmarksStoreManager = project.getService(RemoteBookmarksStoreManager.class);
        IBookmarksModificationValidator bookmarksModificationValidator = new BookmarksModificationValidator(remoteBookmarksStoreManager);
        this.bookmarkDatabase = loadBookmarkDatabase(bookmarksModificationValidator);
        this.bookmarkLabelProvider = new ExtensionBookmarkLabelProvider();
        this.bookmarkLocationProvider = new ExtensionBookmarkLocationProvider();
        this.gotoBookmark = new ExtensionGotoBookmark();
        this.bookmarkPropertiesProvider = new ExtensionBookmarkPropertiesProvider();
        this.newBookmarkPositionProvider = new NewBookmarkPositionProvider(project, bookmarkDatabase);
        this.bookmarksMarkers = new BookmarksMarkers(project, bookmarkDatabase, new BookmarkMarkerAttributesProvider(Arrays.asList(new WorkspaceFileBookmarkMarkerAttributesProvider())));
        this.bookmarksMarkers.init();
        Disposer.register(this, bookmarksMarkers);
        LocalBookmarksSaver localBookmarksSaver = new LocalBookmarksSaver(getBookmarksFilePath(project).toFile(),
                new BookmarksTreeJsonSerializer(true));
        bookmarksSaver = new BookmarksAutoSaver(bookmarkDatabase, localBookmarksSaver);
        bookmarksSaver.init();
        this.recentBookmarksDatabase = new RecentBookmarksDatabase(project, bookmarkDatabase, DEFAULT_RECENT_DURATION);
        this.recentBookmarksDatabase.init();
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

    @Override
    public IRecentBookmarksProvider getRecentBookmarksProvider() {
        return recentBookmarksDatabase;
    }

    public RecentBookmarksDatabase getRecentBookmarksDatabase() {
        return recentBookmarksDatabase;
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

    public BookmarkId addBookmarkFolder(BookmarkId parentFolderId, String folderName) throws BookmarksException {
        AddBookmarkFolderOperation operation = new AddBookmarkFolderOperation(bookmarkDatabase);
		return operation.addBookmarkFolder(parentFolderId, folderName);
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

    public void addBookmarksTree(BookmarkId parentBookmarkId, BookmarksTree sourceBookmarksTree,
                                 Consumer<BookmarksTree> afterCommit) throws BookmarksException {
        AddBookmarksTreeOperation operation = new AddBookmarksTreeOperation(bookmarkDatabase);
        operation.addBookmarksTree(parentBookmarkId, sourceBookmarksTree, afterCommit);
    }

    public void addToRemoteBookmarksStore(String storeId, final BookmarkId bookmarkFolderId,
                                          final ProgressIndicator monitor) throws BookmarksException {
        AddToRemoteBookmarksStoreOperation operation = new AddToRemoteBookmarksStoreOperation(bookmarkDatabase,
                remoteBookmarksStoreManager);
        operation.addToRemoteBookmarksStore(storeId, bookmarkFolderId, monitor);
    }

    public void removeFromRemoteBookmarksStore(String storeId, final BookmarkId bookmarkFolderId,
                                              final ProgressIndicator progress) throws BookmarksException {
        RemoveFromRemoteBookmarksStoreOperation operation = new RemoveFromRemoteBookmarksStoreOperation(
                bookmarkDatabase, remoteBookmarksStoreManager);
        operation.removeFromRemoteBookmarksStore(storeId, bookmarkFolderId, progress);
    }

    @Override
    public Optional<RemoteBookmarkFolder> getRemoteBookmarkFolder(BookmarkId bookmarkFolderId) {
        return remoteBookmarksStoreManager.getRemoteBookmarkFolder(bookmarkFolderId);
    }

    @Override
    public Optional<IRemoteBookmarksStore> getRemoteBookmarksStore(String storeId) {
        return remoteBookmarksStoreManager.getRemoteBookmarksStore(storeId);
    }

    @Override
    public void dispose() {
        if (recentBookmarksDatabase != null) {
            recentBookmarksDatabase.close();
        }
        bookmarksSaver.close();
    }

    @Override
    public @Nullable Element getState() {
        Element root = new Element("BookmarksService");

        // Save bookmarks markers state
        Element markersState = bookmarksMarkers.getState();
        if (markersState != null) {
            root.addContent(markersState);
        }

        // Save recent bookmarks state
        if (recentBookmarksDatabase != null) {
            Element recentBookmarksState = recentBookmarksDatabase.getState();
            if (recentBookmarksState != null) {
                root.addContent(recentBookmarksState);
            }
        }

        return root;
    }

    @Override
    public void loadState(@NotNull Element state) {
        // Load bookmarks markers state
        Element markersState = state.getChild("BookmarksMarkers");
        if (markersState != null) {
            bookmarksMarkers.loadState(markersState);
        }

        // Load recent bookmarks state
        if (recentBookmarksDatabase != null) {
            Element recentBookmarksState = state.getChild("RecentBookmarks");
            if (recentBookmarksState != null) {
                recentBookmarksDatabase.loadState(recentBookmarksState);
            }
        }
    }

    @Override
    public void noStateLoaded() {
    }

    @Override
    public void initializeComponent() {
    }
}
