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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        boolean converted = converter.convert(TEST_FILENAME, in, out, profile);

        // assert
        assertTrue("Should convert when javax packages present", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertEquals(OUTPUT, result);

    }

    @Test
    public void testAcceptsJava() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("HelloServlet.java"));
    }

    @Test
    public void testAcceptsJsp() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("index.jsp"));
    }

    @Test
    public void testAcceptsJspf() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("header.jspf"));
    }

    @Test
    public void testAcceptsJspx() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("page.jspx"));
    }

    @Test
    public void testAcceptsTag() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("mytag.tag"));
    }

    @Test
    public void testAcceptsTagf() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("mytag.tagf"));
    }

    @Test
    public void testAcceptsTagx() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("mytag.tagx"));
    }

    @Test
    public void testAcceptsTld() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("custom.tld"));
    }

    @Test
    public void testAcceptsXml() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("web.xml"));
    }

    @Test
    public void testAcceptsJson() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("config.json"));
    }

    @Test
    public void testAcceptsProperties() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("application.properties"));
    }

    @Test
    public void testAcceptsGroovy() {
        TextConverter converter = new TextConverter();
        assertTrue(converter.accepts("script.groovy"));
    }

    @Test
    public void testAcceptsClassFile() {
        TextConverter converter = new TextConverter();
        assertFalse(converter.accepts("HelloServlet.class"));
    }

    @Test
    public void testAcceptsJarFile() {
        TextConverter converter = new TextConverter();
        assertFalse(converter.accepts("lib.jar"));
    }

    @Test
    public void testAcceptsNoExtension() {
        TextConverter converter = new TextConverter();
        assertFalse(converter.accepts("README"));
    }

    @Test
    public void testConvertNoConversionNeeded() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "This file has no javax packages";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert(TEST_FILENAME, in, out, EESpecProfiles.TOMCAT);

        assertFalse("Should not convert when no javax packages present", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertEquals(content, result);
    }

    @Test
    public void testConvertMultipleReplacements() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "javax.servlet.Servlet\njavax.servlet.http.HttpServlet";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert(TEST_FILENAME, in, out, EESpecProfiles.TOMCAT);

        assertTrue("Should convert when javax packages present", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(result.contains("jakarta.servlet.Servlet"));
        assertTrue(result.contains("jakarta.servlet.http.HttpServlet"));
    }

    @Test
    public void testConvertWithJee8Profile() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "javax.servlet.Servlet";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert(TEST_FILENAME, in, out, EESpecProfiles.JEE8);

        assertFalse("JEE8 profile should not convert", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertEquals(content, result);
    }

    @Test
    public void testConvertJakartaWithJee8Profile() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "import jakarta.servlet.http.HttpServletRequest;";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert("Test.java", in, out, EESpecProfiles.JEE8);

        assertTrue("JEE8 profile should convert jakarta packages", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertEquals("import javax.servlet.http.HttpServletRequest;", result);
    }

    @Test
    public void testConvertEmptyContent() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert(TEST_FILENAME, in, out, EESpecProfiles.TOMCAT);

        assertFalse("Empty content should not be converted", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertEquals("", result);
    }

    @Test
    public void testConvertXmlFile() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "<filter-class>javax.servlet.Filter</filter-class>";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert("web.xml", in, out, EESpecProfiles.TOMCAT);

        assertTrue("Should convert XML content", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(result.contains("jakarta.servlet.Filter"));
    }

    @Test
    public void testConvertPropertiesFile() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "servlet.class=javax.servlet.http.HttpServlet";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert("config.properties", in, out, EESpecProfiles.TOMCAT);

        assertTrue("Should convert properties content", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(result.contains("jakarta.servlet.http.HttpServlet"));
    }

    @Test
    public void testConvertJavaFile() throws IOException {
        TextConverter converter = new TextConverter();
        String content = "import javax.servlet.http.HttpServletRequest;\npublic class Test {}";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert("Test.java", in, out, EESpecProfiles.TOMCAT);

        assertTrue("Should convert Java content", converted);
        String result = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(result.contains("import jakarta.servlet.http.HttpServletRequest"));
    }
}
