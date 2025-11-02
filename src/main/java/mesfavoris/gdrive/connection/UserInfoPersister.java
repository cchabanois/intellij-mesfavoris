package mesfavoris.gdrive.connection;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.remote.UserInfo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class UserInfoPersister {
	private static final String PROP_EMAIL = "email";
	private static final String PROP_DISPLAY_NAME = "displayName";
	private final File file;

	public UserInfoPersister(File file) {
		this.file = file;
	}

	public void saveUser(UserInfo user, ProgressIndicator monitor) throws IOException {
		if (monitor != null) {
			monitor.setText("Saving user info");
		}
		JsonWriter jsonWriter = new JsonWriter(new FileWriter(file));
		jsonWriter.setIndent("  ");
		try {
			jsonWriter.beginObject();
			jsonWriter.name(PROP_DISPLAY_NAME);
			jsonWriter.value(user.getDisplayName());
			jsonWriter.name(PROP_EMAIL);
			jsonWriter.value(user.getEmailAddress());
			jsonWriter.endObject();
		} finally {
			jsonWriter.close();
		}
	}

	public UserInfo loadUser(ProgressIndicator monitor) throws IOException {
		if (monitor != null) {
			monitor.setText("Loading user info");
		}
		if (!file.exists()) {
			return null;
		}
		JsonReader jsonReader = new JsonReader(new FileReader(file));
		String displayName = null;
		String email = null;
		try {
			jsonReader.beginObject();
			while (jsonReader.hasNext()) {
				String name = jsonReader.nextName();
				if (name.equals(PROP_DISPLAY_NAME)) {
					displayName = jsonReader.nextString();
				} else if (name.equals(PROP_EMAIL)) {
					email = jsonReader.nextString();
				} else {
					jsonReader.skipValue();
				}
			}
			return new UserInfo(email, displayName);
		} finally {
			jsonReader.close();
		}
	}

}
