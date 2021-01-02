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

import org.apache.tomcat.jakartaee.profile.RegexProfile;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Specification profile defining the replacements performed.
 */
// TODO?: add a CUSTOM profile passing a predicate impl based on system properties to control:
//       1. includes/excludes (as regex)
//       2. includes/excludes from the usage, opcode+config (GET_STATIC classname.fieldname for ex)
public enum EESpecProfile {

    TOMCAT(new RegexProfile("javax([/\\.](annotation(?![/\\.]processing)|ejb|el|mail|persistence|security[/\\.]auth[/\\.]message|servlet|transaction(?![/\\.]xa)|websocket))")),

    EE(new RegexProfile("javax([/\\.](activation|annotation(?![/\\.]processing)|batch|decorator|ejb|el|enterprise|faces|jms|json|jws|interceptor|inject|mail|persistence|"
                + "resource|security[/\\.](auth[/\\.]message|enterprise|jacc)|servlet|transaction(?![/\\.]xa)|validation|websocket|ws[/\\.]rs|"
                + "xml[/\\.](bind|namespace|registry|rpc|soap|stream|ws|XMLConstants)))"));

    private final Pattern javax;
    private Predicate<String> predicate;

    EESpecProfile(final Predicate<String> predicate) {
        this.predicate = predicate;
        this.javax = Pattern.compile("javax([/\\.])");
    }

    /**
     * @return the test on strings this profile handles - is it a javax or not string.
     */
    public Predicate<String> getPredicate() {
        return predicate;
    }

    /**
     * This method must be protected by the predicate normally.
     *
     * @param name replaces javax by jakarta.
     * @return the jakarta value or the same value if it was not containing javax.
     */
    public String doConvert(final String name) {
        return javax.matcher(name).replaceAll("jakarta$1");
    }

    /**
     * Shortcut method auto testing if there should be a conversion.
     * @param name the string to test.
     * @return the new value of the string (or the same if it does not need to change).
     */
    public String convert(final String name) {
        if (predicate.test(name)) {
            return doConvert(name);
        }
        return name;
    }
}
