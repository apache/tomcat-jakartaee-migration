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
import static org.junit.Assert.assertNotNull;
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
            assertNotNull(is);
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
        ClassConverter converter = new ClassConverter(EESpecProfiles.TOMCAT);
        transformed = converter.transform(this.getClass().getClassLoader(),
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


    /**
     * Multi-fragment constant (mimicking a method descriptor) where one
     * fragment resolves in the jakarta namespace and must be converted, and
     * the other does not and must be reverted. The ';' delimiters between
     * fragments must be preserved either way.
     *
     * @throws Exception if the transformation of the test constants fails
     */
    @Test
    public void testTransformMultiFragmentPartialRevertPreservesDelimiters() throws Exception {
        Set<String> strings = transformTesterConstants();

        assertFalse("Fully unconverted value should not remain",
                strings.contains(TesterConstants.MULTI_FRAGMENT_PARTIAL));
        assertTrue("Convertible fragment should be converted and delimiters preserved",
                strings.contains("(Ljakarta/servlet/CommonGatewayInterface;Ljavax/servlet/DoesNotExist;)V"));
    }


    /**
     * Multi-fragment constant where neither fragment resolves in the jakarta
     * namespace, so every fragment is reverted. The reassembled value must be
     * byte-for-byte identical to the original (delimiters included) and must
     * not be reported as a change to the constant pool.
     *
     * @throws Exception if the transformation of the test constants fails
     */
    @Test
    public void testTransformMultiFragmentFullRevertPreservesDelimiters() throws Exception {
        Set<String> strings = transformTesterConstants();

        assertTrue("Fully reverted value must be reconstructed exactly, delimiters included",
                strings.contains(TesterConstants.MULTI_FRAGMENT_ALL_MISSING));
        assertFalse("Delimiter-stripped corruption must not appear",
                strings.contains("(Ljavax/servlet/DoesNotExistLjavax/servlet/DoesNotExist)V"));
    }


    private Set<String> transformTesterConstants() throws Exception {
        byte[] original;

        try (InputStream is = this.getClass().getResourceAsStream("/org/apache/tomcat/jakartaee/TesterConstants.class");
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            assertNotNull(is);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            original = baos.toByteArray();
        }

        ClassConverter converter = new ClassConverter(EESpecProfiles.TOMCAT);
        byte[] transformed = converter.transform(this.getClass().getClassLoader(),
                "org.apache.tomcat.jakartaee.TesterConstants", null, null, original);

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
        return strings;
    }
}
