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
package org.apache.tomcat.jakartaee.profile;

import org.apache.bcel.classfile.Utility;
import org.apache.tomcat.jakartaee.XmlExclusionListBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class XmlExclusionAwareProfile implements Predicate<String> {
    private final Predicate<String> delegate;
    private final XmlExclusionListBuilder xmlExclusions;

    public XmlExclusionAwareProfile(final Predicate<String> delegate, final XmlExclusionListBuilder xmlExclusions) {
        this.delegate = delegate;
        this.xmlExclusions = xmlExclusions;
    }

    @Override
    public boolean test(final String s) {
        return toClassNames(s).noneMatch(xmlExclusions.getStayInJavax()::contains) && delegate.test(s);
    }

    private Stream<String> toClassNames(final String str) {
        if (str.startsWith("<") && str.endsWith(">")) {
            return Stream.empty();
        }
        try {
            if (str.startsWith("(")) {
                return Stream.of(Utility.methodSignatureArgumentTypes(str, false));
            }
            return Stream.of(Utility.typeSignatureToString(str, false));
        } catch (final RuntimeException re) {
            return Stream.of();
        }
    }
}
