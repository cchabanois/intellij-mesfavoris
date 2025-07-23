package mesfavoris.texteditor.text;

import com.google.common.io.CharStreams;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.texteditor.text.matching.DocumentFuzzySearcher;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentFuzzySearcherTest extends BasePlatformTestCase {
	private DocumentFuzzySearcher searcher;
	private Document document;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		String text = CharStreams.toString(
				new InputStreamReader(this.getClass().getResourceAsStream("AbstractDocument.java.txt"), StandardCharsets.UTF_8));
		document = new DocumentImpl(text);
		searcher = new DocumentFuzzySearcher(document);
	}

	public void testFindLine() {
		// Given
		String searchPattern = "private int computeIndexInPosition(List positions, int offset, boolean orderedByOffset) {";

		// When
		int lineNumber = searcher.findLineNumber(450, searchPattern, new EmptyProgressIndicator());

		// Then
		assertThat(lineNumber).isEqualTo(468);
	}

	public void testFindLineWithoutExpectedLineNumber() {
		// Given
		String searchPattern = "private int computeIndexInPosition(List positions, int offset, boolean orderedByOffset) {";

		// When
		int lineNumber = searcher.findLineNumber(searchPattern, new EmptyProgressIndicator());

		// Then
		assertThat(lineNumber).isEqualTo(468);
	}

	public void testFindLineInRegion() throws Exception {
		// Given
		TextRange region = getRegion(document, 458, 483);
		String searchPattern = "private int computeIndexInPosition(List positions, int offset, boolean orderedByOffset) {";

		// When
		int lineNumber = searcher.findLineNumber(region, 470, searchPattern, new EmptyProgressIndicator());

		// Then
		assertThat(lineNumber).isEqualTo(468);
	}

	public void testFindLineInRegionWithInvalidExpectedLineNumber() throws Exception {
		// Given
		TextRange region = getRegion(document, 458, 483);
		String searchPattern = "private int computeIndexInPosition(List positions, int offset, boolean orderedByOffset) {";

		// When
		int lineNumber = searcher.findLineNumber(region, 490, searchPattern, new EmptyProgressIndicator());

		// Then
		assertThat(lineNumber).isEqualTo(468);
	}

	private TextRange getRegion(Document document, int line1, int line2) {
		int startOffset = document.getLineStartOffset(line1);
		int endOffset = document.getLineEndOffset(line2);
		return new TextRange(startOffset, endOffset);
	}

}
