package mesfavoris.ui.details;

import com.intellij.openapi.Disposable;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Interface that must be implemented for a bookmark detail (comment ...)
 * 
 * @author cchabanois
 *
 */
public interface IBookmarkDetailPart extends Disposable {

	String getTitle();

	/**
	 * Init the bookmark detail part. This is called before the component is created
	 */
	void init();

	/**
	 * Create the component for the detail part
	 * @return the component
	 */
	JComponent createComponent();

	/**
	 * Set the bookmark for this detail part. This method is called if
	 * canHandle(bookmark) returned true
	 * 
	 * @param bookmark
	 *            the bookmark or null if no bookmark is selected
	 */
	void setBookmark(@Nullable Bookmark bookmark);

	/**
	 * Can this detail part handle this bookmark
	 * 
	 * @param bookmark
	 *            the bookmark or null if no bookmark is selected
	 * @return true if this detail part can handle bookmark
	 */
	boolean canHandle(@Nullable Bookmark bookmark);


}
