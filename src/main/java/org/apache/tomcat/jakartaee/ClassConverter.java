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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;

public class ClassConverter implements Converter, ClassFileTransformer {

    private static final Logger logger = Logger.getLogger(ClassConverter.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(ClassConverter.class);

    protected final EESpecProfile profile;
    public ClassConverter() {
        this(EESpecProfile.TOMCAT);
    }
    public ClassConverter(EESpecProfile profile) {
        this.profile = profile;
    }

    @Override
    public String toString() {
        return ClassConverter.class.getCanonicalName() + '[' + profile.toString() + ']';
    }

    @Override
    public boolean accepts(String filename) {
        String extension = Util.getExtension(filename);
        return "class".equals(extension);
    }


    @Override
    public void convert(String path, InputStream src, OutputStream dest, EESpecProfile profile) throws IOException {
        convertInternal(path, src, dest, profile, null);
    }


    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(classfileBuffer);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            convertInternal(className, inputStream, outputStream, profile, loader);
        } catch (IOException e) {
            throw new IllegalClassFormatException(e.getLocalizedMessage());
        }
        return outputStream.toByteArray();
    }


    protected void convertInternal(String path, InputStream src, OutputStream dest, EESpecProfile profile, ClassLoader loader)
            throws IOException {
        ClassParser parser = new ClassParser(src, "unknown");
        JavaClass javaClass = parser.parse();

        boolean converted = false;

        // Loop through constant pool
        Constant[] constantPool = javaClass.getConstantPool().getConstantPool();
        // Need an int as the maximum pool size is 2^16
        for (int i = 0; i < constantPool.length; i++) {
            if (constantPool[i] instanceof ConstantUtf8) {
                ConstantUtf8 c = (ConstantUtf8) constantPool[i];
                String str = c.getBytes();
                String newString = profile.convert(str);
                // Object comparison is deliberate
                if (newString != str) {
                    if (loader != null) {
                        // Since this is a runtime conversion, the idea is to only convert to
                        // Jakarta EE specification classes that exist in the container
                        String[] split = newString.split(";|<");
                        for (String current : split) {
                            int pos = current.indexOf("jakarta/");
                            boolean dotMode = false;
                            if (pos < 0) {
                                pos = current.indexOf("jakarta.");
                                dotMode = true;
                            }
                            if (pos >= 0) {
                                String resourceName = current.substring(pos);
                                if (dotMode) {
                                    resourceName = resourceName.replace('.', '/');
                                }
                                resourceName = resourceName + ".class";
                                if (loader.getResource(resourceName) == null) {
                                    if (logger.isLoggable(Level.FINE)) {
                                        logger.log(Level.FINE, sm.getString("classConverter.skipName",
                                                current.substring(pos).replace('/','.')));
                                    }
                                    // Cancel the replacement as the replacement does not exist
                                    String originalFragment;
                                    if (dotMode) {
                                        originalFragment = current.replace("jakarta.", "javax.");
                                    } else {
                                        originalFragment = current.replace("jakarta/", "javax/");
                                    }
                                    newString = newString.replace(current, originalFragment);
                                }
                            }
                        }
                    }
                    c = new ConstantUtf8(newString);
                    constantPool[i] = c;
                    converted = true;
                }
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            if (converted) {
                logger.log(Level.FINE, sm.getString("classConverter.converted", path.replace('/','.')));
            } else if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, sm.getString("classConverter.noConversion", path.replace('/','.')));
            }
        }

        javaClass.dump(dest);
    }
}
