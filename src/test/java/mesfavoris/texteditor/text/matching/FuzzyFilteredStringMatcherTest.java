package mesfavoris.texteditor.text.matching;

import com.google.common.io.CharStreams;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.texteditor.text.RemoveExtraWhitespacesSequenceFilter;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class FuzzyFilteredStringMatcherTest extends BasePlatformTestCase {
	private FuzzyFilteredStringMatcher matcher;
	private String text;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		text = CharStreams.toString(
				new InputStreamReader(this.getClass().getResourceAsStream("AbstractDocument.java.txt"), StandardCharsets.UTF_8));

		matcher = new FuzzyFilteredStringMatcher(new FuzzyStringMatcher(0.5f, new DistanceMatchScoreComputer(10000)),
				new RemoveExtraWhitespacesSequenceFilter());
	}

	public void testFind() {
		// Given
		String searchPattern = "    while (position != null  &&  position.offset == offset) {";

		// When
		int match = matcher.find(text, searchPattern, 12790, new EmptyProgressIndicator());

		// Then
		assertThat(text.substring(match, match + 100).trim()).startsWith("while (p != null && p.offset == offset) {");
	}
	
	public void testNotFound() {
		// Given
		String searchPattern = "This won't be found";

		// When
		int match = matcher.find(text, searchPattern, 12790, new EmptyProgressIndicator());

		// Then
		assertThat(match).isEqualTo(-1);
	}
}
