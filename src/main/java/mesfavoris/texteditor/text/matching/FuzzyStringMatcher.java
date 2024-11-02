package mesfavoris.texteditor.text.matching;

import com.intellij.openapi.progress.ProgressIndicator;

public class FuzzyStringMatcher implements IFuzzyStringMatcher {
	private final BitapStringMatcher bitapStringMatcher;
	private final BitapBigIntegerStringMatcher bitapBigIntegerStringMatcher;

	public FuzzyStringMatcher(float matchThreshold, IMatchScoreComputer matchScoreComputer) {
		this.bitapStringMatcher = new BitapStringMatcher(matchThreshold, matchScoreComputer);
		this.bitapBigIntegerStringMatcher = new BitapBigIntegerStringMatcher(matchThreshold, matchScoreComputer);
	}

	@Override
	public int find(CharSequence text, String pattern, int expectedLocation, ProgressIndicator progress) {
		// Check for null inputs.
		if (text == null || pattern == null) {
			throw new IllegalArgumentException("Null inputs");
		}

		expectedLocation = Math.max(0, Math.min(expectedLocation, text.length()));
		if (text.equals(pattern)) {
			// Shortcut (potentially not guaranteed by the algorithm)
			return 0;
		} else if (text.isEmpty()) {
			// Nothing to match.
			return -1;
		} else if (expectedLocation + pattern.length() <= text.length()
				&& text.subSequence(expectedLocation, expectedLocation + pattern.length()).toString().equals(pattern)) {
			// Perfect match at the perfect spot!
			return expectedLocation;
		} else {
			// Do a fuzzy compare.
			return getBitapStringMatcher(pattern).find(text, pattern, expectedLocation, progress);
		}
	}

	private IFuzzyStringMatcher getBitapStringMatcher(String pattern) {
		if (pattern.length() <= 64) {
			return bitapStringMatcher;
		} else {
			return bitapBigIntegerStringMatcher;
		}
	}

}
