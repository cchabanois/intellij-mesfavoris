package mesfavoris.texteditor.text;

import com.google.common.io.CharStreams;
import org.junit.Test;

import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

public class RemoveExtraWhitespacesSequenceFilterTest {

	@Test
	public void testFilter() {
		// Given
		ICharSequenceFilter filter = new RemoveExtraWhitespacesSequenceFilter();
		
		// When/Then
		assertEquals("this is a string", new FilteredCharSequence("this is a string", filter).toString());
		assertEquals("this is a string", new FilteredCharSequence(" this is a string ", filter).toString());
		assertEquals("this is a string",
				new FilteredCharSequence("    this    is  a string   ", filter).toString());		
	}
	
	@Test
	public void testFilterJavaMethod() throws Exception {
		// Given
		String source = CharStreams.toString(
				new InputStreamReader(this.getClass().getResourceAsStream("method.java.txt"), "UTF-8"));
		String expectedTarget = CharStreams.toString(
				new InputStreamReader(this.getClass().getResourceAsStream("method-filtered.java.txt"), "UTF-8"));
		
		// When
		FilteredCharSequence target = new FilteredCharSequence(source, new RemoveExtraWhitespacesSequenceFilter());
		
		// Then
		assertEquals(expectedTarget, target.toString());
	}
		
}
