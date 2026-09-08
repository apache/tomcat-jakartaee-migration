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

/**
 * Test fixture whose constants exist to embed {@code javax} class names in
 * the constant pool of this class file.
 * <p>
 * {@link ClassConverterTest} converts that class file and checks which of the
 * embedded strings were changed. The {@code JAVA_PRESENT_} constants reference
 * a class that exists in the {@code jakarta} namespace, so the converter must
 * rewrite them. The {@code JAVA_NOT_PRESENT_} constants reference a class that
 * does not exist in either namespace, so the converter must leave them
 * unchanged.
 * <p>
 * The {@code MULTI_FRAGMENT_} constants mimic a bytecode method descriptor:
 * multiple {@code ;}-delimited class name fragments in a single constant.
 * They exercise the loader-guided per-fragment conversion/reversion in
 * {@link ClassConverter#convertInternal}, which must preserve the {@code ;}
 * delimiters between fragments regardless of which fragments are converted,
 * reverted, or a mix of both.
 */
public class TesterConstants {

    public static final String JAVA_PRESENT_DOT = "javax.servlet.CommonGatewayInterface";
    public static final String JAVA_PRESENT_PATH = "javax/servlet/CommonGatewayInterface";
    public static final String JAVA_NOT_PRESENT_DOT = "javax.servlet.DoesNotExist";
    public static final String JAVA_NOT_PRESENT_PATH = "javax/servlet/DoesNotExist";

    // One fragment resolves in jakarta (must convert), the other does not
    // (must revert) - the ';' delimiters between and around them must survive.
    public static final String MULTI_FRAGMENT_PARTIAL =
            "(Ljavax/servlet/CommonGatewayInterface;Ljavax/servlet/DoesNotExist;)V";

    // Neither fragment resolves in jakarta, so the whole value must revert to
    // exactly the original string, delimiters included.
    public static final String MULTI_FRAGMENT_ALL_MISSING =
            "(Ljavax/servlet/DoesNotExist;Ljavax/servlet/DoesNotExist;)V";
}
