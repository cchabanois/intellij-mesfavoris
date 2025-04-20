package mesfavoris.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.BookmarksException;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.internal.MesFavorisProjectIdManager;
import mesfavoris.internal.bookmarktypes.BookmarkLocationProvider;
import mesfavoris.internal.bookmarktypes.BookmarkMarkerAttributesProvider;
import mesfavoris.internal.bookmarktypes.BookmarkPropertiesProvider;
import mesfavoris.internal.bookmarktypes.GotoBookmark;
import mesfavoris.internal.markers.BookmarksMarkers;
import mesfavoris.internal.persistence.BookmarksAutoSaver;
import mesfavoris.internal.persistence.LocalBookmarksSaver;
import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.internal.placeholders.PathPlaceholdersMap;
import mesfavoris.internal.service.operations.AddBookmarkOperation;
import mesfavoris.internal.service.operations.GotoBookmarkOperation;
import mesfavoris.internal.service.operations.utils.INewBookmarkPositionProvider;
import mesfavoris.internal.service.operations.utils.NewBookmarkPositionProvider;
import mesfavoris.internal.validation.AcceptAllBookmarksModificationValidator;
import mesfavoris.internal.workspace.BookmarksWorkspaceFactory;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.modification.IBookmarksModificationValidator;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.texteditor.internal.GotoWorkspaceFileBookmark;
import mesfavoris.texteditor.internal.TextEditorBookmarkPropertiesProvider;
import mesfavoris.texteditor.internal.WorkspaceFileBookmarkLocationProvider;
import mesfavoris.texteditor.internal.WorkspaceFileBookmarkMarkerAttributesProvider;
import mesfavoris.url.internal.GotoUrlBookmark;
import mesfavoris.url.internal.UrlBookmarkLocationProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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

    public BookmarksService(Project project) throws IOException {
        this.project = project;
        init();
    }

    private void init() throws IOException {
        IBookmarksModificationValidator bookmarksModificationValidator = new AcceptAllBookmarksModificationValidator();

        this.bookmarkDatabase = loadBookmarkDatabase(bookmarksModificationValidator);
        this.bookmarkLocationProvider = new BookmarkLocationProvider(Arrays.asList(new UrlBookmarkLocationProvider(), new WorkspaceFileBookmarkLocationProvider()));
        this.gotoBookmark = new GotoBookmark(Arrays.asList(new GotoUrlBookmark(), new GotoWorkspaceFileBookmark()));
        PathPlaceholdersMap mappings = new PathPlaceholdersMap();
        IPathPlaceholderResolver pathPlaceholderResolver = new PathPlaceholderResolver(mappings);
        this.bookmarkPropertiesProvider = new BookmarkPropertiesProvider(List.of(new TextEditorBookmarkPropertiesProvider(pathPlaceholderResolver)));
        this.newBookmarkPositionProvider = new NewBookmarkPositionProvider(project, bookmarkDatabase);
        this.bookmarksMarkers = new BookmarksMarkers(project, bookmarkDatabase, new BookmarkMarkerAttributesProvider(Arrays.asList(new WorkspaceFileBookmarkMarkerAttributesProvider())));
        this.bookmarksMarkers.init();
        LocalBookmarksSaver localBookmarksSaver = new LocalBookmarksSaver(getBookmarksFilePath(project).toFile(),
                new BookmarksTreeJsonSerializer(true));
        bookmarksSaver = new BookmarksAutoSaver(bookmarkDatabase, localBookmarksSaver);
        bookmarksSaver.init();
    }

    private Path getBookmarksFilePath(Project project) throws IOException {
        Path productSpecificMesFavorisParentDir = PathManager.getConfigDir().resolve("mesfavoris");
        if (!Files.exists(productSpecificMesFavorisParentDir)) {
            Files.createDirectory(productSpecificMesFavorisParentDir);
        }
        String projectId = MesFavorisProjectIdManager.Companion.getProjectId(project);
        return productSpecificMesFavorisParentDir.resolve("bookmarks-" + projectId + ".json");
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

    public IBookmarksMarkers getBookmarksMarkers() {
        return bookmarksMarkers;
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
        return;
    }

    @Override
    public void initializeComponent() {
        return;
    }
}
