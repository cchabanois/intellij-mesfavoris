<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij-mesfavoris Changelog

## [Unreleased]

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
