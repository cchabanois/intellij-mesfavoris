package mesfavoris.java.internal;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMember;
import mesfavoris.bookmarktype.IFileBookmarkLocation;

public class JavaTypeMemberBookmarkLocation implements IFileBookmarkLocation {
	private final PsiMember member;
	private final Integer lineNumber;
	private final Integer lineOffset;

	public JavaTypeMemberBookmarkLocation(PsiMember member, Integer lineNumber, Integer lineOffset) {
		this.member = member;
		this.lineNumber = lineNumber;
		this.lineOffset = lineOffset;
	}

	@Override
	public VirtualFile getFile() {
		return member.getContainingFile().getVirtualFile();
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