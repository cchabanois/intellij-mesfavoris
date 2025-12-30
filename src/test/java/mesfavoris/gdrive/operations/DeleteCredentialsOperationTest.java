package mesfavoris.gdrive.operations;

import com.google.common.collect.Sets;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.mappings.BookmarkMapping;
import mesfavoris.gdrive.mappings.IBookmarkMappings;
import mesfavoris.gdrive.test.GDriveConnectionRule;
import mesfavoris.model.BookmarkId;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteCredentialsOperationTest extends BasePlatformTestCase {

	private GDriveConnectionRule gdriveConnectionRule;
	private DeleteCredentialsOperation operation;
	private IBookmarkMappings bookmarkMappings;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionRule = new GDriveConnectionRule(getProject(), GDriveTestUser.USER1, true);
		gdriveConnectionRule.before();
		bookmarkMappings = mock(IBookmarkMappings.class);
		operation = new DeleteCredentialsOperation(gdriveConnectionRule.getGDriveConnectionManager(), bookmarkMappings);
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (gdriveConnectionRule != null) {
				gdriveConnectionRule.after();
			}
		} finally {
			super.tearDown();
		}
	}

	public void testCannotDeleteCredentialsIfConnected() throws IOException {
		// When
		Throwable thrown = catchThrowable(() -> operation.deleteCredentials());

		// Then
		assertThat(thrown).isInstanceOf(IOException.class).hasMessage("Cannot delete credentials while connected");
	}

	public void testCannotDeleteCredentialsIfMappingsExist() throws IOException {
		// Given
		gdriveConnectionRule.disconnect();
		when(bookmarkMappings.getMappings()).thenReturn(Sets
				.newHashSet(new BookmarkMapping(new BookmarkId("bookmarkFolderId"), "fileId", Collections.emptyMap())));

		// When
		Throwable thrown = catchThrowable(() -> operation.deleteCredentials());

		// Then
		assertThat(thrown).isInstanceOf(IOException.class).hasMessage("Cannot delete credentials if there are mappings");
	}

	public void testDeleteCredentials() throws Exception {
		// Given
		gdriveConnectionRule.disconnect();

		// When
		operation.deleteCredentials();

		// Then
		// Credentials should be deleted
		assertThat(gdriveConnectionRule.getGDriveConnectionManager().getUserInfo()).isNull();
	}

}
