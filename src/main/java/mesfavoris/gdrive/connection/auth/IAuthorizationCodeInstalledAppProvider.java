package mesfavoris.gdrive.connection.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.intellij.openapi.progress.ProgressIndicator;

public interface IAuthorizationCodeInstalledAppProvider {

	AuthorizationCodeInstalledApp get(AuthorizationCodeFlow flow,
			VerificationCodeReceiver receiver, ProgressIndicator progressIndicator);

}
