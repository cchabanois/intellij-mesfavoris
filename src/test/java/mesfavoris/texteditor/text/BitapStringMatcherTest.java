package mesfavoris.texteditor.text;

import com.google.common.io.CharStreams;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.texteditor.text.matching.BitapStringMatcher;
import mesfavoris.texteditor.text.matching.DistanceMatchScoreComputer;

import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

public class BitapStringMatcherTest extends BasePlatformTestCase {
	private BitapStringMatcher matcher;
	private String text;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		text = CharStreams.toString(
				new InputStreamReader(this.getClass().getResourceAsStream("AbstractDocument.java.txt"), "UTF-8"));

		matcher = new BitapStringMatcher(new DistanceMatchScoreComputer(10000));
	}

	public void testFind() {
		// Given
		String searchPattern = "RegisteredReplace(IDocumentListener docListener";

		// When
		int match = matcher.find(text, searchPattern, 30, new EmptyProgressIndicator());

		// Then
		assertThat(text.substring(match))
				.startsWith("RegisteredReplace(IDocumentListener owner, IDocumentExtension.IReplace replace) {");
	}

}
