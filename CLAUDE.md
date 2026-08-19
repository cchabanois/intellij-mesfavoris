# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Plugin Does

Mesfavoris is an advanced bookmark management plugin for IntelliJ IDEA. It provides hierarchical bookmark organization (folders/subfolders), multiple bookmark types (files, URLs, code snippets, shortcuts, Java members, Markdown notes), path placeholders for portability, and remote synchronization via Google Drive.

## Build & Test Commands

```bash
./gradlew build              # Full build
./gradlew buildPlugin        # Build plugin artifact
./gradlew test               # Run all tests
./gradlew runIde             # Run IDE with plugin loaded for manual testing
./gradlew verifyPlugin       # Verify plugin compatibility

# Run a single test class
./gradlew test --tests mesfavoris.model.BookmarkDatabaseTest
./gradlew test --tests "mesfavoris.ui.renderers.BookmarksTreeCellRendererTest"
```

Google Drive integration tests require environment variables: `USER1_GDRIVE_USERNAME`, `USER1_GDRIVE_REFRESH_TOKEN`, `USER2_GDRIVE_USERNAME`, `USER2_GDRIVE_REFRESH_TOKEN` — they are skipped if absent.

GitHub integration tests require the environment variable `USER1_GITHUB_TOKEN` — they are skipped if absent.

## Architecture

### Public API vs Internal

The plugin strictly separates public API from implementation:
- `mesfavoris.service.IBookmarksService` — the single project-scoped service; entry point for all bookmark operations
- `mesfavoris.model.*` — core data structures (public)
- `mesfavoris.extensions.*` / `mesfavoris.bookmarktype.*` — extension point interfaces (public)
- `mesfavoris.internal.*` — implementation details (do not depend on these from extensions)

### Core Data Model (Immutable)

Uses **javimmutable-collections** throughout:
- `Bookmark` — immutable, identified by `BookmarkId`, holds a property map
- `BookmarkFolder extends Bookmark` — folder nodes
- `BookmarksTree` — immutable tree; never mutated in place
- `BookmarkDatabase` — mutable wrapper around `BookmarksTree`; all mutations go through `BookmarkDatabase.modify(Consumer<IBookmarksTreeModifier>)`, which is transactional and fires before/after listeners

### Extension Point System

Two extension points defined in `plugin.xml`:
1. **`bookmarkType`** (`BookmarkTypeExtension`) — add new bookmark types; each type composes multiple provider interfaces:
   - `IBookmarkPropertiesProvider` — discovers/stores properties when bookmarking
   - `IBookmarkLabelProvider` — display labels in the tree
   - `IBookmarkLocationProvider` — resolves file/offset for navigation
   - `IGotoBookmark` — handles navigation
   - `IBookmarkMarkerAttributesProvider` — gutter marker styling
   - `IBookmarkDetailPart` — custom detail panel UI
2. **`remoteBookmarksStore`** (`RemoteBookmarksStoreExtension`) — pluggable remote sync backends (Google Drive and GitHub Gist are the built-in ones)

### Bookmark Type Implementations

Each feature module under `mesfavoris/` is a self-contained feature — a bookmark type, or a remote store (last two):
- `texteditor/` — file + line bookmarks with resilient location tracking (Bitap algorithm)
- `url/` — URL bookmarks
- `snippets/` — code snippet bookmarks
- `java/` — Java class/method member bookmarks
- `intellij/` — wraps native IntelliJ bookmarks
- `notes/` — Markdown note support
- `gdrive/` — Google Drive remote store
- `github/` — GitHub Gist remote store

### Path Placeholders

Path placeholders allow bookmarks to store paths as `${PLACEHOLDER_NAME}/relative/path` instead of absolute paths, making bookmarks portable across machines and users.

**Key types:**
- `IPathPlaceholders` / `PathPlaceholder` — collection of named `(String name, Path path)` pairs; `HOME` is always pre-defined and immutable
- `IPathPlaceholderResolver` — stateless converter: `expand("${HOME}/foo")` → absolute path; `collapse(path)` → shortest placeholder form
- `PathPlaceholdersStore` — IDE-level (application-scoped) singleton service that persists placeholders to `mesfavoris.xml` via `PersistentStateComponent`

**How it integrates with bookmark types:**

When a bookmark is **created**, `IBookmarkPropertiesProvider` calls `collapse(absolutePath)` to store a placeholder-based path in `PROP_FILE_PATH`. When a bookmark is **used** (label display, file navigation, goto), consumers call `expand(placeholderPath)` to recover the absolute path. Bookmark types inject `PathPlaceholdersStore` via constructor and depend only on the `IPathPlaceholderResolver` interface.

`PathBookmarkProperties` (in `mesfavoris/path/`) defines the shared property name constants (`PROP_FILE_PATH`, `PROP_FOLDER_PATH`, `PROP_WORKSPACE_PATH`, `PROP_PROJECT_NAME`) used by any bookmark type that needs path support.

**Bulk operations** (`ExpandBookmarksOperation`, `CollapseBookmarksOperation`) convert all bookmarks in a selection between absolute and placeholder form, accessible from the placeholders settings panel (`internal/settings/placeholders/`).

### Virtual Bookmark Folders

Virtual bookmark folders are read-only, dynamically-computed folders that appear in the bookmarks tree UI without being stored in `BookmarkDatabase`. They are used to surface curated lists such as recent bookmarks, latest visited, and most visited.

**Key types** (in `mesfavoris/internal/ui/virtual/`):
- `VirtualBookmarkFolder` — abstract base; holds a synthetic `BookmarkFolder` with a generated `BookmarkId`; subclasses implement `getChildren()` (returns `List<BookmarkLink>`) and `initListening()`/`stopListening()` for lazy event subscription
- `BookmarkLink` — immutable wrapper pairing a bookmark with its virtual parent ID
- `BookmarkLinkNode` — mutable tree node wrapping a `BookmarkLink` to keep `TreePath` references stable across bookmark edits
- `IVirtualBookmarkFolderListener` — notified when `getChildren()` would return a different result

**Concrete implementations:**
- `RecentBookmarksVirtualFolder` — N most recently accessed bookmarks (listens to `RecentBookmarksListener.TOPIC`)
- `LatestVisitedBookmarksVirtualFolder` — N latest visited by timestamp
- `MostVisitedBookmarksVirtualFolder` — N most frequently visited

**UI integration:** `ExtendedBookmarksTreeModel` wraps the standard `BookmarksTreeModel` and splices virtual folders into the tree. When a real bookmark changes, it updates the corresponding `BookmarkLinkNode`s; when virtual folder children change, it fires `treeStructureChanged`. The three standard virtual folders are instantiated in `MesFavorisPanel` with the root folder as parent. The cell renderer applies a `VIRTUAL_OVERLAY` icon to distinguish them visually.

Virtual folders are not an extension point — they are hardcoded in `MesFavorisPanel`.

### Persistence

- **Local:** JSON via `BookmarksTreeJsonSerializer` / `BookmarksTreeJsonDeserializer`
- **Remote:** pluggable stores (see the `remoteBookmarksStore` extension point):
  - **Google Drive** — mapping tracked in `gdrive/` package; uses IntelliJ's `PersistentStateComponent` for connection state
  - **GitHub Gist** — mapping tracked in `github/mappings/` (`GistMappingsStore`), connection/user state in `github/connection/` (`GithubUserInfoStore`); both persist to `mesfavoris.xml` via `PersistentStateComponent`
- **Auto-save:** `internal/persistence/` with configurable dirty-state tracking

### UI

- Tool window: `internal/toolwindow/` — JTree-based bookmarks panel
- Actions: `internal/actions/` — registered in `META-INF/actions.xml`
- Settings: `internal/settings/` — configurable via IntelliJ Settings dialog
- Search Everywhere: `internal/search/`

### CI/CD & Release Process

Three workflows in `.github/workflows/`:

- **`build.yml`** — triggered on every push to `main` and on PRs; runs tests (with GDrive secrets), Qodana code inspection, plugin verifier, then creates a **draft** GitHub release (not published automatically)
- **`release.yml`** — triggered when a draft release is manually published on GitHub; publishes the plugin to JetBrains Marketplace (`./gradlew publishPlugin`) and opens a PR to update `CHANGELOG.md`. Requires secrets: `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`
- **`run-ui-tests.yml`** — manually triggered (`workflow_dispatch`); launches the IDE with the robot-server plugin on port 8082, then runs `./gradlew test` on Linux, Windows, and macOS

**Release flow:** push to `main` → CI creates draft release → manually publish it on GitHub → `release.yml` publishes to Marketplace.

### Plugin Configuration Files

- `src/main/resources/META-INF/plugin.xml` — main descriptor
- `src/main/resources/META-INF/actions.xml` — action registrations (gutter menu, keyboard shortcuts)
- `src/main/resources/META-INF/extensions.xml` — service and extension registrations
- `src/main/resources/META-INF/mesfavoris-java.xml` / `mesfavoris-markdown.xml` — optional feature modules (Java and Markdown plugins are optional dependencies)
- `gradle.properties` — platform version and plugin version

### Testing Patterns

Always add tests when implementing new features or fixing bugs.

Two test styles:
1. **Plain JUnit** for model/utility code (no IDE required)
2. **`BasePlatformTestCase` subclasses** for anything touching IntelliJ APIs (file system, PSI, editor)

Test helpers in `src/test/java/mesfavoris/tests/commons/`:
- `bookmarks/` — `BookmarkBuilder`, `BookmarksTreeBuilder` fluent builders
- `waits/` — async waiting utilities for UI tests
- `toolwindow/` — tool window interaction helpers

Test data (sample projects for PSI-level tests) lives in `src/test/testData/`.

For async assertions in tests, use `Waiter.waitUntil()` (`mesfavoris.tests.commons.waits.Waiter`) instead of `Thread.sleep()`.

### Clipboard & Notifications

To copy text to the clipboard and notify the user (pattern used in actions):
```java
CopyPasteManager.getInstance().setContents(new StringSelection(text));
Notification notification = new Notification(
        "com.cchabanois.mesfavoris.info",
        "Title",
        "Body",
        NotificationType.INFORMATION
);
Notifications.Bus.notify(notification, project);
```

### MCP Tool Naming

MCP tool names follow the pattern `<verb>_<store>_bookmark_<noun>`:
- `list_github_bookmark_gists`, `import_github_bookmark_gist`, `import_github_bookmark_from_url`
- `list_gdrive_bookmark_files`, `import_gdrive_bookmark_file`, `import_gdrive_bookmark_from_url`
- `list_remote_bookmark_stores`, `connect_remote_bookmark_store`, etc.

Always include the store qualifier (`github`, `gdrive`) in store-specific tool names so tools from different toolsets are unambiguous.

### Code Comments

Keep comments short — a single concise line whenever possible. Explain the "why", not the "what", and don't restate what the code already says. Avoid multi-paragraph Javadoc blocks unless a public API genuinely needs it.

### Commit Messages

Keep commit messages short — one line, no body. Example: `Use MessageBus for dirty bookmarks state notifications`
