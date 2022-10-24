package org.apache.tomcat.jakartaee;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class TextConverterTest {

    private static final String TEST_FILENAME = "text.txt";

	private static final String INPUT = "javax.servlet.http.HttpServletRequest";
	private static final String OUTPUT = "jakarta.servlet.http.HttpServletRequest";

	@Test
	public void testConvert() throws IOException {

		// prepare
		TextConverter converter = new TextConverter();
		ByteArrayInputStream in = new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.ISO_8859_1));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		EESpecProfile profile = EESpecProfiles.EE;

		// test
		converter.convert(TEST_FILENAME, in, out, profile);

		// assert
		String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
		assertEquals(OUTPUT, result);

	}

}
