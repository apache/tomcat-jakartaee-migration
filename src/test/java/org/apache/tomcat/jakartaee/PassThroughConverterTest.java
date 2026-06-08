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

import org.junit.Test;

import static org.junit.Assert.*;

public class PassThroughConverterTest {

    private static final String TEST_FILENAME = "project.properties";

    @Test
    public void testConverter() throws Exception {
        String content = "javax.servlet";

        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Converter converter = new PassThroughConverter();

        assertTrue(converter.accepts(TEST_FILENAME));

        converter.convert(TEST_FILENAME, in, out, null);

        assertArrayEquals(content.getBytes(), out.toByteArray());
    }

    @Test
    public void testAcceptsAlways() {
        PassThroughConverter converter = new PassThroughConverter();
        assertTrue(converter.accepts("file.txt"));
        assertTrue(converter.accepts("file.class"));
        assertTrue(converter.accepts("file.jar"));
        assertTrue(converter.accepts("META-INF/MANIFEST.MF"));
        assertTrue(converter.accepts("image.png"));
        assertTrue(converter.accepts(""));
    }

    @Test
    public void testConvertReturnsFalse() throws Exception {
        PassThroughConverter converter = new PassThroughConverter();
        String content = "test content";
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean converted = converter.convert(TEST_FILENAME, in, out, EESpecProfiles.TOMCAT);

        assertFalse("PassThroughConverter should always return false", converted);
    }

    @Test
    public void testConvertPreservesBinaryContent() throws Exception {
        PassThroughConverter converter = new PassThroughConverter();
        byte[] binaryContent = new byte[]{0, 1, 2, 127, -128, -1, 0, (byte) 255};
        ByteArrayInputStream in = new ByteArrayInputStream(binaryContent);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        converter.convert("binary.dat", in, out, null);

        assertArrayEquals("Binary content should be preserved exactly",
                binaryContent, out.toByteArray());
    }

    @Test
    public void testConvertEmptyContent() throws Exception {
        PassThroughConverter converter = new PassThroughConverter();
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        converter.convert("empty.txt", in, out, null);

        assertEquals("Empty content should produce empty output", 0, out.size());
    }

    @Test
    public void testConvertWithDifferentProfiles() throws Exception {
        PassThroughConverter converter = new PassThroughConverter();
        String content = "javax.servlet";

        for (EESpecProfile profile : EESpecProfiles.values()) {
            ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            boolean converted = converter.convert("test.txt", in, out, profile);

            assertFalse("Should return false for profile " + profile, converted);
            assertArrayEquals("Content should be unchanged for profile " + profile,
                    content.getBytes(), out.toByteArray());
        }
    }

    @Test
    public void testConvertLargeContent() throws Exception {
        PassThroughConverter converter = new PassThroughConverter();
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(largeContent);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        converter.convert("large.bin", in, out, null);

        assertArrayEquals("Large content should be preserved exactly",
                largeContent, out.toByteArray());
    }
}
