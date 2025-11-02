package mesfavoris.gdrive;

import com.google.api.client.auth.oauth2.StoredCredential;

import java.util.Optional;

public enum GDriveTestUser {

	USER1,
	USER2;
	
	public String getUserName() {
		String userName = System.getenv(name()+"_GDRIVE_USERNAME");
		if (userName == null) {
			throw new IllegalStateException("Could not get test username from env for "+name());
		}
		return userName;
	}
	
	public String getEmail() {
		return getUserName()+"@gmail.com";
	}
	
	public Optional<StoredCredential> getCredential() {
		String accessToken =  System.getenv(name() +"_GDRIVE_ACCESS_TOKEN");
		String refreshToken = System.getenv(name() +"_GDRIVE_REFRESH_TOKEN");
		if (accessToken == null && refreshToken == null) {
			return Optional.empty();
		}
		StoredCredential credential = new StoredCredential();
		credential.setAccessToken(accessToken);
		credential.setRefreshToken(refreshToken);
		return Optional.of(credential);
	}

}
