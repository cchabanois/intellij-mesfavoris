package mesfavoris.texteditor.text.matching;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import mesfavoris.texteditor.text.CharSubSequence;
import mesfavoris.texteditor.text.LowerCaseCharSequence;
import mesfavoris.texteditor.text.RemoveExtraWhitespacesSequenceFilter;

public class DocumentFuzzySearcher {
	private final float matchThreshold;
	private final Document document;

	public DocumentFuzzySearcher(Document document) {
		this(document, 0.5f);
	}

	public DocumentFuzzySearcher(Document document, float matchThreshold) {
		this.document = document;
		this.matchThreshold = matchThreshold;
	}

	public int findLineNumber(String lineContent, ProgressIndicator monitor) {
		return findLineNumber(-1, lineContent, monitor);
	}

	public int findLineNumber(int expectedLineNumber, String lineContent, ProgressIndicator monitor) {
		return findLineNumber(new TextRange(0, document.getTextLength()), expectedLineNumber, lineContent, monitor);
	}

	public int findLineNumber(TextRange region, String lineContent, ProgressIndicator monitor) {
		return findLineNumber(region, -1, lineContent, monitor);
	}

	public int findLineNumber(TextRange region, int expectedLineNumber, String lineContent, ProgressIndicator monitor) {
		try {
			CharSubSequence charSubSequence = new CharSubSequence(document.getCharsSequence(), region);
			CharSequence lowerCharSubSequence = new LowerCaseCharSequence(charSubSequence);
			int expectedLocationInSubSequence;
			if (expectedLineNumber == -1) {
				expectedLocationInSubSequence = -1;
			} else {
				int minLineNumber = document.getLineNumber(region.getStartOffset());
				int maxLineNumber = document.getLineNumber(region.getStartOffset()+region.getLength()-1);
				if (expectedLineNumber < minLineNumber) {
					expectedLineNumber = minLineNumber;
				}
				if (expectedLineNumber > maxLineNumber) {
					expectedLineNumber = maxLineNumber;
				}
				expectedLocationInSubSequence = document.getLineStartOffset(expectedLineNumber) - region.getStartOffset();
			}
			IMatchScoreComputer matchScoreComputer = getMatchScoreComputer(document, region, expectedLineNumber);
			String pattern = new LowerCaseCharSequence(lineContent).toString();

			FuzzyFilteredStringMatcher fuzzyFilteredStringMatcher = new FuzzyFilteredStringMatcher(
					new FuzzyStringMatcher(matchThreshold, matchScoreComputer),
					new RemoveExtraWhitespacesSequenceFilter());
			int matchPositionInSubSequence = fuzzyFilteredStringMatcher.find(lowerCharSubSequence, pattern,
					expectedLocationInSubSequence, monitor);
			if (matchPositionInSubSequence == -1) {
				return -1;
			}
			int matchPosition = charSubSequence.getParentIndex(matchPositionInSubSequence);
			return document.getLineNumber(matchPosition);
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	private IMatchScoreComputer getMatchScoreComputer(Document document, TextRange region, int expectedLineNumber)
			throws IndexOutOfBoundsException {
		if (expectedLineNumber == -1) {
			return new ErrorCountMatchScoreComputer();
		} else {
			int matchDistance = Math.max(document.getLineStartOffset(expectedLineNumber) - region.getStartOffset(),
					region.getStartOffset() + region.getLength() - document.getLineStartOffset(expectedLineNumber));
			return new DistanceMatchScoreComputer(matchDistance);
		}
	}

}
