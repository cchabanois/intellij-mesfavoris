package mesfavoris.gdrive.connection.auth;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.io.IOException;
import java.util.concurrent.*;

public class CancellableLocalServerReceiver implements VerificationCodeReceiver {
	private final ProgressIndicator progressIndicator;
    private final VerificationCodeReceiver localServerReceiver;

	public CancellableLocalServerReceiver(VerificationCodeReceiver localServerReceiver,
                                          ProgressIndicator progressIndicator) {
		this.progressIndicator = progressIndicator;
        this.localServerReceiver = localServerReceiver;
	}

	@Override
	public String waitForCode() throws IOException {
		ExecutorService executor = AppExecutorUtil.getAppExecutorService();

		Callable<String> task = () -> localServerReceiver.waitForCode();
		Future<String> future = executor.submit(task);

		// Poll every 500ms to check if the progress indicator was cancelled
		while (true) {
			if (progressIndicator != null && progressIndicator.isCanceled()) {
				try {
					localServerReceiver.stop();
				} catch (IOException e) {
					// Ignore errors during stop
				}
				throw new IOException("Authorization cancelled by user");
			}

			try {
				// Wait for 500ms before checking again, or return immediately if done
				return future.get(500, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				// Expected - continue polling
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Authorization interrupted", e);
			} catch (ExecutionException e) {
				// The worker thread threw an exception - unwrap and rethrow it
				Throwable cause = e.getCause();
				if (cause instanceof IOException) {
					throw (IOException) cause;
				}
				throw new IOException("Unexpected error during authorization", cause);
			}
		}
	}

	@Override
	public String getRedirectUri() throws IOException {
		return localServerReceiver.getRedirectUri();
	}

	@Override
	public void stop() throws IOException {
		localServerReceiver.stop();
	}
}
