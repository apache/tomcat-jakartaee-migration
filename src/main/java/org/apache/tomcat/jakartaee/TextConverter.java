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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TextConverter implements Converter {

    private static final List<String> supportedExtensions;

    static {
        supportedExtensions = new ArrayList<>();
        supportedExtensions.add("java");
        supportedExtensions.add("jsp");
        supportedExtensions.add("jspx");
        supportedExtensions.add("tag");
        supportedExtensions.add("tagx");
        supportedExtensions.add("tld");
        supportedExtensions.add("txt");
        supportedExtensions.add("xml");
    }


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
    public void convert(InputStream src, OutputStream dest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        flow(src, baos);

        String srcString = new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);

        String destString = Util.convert(srcString);

        ByteArrayInputStream bais = new ByteArrayInputStream(destString.getBytes(StandardCharsets.ISO_8859_1));
        flow (bais, dest);
    }


    private static void flow(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[8192];
        int numRead;
        while ( (numRead = is.read(buf) ) >= 0) {
            if (os != null) {
                os.write(buf, 0, numRead);
            }
        }
    }
}
