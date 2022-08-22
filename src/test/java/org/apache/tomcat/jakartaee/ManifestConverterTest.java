package org.apache.tomcat.jakartaee;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ManifestConverterTest {

    @Test
    public void testAccepts() {
        ManifestConverter converter = new ManifestConverter();

        assertTrue(converter.accepts("META-INF/MANIFEST.MF"));
        assertTrue(converter.accepts("WEB-INF/bundles/com.example.bundle/META-INF/MANIFEST.MF"));
    }
}
