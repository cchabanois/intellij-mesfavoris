package mesfavoris.gdrive.connection.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AuthorizationCodeIntellijApp extends AuthorizationCodeInstalledApp {
	private static final Logger LOG = Logger.getInstance(AuthorizationCodeIntellijApp.class);
	private final ProgressIndicator progressIndicator;

	public AuthorizationCodeIntellijApp(AuthorizationCodeFlow flow,
			VerificationCodeReceiver receiver, ProgressIndicator progressIndicator) {
		super(flow, receiver);
		this.progressIndicator = progressIndicator;
	}

	@Override
	protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
		String authorizationUrlAsString = authorizationUrl.build();
		copyTextToClipboard(authorizationUrlAsString);
		progressIndicator.setText("Please authorize the application in your browser");
        progressIndicator.setText2("URL has been copied to clipboard");
		openInExternalBrowser(authorizationUrlAsString);
	}

	private void copyTextToClipboard(String text) {
		CopyPasteManager.getInstance().setContents(new StringSelection(text));
	}

	private void openInExternalBrowser(String url) {
		try {
			BrowserLauncher.getInstance().browse(new URI(url));
		} catch (URISyntaxException e) {
			LOG.error("Could not open browser", e);
		}
	}

	public static class Provider implements IAuthorizationCodeInstalledAppProvider {

		@Override
		public AuthorizationCodeInstalledApp get(AuthorizationCodeFlow flow, VerificationCodeReceiver receiver,
				ProgressIndicator progressIndicator) {
            // TODO : use AuthorizationCodeInstalledApp with custom Browser implementation
			return new AuthorizationCodeIntellijApp(flow, receiver, progressIndicator);
		}

	}

}
