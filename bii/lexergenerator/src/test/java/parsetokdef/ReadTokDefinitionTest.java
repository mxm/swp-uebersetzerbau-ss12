package parsetokdef;

import lexergen.Settings;
import org.junit.Test;

/**
 * 
 * @author benjamin
 */
public class ReadTokDefinitionTest {

	/**
	 * Test of readFile method, of class ReadTokDefinition.
	 */
	@Test
	public void testReadFile() throws Exception {
		System.out.println("readFile");
		String path = Settings.getDefaultTokenDef();
		ReadTokDefinition instance = new ReadTokDefinition();
		instance.readFile(path);
	}
}
