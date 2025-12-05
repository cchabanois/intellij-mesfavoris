package mesfavoris;

import mesfavoris.commons.Status;

import java.io.Serial;

public class BookmarksException extends Exception {
	@Serial
	private static final long serialVersionUID = 7786165398262067509L;

    public BookmarksException(Status status) {
		this(status.getMessage(), status.getException());
	}

	public BookmarksException(String message, Throwable e) {
		super(message, e);
	}	
	
	public BookmarksException(String message) {
		this(message, null);
	}
	
}
