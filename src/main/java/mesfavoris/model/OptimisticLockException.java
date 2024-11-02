package mesfavoris.model;

import mesfavoris.BookmarksException;

import java.io.Serial;

public class OptimisticLockException extends BookmarksException {

	@Serial
	private static final long serialVersionUID = 6506162570729610798L;

	public OptimisticLockException() {
		super("Data has changed");
	}
	
}
