# intellij-mesfavoris

![Build](https://github.com/cchabanois/intellij-mesfavoris/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/29581)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/29581)

<!-- Plugin description -->
Mes Favoris is an advanced bookmark management plugin for IntelliJ IDEA that provides a powerful alternative to the built-in bookmark system.

## Features

- **Hierarchical Bookmarks**: Organize bookmarks in folders and subfolders
- **Path Placeholders**: Use variables like `${WORKSPACE_LOC}` for portable bookmarks across different environments
- **Remote Synchronization**: Share bookmarks with your team via Google Drive
- **Code Snippets**: Save code snippets as bookmarks for quick reference
- **Search Everywhere Integration**: Find bookmarks quickly using IntelliJ's Search Everywhere (double Shift)
- **Keyboard Shortcuts**: Optional replacement of IntelliJ's default bookmark shortcuts (F11, Shift+F11)
- **Import/Export**: Share bookmark collections with colleagues

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "intellij-mesfavoris"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/cchabanois/intellij-mesfavoris/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

Copyright 2024-2026 Cédric Chabanois

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
