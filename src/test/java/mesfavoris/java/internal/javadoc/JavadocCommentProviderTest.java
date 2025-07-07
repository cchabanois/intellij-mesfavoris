package mesfavoris.java.internal.javadoc;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavadocCommentProviderTest extends BasePlatformTestCase {
    private final JavadocCommentProvider javadocCommentProvider = new JavadocCommentProvider();

    @Test
    public void testJavadocCommentShortDescription() {
        // Given
        String javaCode = """
            public class TestClass {
                /**
                 * Query to see if this Option requires an argument
                 *
                 * @return boolean flag indicating if an argument is required
                 */
                public boolean requiresArgument() {
                    return false;
                }
            }
            """;

        // When
        PsiMethod method = createMethodFromCode(javaCode);
        String shortDescription = javadocCommentProvider.getJavadocCommentShortDescription(method);

        // Then
        assertThat(shortDescription).isEqualTo("Query to see if this Option requires an argument");
    }

    @Test
    public void testJavadocCommentWithHtmlTags() {
        // Given
        String javaCode = """
            public class TestClass {
                /**
                 * Returns the specified value of this Option or
                 * <code>null</code> if there is no value.
                 *
                 * @return the value/first value of this Option or
                 * <code>null</code> if there is no value.
                 */
                public String getValue() {
                    return null;
                }
            }
            """;

        // When
        PsiMethod method = createMethodFromCode(javaCode);
        String shortDescription = javadocCommentProvider.getJavadocCommentShortDescription(method);

        // Then
        assertThat(shortDescription).isEqualTo("Returns the specified value of this Option or null if there is no value.");
    }

    @Test
    public void testJavadocCommentWithInlineTags() {
        // Given
        String javaCode = """
            public class TestClass {
                /**
                 * Returns the specified value of this Option or
                 * {@code null} if there is no value.
                 *
                 * @return the value/first value of this Option or
                 * <code>null</code> if there is no value.
                 */
                public String getValue() {
                    return null;
                }
            }
            """;

        // When
        PsiMethod method = createMethodFromCode(javaCode);
        String shortDescription = javadocCommentProvider.getJavadocCommentShortDescription(method);

        // Then
        assertThat(shortDescription).isEqualTo("Returns the specified value of this Option or null if there is no value.");
    }

    @Test
    public void testJavadocWithLinkTags() {
        // Given
        String javaCode = """
            public class TestClass {
                /**
                 * See {@link String} for more details or
                 * {@linkplain java.util.List the List interface}.
                 *
                 * @return some value
                 */
                public String getValue() {
                    return null;
                }
            }
            """;

        // When
        PsiMethod method = createMethodFromCode(javaCode);
        String shortDescription = javadocCommentProvider.getJavadocCommentShortDescription(method);

        // Then
        assertThat(shortDescription).isEqualTo("See String for more details or the List interface.");
    }

    @Test
    public void testJavadocWithMultipleParagraphs() {
        // Given
        String javaCode = """
            public class TestClass {
                /**
                 * This is the first paragraph.
                 *
                 * This is the second paragraph and should not be included.
                 *
                 * @return some value
                 */
                public String getValue() {
                    return null;
                }
            }
            """;

        // When
        PsiMethod method = createMethodFromCode(javaCode);
        String shortDescription = javadocCommentProvider.getJavadocCommentShortDescription(method);

        // Then
        String fullDescription = javadocCommentProvider.getJavadocCommentAsText(method);
        assertThat(fullDescription).contains("This is the first paragraph.");
        assertThat(fullDescription).contains("This is the second paragraph and should not be included.");
        assertThat(shortDescription).isEqualTo("This is the first paragraph.");
    }

    @Test
    public void testJavadocWithNoDescription() {
        // Given
        String javaCode = """
            public class TestClass {
                /**
                 * @return some value
                 */
                public String getValue() {
                    return null;
                }
            }
            """;

        // When
        PsiMethod method = createMethodFromCode(javaCode);
        String shortDescription = javadocCommentProvider.getJavadocCommentShortDescription(method);

        // Then
        assertThat(shortDescription).isNull();
    }

    @Test
    public void testMethodWithoutJavadoc() {
        // Given
        String javaCode = """
            public class TestClass {
                public String getValue() {
                    return null;
                }
            }
            """;

        // When
        PsiMethod method = createMethodFromCode(javaCode);
        String shortDescription = javadocCommentProvider.getJavadocCommentShortDescription(method);

        // Then
        assertThat(shortDescription).isNull();
    }

    private PsiMethod createMethodFromCode(String javaCode) {
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByText("TestClass.java", javaCode);
        PsiClass psiClass = javaFile.getClasses()[0];
        return psiClass.getMethods()[0];
    }
}
