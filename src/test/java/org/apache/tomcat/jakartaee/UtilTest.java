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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void testGetExtension() {
        assertEquals("java", Util.getExtension("HelloServlet.java"));
        assertEquals("", Util.getExtension("HelloServlet"));
    }

    @Test
    public void testGetExtensionMultipleDots() {
        assertEquals("gz", Util.getExtension("file.tar.gz"));
        assertEquals("class", Util.getExtension("com.example.MyClass.class"));
    }

    @Test
    public void testGetExtensionLeadingDot() {
        assertEquals("gitignore", Util.getExtension(".gitignore"));
    }

    @Test
    public void testGetExtensionEmptyString() {
        assertEquals("", Util.getExtension(""));
    }

    @Test
    public void testGetExtensionOnlyDot() {
        assertEquals("", Util.getExtension("."));
    }

    @Test
    public void testGetExtensionUpperCase() {
        assertEquals("java", Util.getExtension("File.JAVA"));
    }

    @Test
    public void testGetExtensionMixedCase() {
        assertEquals("java", Util.getExtension("File.JaVa"));
    }

    @Test
    public void testGetExtensionPathWithEmpty() {
        assertEquals("", Util.getExtension("/path/some.path/file"));
    }

    @Test
    public void testGetExtensionPathWithExtension() {
        assertEquals("txt", Util.getExtension("/path/some.path/file.txt"));
    }

    @Test
    public void testGetExtensionPathRoot() {
        assertEquals("", Util.getExtension("/"));
    }

    @Test
    public void testCopy() throws IOException {
        byte[] source = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Util.copy(in, out);

        assertArrayEquals(source, out.toByteArray());
    }

    @Test
    public void testCopyEmpty() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Util.copy(in, out);

        assertEquals(0, out.size());
    }

    @Test
    public void testCopyLarge() throws IOException {
        byte[] source = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < source.length; i++) {
            source[i] = (byte) (i % 256);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Util.copy(in, out);

        assertArrayEquals(source, out.toByteArray());
    }
}
