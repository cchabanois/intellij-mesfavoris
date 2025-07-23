package mesfavoris.texteditor.text;

import com.google.common.io.CharStreams;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.texteditor.text.matching.DistanceMatchScoreComputer;
import mesfavoris.texteditor.text.matching.FuzzyStringMatcher;

import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

public class FuzzyStringMatcherTest extends BasePlatformTestCase {
	private FuzzyStringMatcher matcher;
	private String text;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		text = CharStreams.toString(
				new InputStreamReader(this.getClass().getResourceAsStream("AbstractDocument.java.txt"), "UTF-8"));

		matcher = new FuzzyStringMatcher(0.5f, new DistanceMatchScoreComputer(10000));
	}

	public void testFind() {
		// Given
		String searchPattern = "    while (position != null && position.offset == offset) {";

		// When
		int match = matcher.find(text, searchPattern, 12790, new EmptyProgressIndicator());

		// Then
		assertThat(text.substring(match, match+100).trim())
				.startsWith("while (p != null && p.offset == offset) {");
	}
}
