package mesfavoris.java.internal.editor;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaEditorUtilsTest extends BasePlatformTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Copy test data to project
		myFixture.copyDirectoryToProject("commons-cli", "commons-cli");
	}

	@Override
	protected String getTestDataPath() {
		return "src/test/testData";
	}

	public void testGetLineNumber() {
		// Given
		PsiClass psiClass = JavaPsiFacade.getInstance(getProject())
				.findClass("org.apache.commons.cli.BasicParser", GlobalSearchScope.allScope(getProject()));

		// When
		int lineNumber = JavaEditorUtils.getLineNumber(psiClass);

		// Then
		assertThat(lineNumber).isEqualTo(27);
	}

	public void testMethodSimpleSignature() {
		// Given
		PsiClass psiClass = JavaPsiFacade.getInstance(getProject())
				.findClass("org.apache.commons.cli.BasicParser", GlobalSearchScope.allScope(getProject()));
		PsiMethod method = getAnyMethodWithName(psiClass, "flatten").get();

		// When
		String simpleSignature = JavaEditorUtils.getMethodSimpleSignature(method);

		// Then
		assertThat(simpleSignature).isEqualTo("String[] flatten(Options,String[],boolean)");
	}

	private Optional<PsiMethod> getAnyMethodWithName(PsiClass psiClass, String name) {
		return Arrays.stream(psiClass.getMethods())
				.filter(method -> method.getName().equals(name))
				.findFirst();
	}

}
