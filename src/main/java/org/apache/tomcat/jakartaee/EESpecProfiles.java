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

import java.util.regex.Pattern;

/**
 * Specification profile defining the replacements performed.
 */
public enum EESpecProfiles implements EESpecProfile {

    TOMCAT("javax", "jakarta",
            "javax([/\\.](annotation(?![/\\.]processing)" +
                    "|ejb" +
                    "|el" +
                    "|mail" +
                    "|persistence" +
                    "|security[/\\.]auth[/\\.]message" +
                    "|servlet" +
                    "|transaction(?![/\\.]xa)" +
                    "|websocket))"),

    EE("javax", "jakarta",
            "javax([/\\.](activation" +
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
                    "|management[/\\.]j2ee" +
                    "|persistence" +
                    "|resource" +
                    "|security[/\\.](auth[/\\.]message|enterprise|jacc)" +
                    "|servlet" +
                    "|transaction(?![/\\.]xa)" +
                    "|validation" +
                    "|websocket" +
                    "|ws[/\\.]rs" +
                    "|xml[/\\.](bind|soap|ws)))"),
    JEE8("jakarta", "javax",
            "jakarta([/\\.](activation" +
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
                    "|management[/\\.]j2ee" +
                    "|persistence" +
                    "|resource" +
                    "|security[/\\.](auth[/\\.]message|enterprise|jacc)" +
                    "|servlet" +
                    "|transaction(?![/\\.]xa)" +
                    "|validation" +
                    "|websocket" +
                    "|ws[/\\.]rs" +
                    "|xml[/\\.](bind|soap|ws)))");
    private String source;
    private String target;
    private Pattern pattern;

    EESpecProfiles(String source, String target, String pattern) {
        this.source = source;
        this.target = target;
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }
}
