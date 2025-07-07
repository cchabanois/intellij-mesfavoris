package mesfavoris.java.internal.javadoc;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaDocumentedElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.javadoc.PsiInlineDocTag;
import org.jetbrains.annotations.Nullable;

/**
 * Provider for extracting Javadoc comments from PSI elements
 */
public class JavadocCommentProvider {

    public String getJavadocCommentShortDescription(PsiMember member) {
        String javadocComment = getJavadocCommentAsText(member);
        if (javadocComment == null) {
            return null;
        }
        return getJavadocCommentShortDescription(javadocComment);
    }

    public String getJavadocCommentShortDescription(String javadoc) {
        int index = javadoc.indexOf("\n\n");
        if (index != -1) {
            javadoc = javadoc.substring(0, index);
        }
        return javadoc;
    }

    /**
     * Get the Javadoc comment for a PSI member
     *
     * @param member the PSI member (must not be null)
     * @return the Javadoc comment or null if not available
     */
    @Nullable
    public String getJavadocCommentAsText(PsiMember member) {
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
     * @param docComment the PSI doc comment (must not be null)
     * @return the description text or null if not available
     */
    @Nullable
    private String extractJavadocDescription(PsiDocComment docComment) {
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
                if (tokenText.equals("/**") || tokenText.equals("*/")) {
                    continue;
                } else if (tokenText.trim().equals("*")) {
                    description.append('\n');
                } else {
                    // Clean up the text by removing leading asterisks and whitespace
                    String cleanText = cleanJavadocLine(tokenText);
                    if (!cleanText.isEmpty()) {
                        // Don't add space before punctuation
                        boolean needsSpace = !description.isEmpty() &&
                                           !description.toString().endsWith(" ") &&
                                           !cleanText.matches("^[.,:;!?].*");
                        if (needsSpace) {
                            description.append(" ");
                        }
                        description.append(cleanText);
                    }
                }
            }

            // Handle inline tags like {@code}, {@link}, etc.
            if (child instanceof PsiInlineDocTag inlineTag) {
                String tagText = extractInlineTagText(inlineTag);
                if (tagText != null && !tagText.isEmpty()) {
                    // Don't add space before punctuation
                    boolean needsSpace = !description.isEmpty() &&
                                       !description.toString().endsWith(" ") &&
                                       !tagText.matches("^[.,:;!?].*");
                    if (needsSpace) {
                        description.append(" ");
                    }
                    description.append(tagText);
                }
            }
        }

        String result = description.toString().trim();
        // Remove HTML tags from the final result
        result = removeHtmlTags(result);
        // Normalize whitespace (convert multiple whitespace including newlines to single spaces)
        result = result.replaceAll("\\s+", " ").trim();
        return result.isEmpty() ? null : result;
    }

    /**
     * Clean a single line of Javadoc text by removing comment markers
     *
     * @param line the raw line from Javadoc (must not be null)
     * @return the cleaned line
     */
    private String cleanJavadocLine(String line) {
        // Remove leading whitespace and asterisks
        String cleaned = line.replaceFirst("^\\s*\\*\\s?", "");

        // Remove trailing whitespace
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * Remove HTML tags from text and normalize whitespace only if HTML tags were actually removed
     *
     * @param text the text that may contain HTML tags (must not be null)
     * @return the text with HTML tags removed and whitespace normalized if needed
     */
    private String removeHtmlTags(String text) {
        // Remove HTML tags using regex
        String result = text.replaceAll("<[^>]+>", "");

        // Only normalize whitespace if HTML tags were actually removed from this specific text
        if (!text.equals(result)) {
            // HTML tags were found and removed, so normalize whitespace to clean up any gaps
            result = result.replaceAll("\\s+", " ").trim();
        }

        return result;
    }

    /**
     * Extract text from a PSI inline tag
     *
     * @param inlineTag the PSI inline tag (must not be null)
     * @return the extracted text or null
     */
    @Nullable
    private String extractInlineTagText(PsiInlineDocTag inlineTag) {
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
}
