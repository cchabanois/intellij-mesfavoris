package mesfavoris.java.internal;

import com.intellij.psi.PsiMember;
import mesfavoris.bookmarktype.IBookmarkLocation;

public class JavaTypeMemberBookmarkLocation implements IBookmarkLocation {
	private final PsiMember member;
	private final Integer lineNumber;
	private final Integer lineOffset;

	public JavaTypeMemberBookmarkLocation(PsiMember member, Integer lineNumber, Integer lineOffset) {
		this.member = member;
		this.lineNumber = lineNumber;
		this.lineOffset = lineOffset;
	}

	public PsiMember getMember() {
		return member;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public Integer getLineOffset() {
		return lineOffset;
	}
	
}