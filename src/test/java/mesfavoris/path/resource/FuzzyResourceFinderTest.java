package mesfavoris.path.resource;

import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
//import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;

public class FuzzyResourceFinderTest /* extends LightJavaCodeInsightFixtureTestCase */ {
    private static final TempDirTestFixture tempDirTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture();
//    private final FuzzyResourceFinder fuzzyResourceFinder = new FuzzyResourceFinder();
/*
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDirTestFixture.setUp();
        tempDirTestFixture.copyAll("src/test/testData/commons-cli", "fuzzyResourceFinderTest");
    }

    @Override
    protected void tearDown() throws Exception {
        tempDirTestFixture.tearDown();
        super.tearDown();
    }
*/
/*
    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }
*/
    /*
//    @Test
    public void testExactPath() {
        myFixture.copyDirectoryToProject("commons-cli", "commons-cli");
        // When
        Optional<VirtualFile> resource = fuzzyResourceFinder.find(getProject(), Paths.get("/fuzzyResourceFinderTest/NOTICE.txt"), false, new EmptyProgressIndicator());

        // Then
        assertTrue(resource.isPresent());
        assertEquals("/fuzzyResourceFinderTest/NOTICE.txt", resource.get().getPath());
    }

    @Test
    public void testFuzzyPath() {
        // When
        Optional<VirtualFile> resource = fuzzyResourceFinder.find(getProject(), Paths.get("/fuzzyResourceFinderTest/org/apache/commons/cli/CommandLine.java"), false, new EmptyProgressIndicator());

        // Then
        assertTrue(resource.isPresent());
        assertEquals("/fuzzyResourceFinderTest/src/main/java/org/apache/commons/cli/CommandLine.java", resource.get().getPath());
    }
*/
  /*  private static void importProjectFromTemplate(String projectName, String templateName)
            throws InvocationTargetException, InterruptedException {
        Bundle bundle = Platform.getBundle("mesfavoris.path.tests");
        new BundleProjectImportOperation(bundle, projectName, "/projects/" + templateName + "/").run(null);
    } */

}
