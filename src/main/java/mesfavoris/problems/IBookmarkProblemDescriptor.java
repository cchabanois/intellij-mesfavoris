package mesfavoris.problems;

import java.util.Optional;

public interface IBookmarkProblemDescriptor {

	enum Severity {
		// order is important
		ERROR, WARNING, INFO
	}	
	
	String getProblemType();

	Severity getSeverity();

	String getErrorMessage(BookmarkProblem bookmarkProblem);

	Optional<IBookmarkProblemHandler> getBookmarkProblemHandler();

}