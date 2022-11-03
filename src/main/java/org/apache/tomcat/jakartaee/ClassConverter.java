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
import org.apache.commons.io.IOUtils;

/**
 * Class converter and transformer.
 */
public class ClassConverter implements Converter, ClassFileTransformer {

    private static final Logger logger = Logger.getLogger(ClassConverter.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(ClassConverter.class);

    /**
     * The configured spec profile.
     */
    protected final EESpecProfile profile;

    /**
     * Create a class converter with the default TOMCAT profile.
     */
    public ClassConverter() {
        this(EESpecProfiles.TOMCAT);
    }

    /**
     * Create a class converter with the specified spec profile.
     * @param profile the specification profile to use for conversion
     */
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
    public boolean convert(String path, InputStream src, OutputStream dest, EESpecProfile profile) throws IOException {
        return convertInternal(path, src, dest, profile, null);
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


    /**
     * Convert specified class bytecode.
     * @param path the path
     * @param src the source byte stream
     * @param dest the output byte stream
     * @param profile the specification profile to use
     * @param loader the class loader
     * @return true if conversion occurred
     * @throws IOException rethrow on byte read or write
     */
    protected boolean convertInternal(String path, InputStream src, OutputStream dest, EESpecProfile profile, ClassLoader loader)
            throws IOException {
        byte[] classBytes = IOUtils.toByteArray(src);
        ClassParser parser = new ClassParser(new ByteArrayInputStream(classBytes), "unknown");
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
                            int pos = current.indexOf(profile.getTarget() + "/");
                            boolean dotMode = false;
                            if (pos < 0) {
                                pos = current.indexOf(profile.getTarget() + ".");
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
                                                profile.getSource(),
                                                current.substring(pos).replace('/','.')));
                                    }
                                    // Cancel the replacement as the replacement does not exist
                                    String originalFragment;
                                    if (dotMode) {
                                        originalFragment = current.replace(profile.getTarget() + ".", profile.getSource() + ".");
                                    } else {
                                        originalFragment = current.replace(profile.getTarget() + "/", profile.getSource() + "/");
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

        if (converted) {
            javaClass.dump(dest);
        } else {
            IOUtils.writeChunked(classBytes, dest);
        }

        return converted;
    }
}
