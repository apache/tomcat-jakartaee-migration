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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Utility methods.
 */
public class Util {

    /**
     * Get the string after the last dot in the given path.
     * <p>
     * Returns the substring after the last '{@code .}' character in the path,
     * converted to lower case. For paths with dots in directory names, this
     * may not return the actual file extension.
     *
     * @param path the file path or name
     * @return the extension (lowercase) or an empty string if no dot is found
     */
    public static String getExtension(String path) {
        // Extract the extension
        int lastPeriod = path.lastIndexOf('.');
        if (lastPeriod == -1) {
            return "";
        }
        return path.substring(lastPeriod + 1).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Buffered copy.
     * @param is the input
     * @param os the output
     * @throws IOException if an exception occurs
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[8192];
        int numRead;
        while ((numRead = is.read(buf)) >= 0) {
            os.write(buf, 0, numRead);
        }
        os.flush();
    }

    /**
     * Convert the input bytes as a string.
     * @param is the input byte stream
     * @param charset the charset to use
     * @return the converted string
     * @throws IOException if an exception occurs
     */
    public static String toString(InputStream is, Charset charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Util.copy(is, baos);

        return new String(baos.toByteArray(), charset);
    }

    private Util() {
        // Hide default constructor. Utility class.
    }
}
