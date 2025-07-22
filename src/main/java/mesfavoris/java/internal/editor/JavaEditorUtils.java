package mesfavoris.java.internal.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for working with Java elements in IntelliJ editors
 */
public class JavaEditorUtils {

    /**
     * Get the line number (0-based) of a PSI element
     *
     * @param element the PSI element (must not be null)
     * @return the line number or -1 if not available
     */
    public static int getLineNumber(PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return -1;
        }
        
        Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(containingFile);
        if (document == null) {
            return -1;
        }
        
        int offset = element.getTextOffset();
        return document.getLineNumber(offset);
    }

    /**
     * Get the Java member at the specified offset in the file
     *
     * @param psiJavaFile the PSI Java file
     * @param offset the offset
     * @return the Java member or null if not found
     */
    @Nullable
    public static PsiMember getJavaElementAt(PsiJavaFile psiJavaFile, int offset) {
        PsiElement element = psiJavaFile.findElementAt(offset);
        if (element == null) {
            return null;
        }
        
        // Find the containing Java member (method, field, class, etc.)
        return PsiTreeUtil.getParentOfType(element, PsiMember.class);
    }

    /**
     * Get a simple method signature for a PSI method
     *
     * @param method the PSI method (must not be null)
     * @return the method signature in format "ReturnType methodName(ParamType1, ParamType2)"
     */
    public static String getMethodSimpleSignature(PsiMethod method) {

        StringBuilder signature = new StringBuilder();

        // Add return type
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            signature.append(returnType.getPresentableText()).append(" ");
        }

        // Add method name
        signature.append(method.getName()).append("(");

        // Add parameters
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            PsiType type = parameters[i].getType();
            signature.append(type.getPresentableText());
        }

        signature.append(")");
        return signature.toString();
    }

    /**
     * Get the offset of the first non-whitespace character at the specified line
     * 
     * @param editor the editor
     * @param lineNumber the line number (0-based)
     * @return the offset or -1 if not available
     */
    public static int getOffsetOfFirstNonWhitespaceCharAtLine(Editor editor, int lineNumber) {
        Document document = editor.getDocument();
        if (lineNumber < 0 || lineNumber >= document.getLineCount()) {
            return -1;
        }
        
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int lineEndOffset = document.getLineEndOffset(lineNumber);
        
        String lineText = document.getText().substring(lineStartOffset, lineEndOffset);
        for (int i = 0; i < lineText.length(); i++) {
            if (!Character.isWhitespace(lineText.charAt(i))) {
                return lineStartOffset + i;
            }
        }
        
        return lineStartOffset;
    }
}
