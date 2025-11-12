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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        boolean converted = converter.convert("/MANIFEST.test.MF", getClass().getResourceAsStream("/MANIFEST.test.MF"),
                os, EESpecProfiles.TOMCAT);
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
}
