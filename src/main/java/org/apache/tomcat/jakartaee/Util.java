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

import java.util.Locale;

public class Util {

    /**
     * Get the extension of a filename
     * <p>
     * The extension is the string after the last '{@code .}' in the filename.
     * @param filename the name of the file
     * @return the extension or an empty string, if no dot is found in the filename
     */
    public static String getExtension(String filename) {
        // Extract the extension
        int lastPeriod = filename.lastIndexOf('.');
        if (lastPeriod == -1) {
            return "";
        }
        return filename.substring(lastPeriod + 1).toLowerCase(Locale.ENGLISH);
    }

    private Util() {
        // Hide default constructor. Utility class.
    }
}
