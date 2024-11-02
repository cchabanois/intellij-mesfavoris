package mesfavoris.texteditor.text.matching;

import com.intellij.openapi.progress.ProgressIndicator;

public interface IFuzzyStringMatcher {

	/**
	 * Locate the best instance of 'pattern' in 'text' near 'expectedLocation'.
	 * Returns -1 if no match found.
	 * 
	 * @param text
	 *            The text to search.
	 * @param pattern
	 *            The pattern to search for.
	 * @param expectedLocation
	 *            The location to search around, -1 if unknown.
	 * @param progress
	 * @return Best match index or -1.
	 */	
	public int find(CharSequence text, String pattern, int expectedLocation, ProgressIndicator progress);
	
}