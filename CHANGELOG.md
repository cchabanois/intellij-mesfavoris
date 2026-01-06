<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij-mesfavoris Changelog

## [Unreleased]

## [0.0.1-beta.4] - 2026-01-06

### Fixed

- Fixed ShowBookmarksAction to not use SelectBookmarkAtCaretAction.update which has @ApiStatus.OverrideOnly
- Fixed DeleteBookmarkAction.update to check if bookmarks list is not empty before checking if they can be modified

## [0.0.1-beta.3] - 2026-01-04

### Added

- **Keyboard Shortcuts**
  - Option to use IntelliJ IDEA default bookmark shortcuts instead of Mesfavoris shortcuts
  - Dynamic shortcut display in settings showing actual configured shortcuts
  - No restart required when changing shortcut preferences
  - Shortcut displayed on tool window icon
- **Tool Window Actions**
  - Hide/show tool window action that also selects bookmark at caret
  - Select bookmark at caret action
  - Refresh remote bookmark folders action
  - Manage Placeholders action moved to settings button sub-menu

### Changed

- Google Drive integration now uses DRIVE_FILE scope (more secure - only accesses files created by the app)
- Removed Context parameter from IBookmarkLabelProvider interface
- Updated dependencies to latest versions

### Fixed

- LocalBookmarksSaver now creates parent directory if it does not exist
- Fixed bookmark markers functionality
- Fixed shortcuts display in settings (now shows actual configured shortcuts instead of hardcoded values)
- Use same file for @Storage annotations
- Fixed vendor information

## [0.0.1-beta.1] - 2026-01-01

### Added

- **Bookmark Management**
  - Tool window for organizing bookmarks in a tree structure
  - Add bookmarks with `Alt+B` keyboard shortcut
  - Add bookmark action in editor gutter context menu
  - Goto bookmark functionality
  - Copy/paste bookmarks with special paste options (paste as snippet)
  - Drag and drop support for bookmarks
  - Search bookmarks via "Search Everywhere" (double Shift)
  - Recent bookmarks virtual folder
  - Bookmark markers with visual indicators in the editor gutter
- **Bookmark Types**
  - Text editor bookmarks (files with line numbers)
  - URL bookmarks with favicon support
  - Java type member bookmarks (classes, methods, fields)
  - Snippet bookmarks (code snippets)
  - Extensible bookmark type system via extension points
- **Path Placeholders**
  - Define shortcuts for frequently used paths (e.g., `${HOME}/documents`)
  - Manage placeholders dialog with usage statistics
  - Automatic HOME placeholder
  - Visual distinction for non-modifiable placeholders
  - Collapse/expand paths using placeholders
- **Remote Bookmarks Storage**
  - Google Drive integration for sharing bookmarks
  - OAuth 2.0 authentication
  - Sync bookmarks across multiple machines
  - Conflict detection and resolution
  - Visual overlay icons for remote bookmarks
  - Extensible remote storage system via extension points
- **User Interface**
  - Tree view with custom renderers
  - Bookmark details panel showing properties and markers
  - Context menu with bookmark operations
  - Settings pages for placeholders, bookmark types, and Google Drive
  - Layered icons with overlays for remote stores and markers
  - Collapse/expand all actions
- **Developer Features**
  - Extension points for custom bookmark types
  - Extension points for custom remote storage providers
  - Bookmark property descriptors with builder pattern
  - Bookmark label providers for custom rendering
  - Bookmark properties providers for extracting metadata

[Unreleased]: https://github.com/cchabanois/intellij-mesfavoris/compare/v0.0.1-beta.4...HEAD
[0.0.1-beta.4]: https://github.com/cchabanois/intellij-mesfavoris/compare/v0.0.1-beta.3...v0.0.1-beta.4
[0.0.1-beta.3]: https://github.com/cchabanois/intellij-mesfavoris/compare/v0.0.1-beta.1...v0.0.1-beta.3
[0.0.1-beta.1]: https://github.com/cchabanois/intellij-mesfavoris/commits/v0.0.1-beta.1
