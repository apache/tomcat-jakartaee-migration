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
}
