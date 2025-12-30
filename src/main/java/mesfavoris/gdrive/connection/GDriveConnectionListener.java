package mesfavoris.gdrive.connection;

import com.intellij.util.messages.Topic;

/**
 * Listener for Google Drive connection events
 */
public interface GDriveConnectionListener {
	Topic<GDriveConnectionListener> TOPIC = Topic.create("GDriveConnectionListener", GDriveConnectionListener.class);

	/**
	 * Called when connection to Google Drive is established
	 */
	void connected();

	/**
	 * Called when disconnected from Google Drive
	 */
	void disconnected();
}
