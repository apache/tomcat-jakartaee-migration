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
import java.util.Properties;

class Info {

    private static final String VERSION;

    static {
        Properties props = new Properties();

        String version = null;
        try {
            props.load(Info.class.getClassLoader().getResourceAsStream("info.properties"));

            version = props.getProperty("version");

        } catch (IOException e) {
            // Handled below
        }

        if (version == null) {
            VERSION = "UNKNOWN";
        } else {
            VERSION = version;
        }
    }

    public static String getVersion() {
        return VERSION;
    }


    private Info() {
        // Utility class. Hide default constructor.
    }
}
