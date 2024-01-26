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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converter for text resources.
 */
public class TextConverter implements Converter {

    private static final Logger logger = Logger.getLogger(TextConverter.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(TextConverter.class);

    private static final List<String> supportedExtensions;

    static {
        supportedExtensions = new ArrayList<>();
        supportedExtensions.add("java");
        supportedExtensions.add("jsp");
        supportedExtensions.add("jspf");
        supportedExtensions.add("jspx");
        supportedExtensions.add("tag");
        supportedExtensions.add("tagf");
        supportedExtensions.add("tagx");
        supportedExtensions.add("tld");
        supportedExtensions.add("txt");
        supportedExtensions.add("xml");
        supportedExtensions.add("json");
        supportedExtensions.add("properties");
        supportedExtensions.add("groovy");
    }

    /**
     * Default constructor.
     */
    public TextConverter() {}

    @Override
    public boolean accepts(String filename) {
        String extension = Util.getExtension(filename);

        return supportedExtensions.contains(extension);
    }


    /*
     * This is a bit of a hack so the same Pattern can be used for text files as
     * for Strings in class files. An approach that worked directly on the
     * streams would be more efficient - but also require significantly more
     * code. Since the conversion process is intended to be a one-time process,
     * this implementation opts for simplicity of code over efficiency of
     * execution.
     */
    @Override
    public boolean convert(String path, InputStream src, OutputStream dest, EESpecProfile profile) throws IOException {
        String srcString = Util.toString(src, StandardCharsets.ISO_8859_1);
        String destString = profile.convert(srcString);
        // Object comparison is deliberate here
        boolean converted = srcString != destString;

        if (converted) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, sm.getString("textConverter.converted", path));
            }
        } else {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, sm.getString("classConverter.noConversion", path));
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(destString.getBytes(StandardCharsets.ISO_8859_1));
        Util.copy(bais, dest);

        return converted;
    }
}
