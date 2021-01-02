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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specification profile defining the replacements performed.
 */
public enum EESpecProfile {

    TOMCAT("javax([/\\.](annotation(?![/\\.]processing)" +
            "|ejb" +
            "|el" +
            "|mail" +
            "|persistence" +
            "|security[/\\.]auth[/\\.]message" +
            "|servlet" +
            "|transaction(?![/\\.]xa)" +
            "|websocket))"),

    EE("javax([/\\.](activation" +
            "|annotation(?![/\\.]processing)" +
            "|batch" +
            "|decorator" +
            "|ejb" +
            "|el" +
            "|enterprise" +
            "|faces" +
            "|jms" +
            "|json" +
            "|jws" +
            "|interceptor" +
            "|inject" +
            "|mail" +
            "|persistence" +
            "|resource" +
            "|security[/\\.](auth[/\\.]message|enterprise|jacc)" +
            "|servlet" +
            "|transaction(?![/\\.]xa)" +
            "|validation" +
            "|websocket" +
            "|ws[/\\.]rs" +
            "|xml[/\\.](bind|namespace|registry|rpc|soap|stream|ws|XMLConstants)))");

    private Pattern pattern;

    EESpecProfile(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public String convert(String name) {
        Matcher m = pattern.matcher(name);
        return m.replaceAll("jakarta$1");
    }
}
