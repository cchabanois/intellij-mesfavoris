package mesfavoris.gdrive.operations;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class GetFileIdFromUrlOperationTest extends BasePlatformTestCase {
	private GetFileIdFromUrlOperation operation;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		operation = new GetFileIdFromUrlOperation();
	}

	public void testGetFileId() {
		assertThat(operation
				.getFileId("https://docs.google.com/a/mycompany.com/document/d/0B7a_ei8brT1TMy1CQ0o5NmZQNEE/edit?usp=sharing")
				.get()).isEqualTo("0B7a_ei8brT1TMy1CQ0o5NmZQNEE");

		assertThat(operation
				.getFileId("https://drive.google.com/file/d/0B7a_ei8brT1TMy1CQ0o5NmZQNEE/view?usp=sharing")
				.get()).isEqualTo("0B7a_ei8brT1TMy1CQ0o5NmZQNEE");

		assertThat(operation
				.getFileId("https://drive.google.com/open?id=0B7a_ei8brT1TMy1CQ0o5NmZQNEE")
				.get()).isEqualTo("0B7a_ei8brT1TMy1CQ0o5NmZQNEE");

		assertThat(operation
				.getFileId("https://drive.google.com/drive/u/3/folders/0B7a_ei8brT1TQTUyc1JKYW9nTHM")
				.get()).isEqualTo("0B7a_ei8brT1TQTUyc1JKYW9nTHM");
	}

}
