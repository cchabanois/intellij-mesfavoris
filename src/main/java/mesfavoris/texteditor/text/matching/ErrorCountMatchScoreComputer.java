package mesfavoris.texteditor.text.matching;

/**
 * An {@link IMatchScoreComputer} that only considers the errorsCount
 * 
 * @author cchabanois
 *
 */
public class ErrorCountMatchScoreComputer implements IMatchScoreComputer {

	@Override
	public double score(int errorsCount, int matchLocation, int expectedLocation, String pattern) {
        return (float) errorsCount / pattern.length();
	}

}
