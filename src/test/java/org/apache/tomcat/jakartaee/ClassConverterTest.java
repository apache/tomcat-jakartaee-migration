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

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class ClassConverterTest {

    @Test
    public void testAccepts() {
        Converter converter = new ClassConverter();

        assertTrue(converter.accepts("HelloServlet.class"));
        assertFalse(converter.accepts("HelloServlet.java"));
    }


    @Test
    public void testTransform() throws Exception {
        byte[] original = null;
        byte[] transformed;

        // Get the original bytes
        try (InputStream is = this.getClass().getResourceAsStream("/org/apache/tomcat/jakartaee/TesterConstants.class");
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            original = baos.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail();
        }

        // Transform
        ClassConverter convertor = new ClassConverter(EESpecProfile.TOMCAT);
        transformed = convertor.transform(this.getClass().getClassLoader(),
                "org.apache.tomcat.jakartaee.TesterConstants", null, null, original);

        // Extract strings
        Set<String> strings = new HashSet<>();
        ClassParser parser = new ClassParser(new ByteArrayInputStream(transformed), "unknown");
        JavaClass javaClass = parser.parse();
        Constant[] constantPool = javaClass.getConstantPool().getConstantPool();
        for (int i = 0; i < constantPool.length; i++) {
            if (constantPool[i] instanceof ConstantUtf8) {
                ConstantUtf8 c = (ConstantUtf8) constantPool[i];
                strings.add(c.getBytes());
            }
        }

        // Check the results
        // Should not be converted
        assertTrue(strings.contains("javax.servlet.DoesNotExist"));
        assertTrue(strings.contains("javax/servlet/DoesNotExist"));
        assertFalse(strings.contains("jakarta.servlet.DoesNotExist"));
        assertFalse(strings.contains("jakarta/servlet/DoesNotExist"));
        // Should be converted
        assertFalse(strings.contains("javax.servlet.CommonGatewayInterface"));
        assertFalse(strings.contains("javax/servlet/CommonGatewayInterface"));
        assertTrue(strings.contains("jakarta.servlet.CommonGatewayInterface"));
        assertTrue(strings.contains("jakarta/servlet/CommonGatewayInterface"));
    }
}
