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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Utility methods.
 */
public class Util {

    /**
     * Get the string after the last dot in filename in the given path / name.
     * <p>
     * Returns the substring after the last '{@code .}' character in the path,
     * converted to lower case.
     *
     * @param path the file path or name
     * @return the extension (lowercase) or an empty string if no dot is found
     */
    public static String getExtension(String path) {
        Path filePath = Paths.get(path).getFileName();
        if (filePath == null) {
            return "";
        }
        String fileName = filePath.toString();
        // Extract the extension
        int lastPeriod = fileName.lastIndexOf('.');
        if (lastPeriod == -1) {
            return "";
        }
        return fileName.substring(lastPeriod + 1).toLowerCase(Locale.ENGLISH);
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

    private Util() {
        // Hide default constructor. Utility class.
    }
}
