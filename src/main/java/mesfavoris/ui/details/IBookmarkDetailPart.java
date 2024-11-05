package mesfavoris.ui.details;

import mesfavoris.model.Bookmark;

import javax.swing.*;

/**
 * Interface that must be implemented for a bookmark detail (comment ...)
 * 
 * @author cchabanois
 *
 */
public interface IBookmarkDetailPart {

	String getTitle();

	JComponent createComponent();

	/**
	 * Set the bookmark for this detail part. This method is called if
	 * canHandle(bookmark) returned true
	 * 
	 * @param bookmark
	 *            the bookmark or null if no bookmark is selected
	 */
	void setBookmark(Bookmark bookmark);

	/**
	 * Can this detail part handle this bookmark
	 * 
	 * @param bookmark
	 *            the bookmark or null if no bookmark is selected
	 * @return true if this detail part can handle bookmark
	 */
	boolean canHandle(Bookmark bookmark);

	void dispose();
}
