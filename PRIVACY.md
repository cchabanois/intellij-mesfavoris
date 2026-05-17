# Privacy Policy

**Mes Favoris** is an open-source bookmark management plugin available for IntelliJ IDEA and Eclipse. This policy describes what data the plugin accesses and how it is used.

## Google Drive

The Google Drive integration is optional. It is shared between the **Mes Favoris IntelliJ plugin** and the **Mes Favoris Eclipse plugin** so that bookmark files can be exchanged between the two IDEs.

### IntelliJ plugin

The IntelliJ plugin requests the `drive.file` scope only — it can read and write exclusively the files it created itself. It cannot access any other files in your Google Drive.

### Eclipse plugin

The Eclipse plugin requests the `drive` scope — full access to Google Drive. No data from your Google Drive is read, modified, or transmitted beyond what is needed to store and retrieve bookmark files.

---

No data from your Google Drive is sent to any third party. All synchronization happens directly between your IDE and Google Drive.

Google Drive credentials (OAuth tokens) are stored locally on your machine by the IDE's credential store and are never transmitted elsewhere.

## No data collection

Mes Favoris does not collect, transmit, or share any personal data, usage statistics, or telemetry. Your bookmarks are stored locally on your machine (or in your own Google Drive if you enable sync).

## Contact

If you have any questions, please open an issue at <https://github.com/cchabanois/intellij-mesfavoris/issues>.
