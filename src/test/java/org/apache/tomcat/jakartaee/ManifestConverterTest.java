/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.jakartaee;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.Test;

public class ManifestConverterTest {

    @Test
    public void testAccepts() {
        ManifestConverter converter = new ManifestConverter();

        assertTrue(converter.accepts("META-INF/MANIFEST.MF"));
        assertTrue(converter.accepts("WEB-INF/bundles/com.example.bundle/META-INF/MANIFEST.MF"));

        assertFalse(converter.accepts("xMETA-INF/MANIFEST.MF"));
        assertFalse(converter.accepts("WEB-INF/bundles/com.example.bundle/xMETA-INF/MANIFEST.MF"));
    }


    @Test
    public void testConvert() throws IOException {
        ManifestConverter converter = new ManifestConverter();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        boolean converted;
        try (InputStream src = getClass().getResourceAsStream("/MANIFEST.test.MF")) {
            converted = converter.convert("/MANIFEST.test.MF", src, os, EESpecProfiles.TOMCAT);
        }
        assertTrue(converted);

        String result = os.toString("UTF-8");
        System.out.println(result);
        assertTrue(result.length() != 0);
        result = result.replaceAll("\\s", "");

        // Basic test
        String imports = "jakarta.servlet;version=\"[5.0.0,7.0.0)\"";

        // Test with directives
        String imports2 = "jakarta.servlet.http;version=\"[5.0.0,7.0.0)\";resolution:=\"optional\"";
        assertTrue(result.contains(imports));
        assertTrue(result.contains(imports2));

        // Test with directive and version
        String exports = "jakarta.servlet;version=\"5.0.0\";uses:=\"org.eclipse.core.runtime\"";

        // Same as above, with javax.servlet package in the directive
        String exports2 = "jakarta.servlet.http;version=\"5.0.0\";uses:=\"jakarta.servlet\"";

        // Export a different package that has javax.servlet in a directive so version
        // isn't updated
        String exports3 = "org.apache.tomcat.jakartaee.test;version=\"1.0.0\";uses:=\"jakarta.servlet\"";

        assertTrue(result.contains(exports));
        assertTrue(result.contains(exports2));
        assertTrue(result.contains(exports3));
    }

    @Test
    public void testAcceptsRootManifest() {
        ManifestConverter converter = new ManifestConverter();
        assertTrue(converter.accepts("META-INF/MANIFEST.MF"));
    }

    @Test
    public void testAcceptsNestedManifest() {
        ManifestConverter converter = new ManifestConverter();
        assertTrue(converter.accepts("lib/bundle/META-INF/MANIFEST.MF"));
    }

    @Test
    public void testRejectsNonManifest() {
        ManifestConverter converter = new ManifestConverter();
        assertFalse(converter.accepts("META-INF/SOMEFILE.MF"));
        assertFalse(converter.accepts("MANIFEST.MF"));
        assertFalse(converter.accepts("src/META-INF/MANIFEST.MF.txt"));
    }

    @Test
    public void testConvertNoConversionNeeded() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Create a manifest with no javax packages
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.JEE8);

        assertFalse("Should not convert manifest with no javax packages", converted);
    }

    @Test
    public void testConvertWithImplementationVersion() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Create a manifest with Implementation-Version
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "1.0.0");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        // The suffix is added without counting as a conversion
        assertFalse("Version suffix should not count as a conversion", converted);
        String result = dest.toString("UTF-8");
        assertTrue("Implementation-Version should have migration suffix",
                result.contains("Implementation-Version: 1.0.0-" + Info.getVersion()));
    }

    @Test
    public void testConvertWithJee8Profile() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Create a manifest with javax.servlet reference
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Custom-Header", "javax.servlet.Servlet");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.JEE8);

        assertFalse("JEE8 profile should not convert javax packages", converted);
        String result = dest.toString("UTF-8");
        assertTrue("javax.servlet should remain unchanged with JEE8",
                result.contains("javax.servlet.Servlet"));
    }

    @Test
    public void testConvertRemovesSignatures() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Create a manifest with signature entries
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.SIGNATURE_VERSION, "1.0");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        String result = dest.toString("UTF-8");
        assertFalse("Signature-Version should be removed",
                result.contains("Signature-Version"));
    }

    @Test
    public void testConvertAlreadyMigratedManifest() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Create a manifest that already has the migration suffix
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        String currentVersion = Info.getVersion();
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION,
                "1.0.0-" + currentVersion);

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        String result = dest.toString("UTF-8");
        // Should not double-add the suffix
        assertFalse("Should not double-add migration suffix",
                result.contains("-" + currentVersion + "-" + currentVersion));
    }

    @Test
    public void testConvertNoConversionNeededWithTomcatProfile() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Create a manifest with no javax packages
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        assertFalse("Should not convert manifest with no javax packages", converted);
    }

    @Test
    public void testReplaceVersionInNonOSGiHeader() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // A header that is not Import-Package/Export-Package: the version of
        // the jakarta.servlet package should be replaced with the import
        // version range, other packages should be left alone
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Custom-Header",
                "javax.servlet;version=\"4.0.0\",javax.sql.DataSource;version=\"2.0.0\"");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        assertTrue("Version replacement should count as a conversion", converted);
        String result = dest.toString("UTF-8").replaceAll("\\s", "");
        assertTrue(result, result.contains(
                "jakarta.servlet;version=\"[5.0.0,7.0.0)\",javax.sql.DataSource;version=\"2.0.0\""));
    }

    @Test
    public void testReplaceVersionUsesConfiguredVersion() throws IOException {
        ManifestConverter converter = new ManifestConverter();
        converter.setServletImportVersion("[6.0.0,8.0.0)");

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Custom-Header",
                "jakarta.servlet.http;version=\"4.0.0\"");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        assertTrue("Version replacement should count as a conversion", converted);
        String result = dest.toString("UTF-8").replaceAll("\\s", "");
        assertTrue(result,
                result.contains("jakarta.servlet.http;version=\"[6.0.0,8.0.0)\""));
    }

    @Test
    public void testReplaceVersionInSectionAttributes() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Attributes section = new Attributes();
        section.putValue("Custom-Header", "javax.servlet;version=\"4.0.0\"");
        manifest.getEntries().put("test.txt", section);

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        assertTrue("Version replacement should count as a conversion", converted);
        String result = dest.toString("UTF-8").replaceAll("\\s", "");
        assertTrue(result,
                result.contains("jakarta.servlet;version=\"[5.0.0,7.0.0)\""));
    }

    @Test
    public void testReplaceVersionFallbackOnInvalidImportPackage() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Malformed header: parsing fails so the fallback version replacement
        // applies the import version range
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Import-Package",
                "javax.servlet;uses:=\"x\"(;version=\"4.0.0\"");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        assertTrue("Version replacement should count as a conversion", converted);
        String result = dest.toString("UTF-8").replaceAll("\\s", "");
        assertTrue(result, result.contains(
                "jakarta.servlet;uses:=\"x\"(;version=\"[5.0.0,7.0.0)\""));
    }

    @Test
    public void testReplaceVersionFallbackOnInvalidExportPackage() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Malformed Export-Package header: parsing fails so the fallback
        // version replacement applies the single export version (Export-
        // Package cannot use a version range)
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Export-Package",
                "javax.servlet;uses:=\"x\"(;version=\"4.0.0\"");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        assertTrue("Version replacement should count as a conversion", converted);
        String result = dest.toString("UTF-8").replaceAll("\\s", "");
        assertTrue(result, result.contains(
                "jakarta.servlet;uses:=\"x\"(;version=\"5.0.0\""));
    }

    @Test
    public void testReplaceVersionIgnoresLookalikePackages() throws IOException {
        ManifestConverter converter = new ManifestConverter();

        // Values that contain the jakarta.servlet string but do not match the
        // servlet package version pattern must be left unchanged
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Custom-Header",
                "com.example.jakarta.servlet;version=\"1.0.0\", " +
                "jakarta.servletX;version=\"1.0.0\", " +
                "jakarta.servlet");

        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        boolean converted = converter.convert("META-INF/MANIFEST.MF",
                new ByteArrayInputStream(manifestBytes.toByteArray()), dest, EESpecProfiles.TOMCAT);

        assertFalse("Similar package names should not be modified", converted);
        String result = dest.toString("UTF-8").replaceAll("\\s", "");
        assertTrue(result, result.contains(
                "com.example.jakarta.servlet;version=\"1.0.0\"," +
                "jakarta.servletX;version=\"1.0.0\"," +
                "jakarta.servlet"));
    }
}
