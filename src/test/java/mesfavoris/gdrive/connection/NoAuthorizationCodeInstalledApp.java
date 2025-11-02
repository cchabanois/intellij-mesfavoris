package mesfavoris.gdrive.connection;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.gdrive.connection.auth.IAuthorizationCodeInstalledAppProvider;

import java.io.IOException;

public class NoAuthorizationCodeInstalledApp extends AuthorizationCodeInstalledApp {
    private final VerificationCodeReceiver receiver;

    public NoAuthorizationCodeInstalledApp(AuthorizationCodeFlow flow,
            VerificationCodeReceiver receiver, ProgressIndicator progressIndicator) {
        super(flow, receiver);
        this.receiver = receiver;
    }

    @Override
    protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
        receiver.stop();
        throw new IOException("Test need to provide a non expired refresh token");
    }

    public static class Provider implements IAuthorizationCodeInstalledAppProvider {

        @Override
        public AuthorizationCodeInstalledApp get(AuthorizationCodeFlow flow, VerificationCodeReceiver receiver,
                                                 ProgressIndicator progressIndicator) {
            return new NoAuthorizationCodeInstalledApp(flow, receiver, progressIndicator);
        }

    }

}
