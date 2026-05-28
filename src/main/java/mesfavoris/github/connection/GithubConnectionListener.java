package mesfavoris.github.connection;

import com.intellij.util.messages.Topic;

/** MessageBus topic fired when the GitHub connection state changes (connected / disconnected). */
public interface GithubConnectionListener {
    Topic<GithubConnectionListener> TOPIC =
            Topic.create("GithubConnectionListener", GithubConnectionListener.class);

    void connected();

    void disconnected();
}
