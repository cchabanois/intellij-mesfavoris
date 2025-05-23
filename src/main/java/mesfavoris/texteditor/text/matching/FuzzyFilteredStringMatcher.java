package mesfavoris.texteditor.text.matching;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.texteditor.text.FilteredCharSequence;
import mesfavoris.texteditor.text.ICharSequenceFilter;

/**
 * A fuzzy string matcher that filters the text (removing whitespaces for ex)
 * before searching for the pattern
 * 
 * @author cchabanois
 *
 */
public class FuzzyFilteredStringMatcher implements IFuzzyStringMatcher {
	private final ICharSequenceFilter filter;
	private final IFuzzyStringMatcher fuzzyStringMatcher;

	public FuzzyFilteredStringMatcher(IFuzzyStringMatcher fuzzyStringMatcher, ICharSequenceFilter filter) {
		this.filter = filter;
		this.fuzzyStringMatcher = fuzzyStringMatcher;
	}

	@Override
	public int find(CharSequence text, String pattern, int expectedLocation, ProgressIndicator progress) {
		FilteredCharSequence filteredCharSequence = new FilteredCharSequence(text, filter);
		String filteredPattern = new FilteredCharSequence(pattern, filter).toString();
		int filteredExpectedLocation;
		if (expectedLocation < 0) {
			filteredExpectedLocation = -1;
		} else if (expectedLocation >= text.length()){
			filteredExpectedLocation = filteredCharSequence.length()-1;
		} else {
			filteredExpectedLocation = filteredCharSequence.getIndex(expectedLocation);
		}
		int filteredIndex = fuzzyStringMatcher.find(filteredCharSequence, filteredPattern, filteredExpectedLocation,
				progress);
		if (filteredIndex == -1) {
			return -1;
		}
		return filteredCharSequence.getParentIndex(filteredIndex);
	}

}
