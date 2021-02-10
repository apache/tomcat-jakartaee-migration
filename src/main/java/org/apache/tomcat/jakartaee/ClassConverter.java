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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;

public class ClassConverter implements Converter {

    private static final Logger logger = Logger.getLogger(ClassConverter.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(ClassConverter.class);

    @Override
    public boolean accepts(String filename) {
        String extension = Util.getExtension(filename);
        return "class".equals(extension);
    }


    @Override
    public void convert(String path, InputStream src, OutputStream dest, EESpecProfile profile) throws IOException {

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
                    c = new ConstantUtf8(profile.convert(str));
                    constantPool[i] = c;
                    converted = true;
                }
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            if (converted) {
                logger.log(Level.FINE, sm.getString("classConverter.converted", path));
            } else if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, sm.getString("classConverter.noConversion", path));
            }
        }

        javaClass.dump(dest);
    }
}
