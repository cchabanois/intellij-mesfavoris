package mesfavoris.java.internal;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.javadoc.PsiInlineDocTag;
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
     * @return the method signature
     */
    public static String getMethodSimpleSignature(PsiMethod method) {
        
        StringBuilder signature = new StringBuilder();
        signature.append(method.getName()).append("(");
        
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                signature.append(", ");
            }
            PsiType type = parameters[i].getType();
            signature.append(type.getPresentableText());
        }
        
        signature.append(")");
        return signature.toString();
    }



    /**
     * Get the Javadoc comment for a PSI member
     *
     * @param member the PSI member (must not be null)
     * @return the Javadoc comment or null if not available
     */
    @Nullable
    public static String getJavadocComment(PsiMember member) {
        if (member instanceof PsiJavaDocumentedElement javaDocumentedElement) {
            PsiDocComment docComment = javaDocumentedElement.getDocComment();
            if (docComment != null) {
                return extractJavadocDescription(docComment);
            }
        }
        return null;
    }

    /**
     * Extract the description part from a Javadoc comment, excluding tags
     *
     * @param docComment the PSI doc comment
     * @return the description text or null if not available
     */
    @Nullable
    private static String extractJavadocDescription(PsiDocComment docComment) {
        StringBuilder description = new StringBuilder();

        // Get all children of the doc comment
        PsiElement[] children = docComment.getChildren();

        for (PsiElement child : children) {
            // Stop when we encounter block tags (like @param, @return, etc.)
            // but continue processing inline tags (like {@code}, {@link})
            if (child instanceof PsiDocTag && !(child instanceof PsiInlineDocTag)) {
                break;
            }

            // Process text content
            if (child instanceof PsiDocToken token) {
                String tokenText = token.getText();

                // Skip comment markers and leading asterisks
                if (tokenText.equals("/**") || tokenText.equals("*/") || tokenText.trim().equals("*")) {
                    continue;
                }

                // Clean up the text by removing leading asterisks and whitespace
                String cleanText = cleanJavadocLine(tokenText);
                if (!cleanText.isEmpty()) {
                    if (!description.isEmpty() && !description.toString().endsWith(" ")) {
                        description.append(" ");
                    }
                    description.append(cleanText);
                }
            }

            // Handle inline tags like {@code}, {@link}, etc.
            if (child instanceof PsiInlineDocTag inlineTag) {
                String tagText = extractInlineTagText(inlineTag);
                if (tagText != null && !tagText.isEmpty()) {
                    if (!description.isEmpty() && !description.toString().endsWith(" ")) {
                        description.append(" ");
                    }
                    description.append(tagText);
                }
            }
        }

        String result = description.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /**
     * Clean a single line of Javadoc text by removing comment markers
     *
     * @param line the raw line from Javadoc (must not be null)
     * @return the cleaned line
     */
    private static String cleanJavadocLine(String line) {

        // Remove leading whitespace and asterisks
        String cleaned = line.replaceFirst("^\\s*\\*\\s?", "");

        // Remove trailing whitespace
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * Extract text from a PSI inline tag
     *
     * @param inlineTag the PSI inline tag (must not be null)
     * @return the extracted text or null
     */
    @Nullable
    private static String extractInlineTagText(PsiInlineDocTag inlineTag) {
        String tagName = inlineTag.getName();

        // Get the content of the tag (everything after the tag name)
        String tagText = inlineTag.getText();
        if (tagText == null) {
            return null;
        }

        // Remove the opening {@tagname and closing }
        String prefix = "{@" + tagName;
        if (tagText.startsWith(prefix) && tagText.endsWith("}")) {
            String content = tagText.substring(prefix.length(), tagText.length() - 1).trim();
            if (content.isEmpty()) {
                return null;
            }

            // Process based on tag type
            switch (tagName) {
                case "code":
                case "literal":
                    // Return content as-is
                    return content;

                case "link":
                case "linkplain":
                    // For links, try to extract description or use reference
                    String[] parts = content.split("\\s+", 2);
                    if (parts.length > 1) {
                        // Has description, use it
                        return parts[1];
                    } else {
                        // No description, use reference (remove package if present)
                        String reference = parts[0];
                        int lastDot = reference.lastIndexOf('.');
                        return lastDot != -1 ? reference.substring(lastDot + 1) : reference;
                    }

                default:
                    // For other tags, return content
                    return content;
            }
        }

        return null;
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
