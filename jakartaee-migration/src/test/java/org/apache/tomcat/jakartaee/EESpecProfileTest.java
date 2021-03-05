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

import org.junit.Test;

import static org.junit.Assert.*;

public class EESpecProfileTest {

    @Test
    public void testProfileTomcat() {
        EESpecProfile profile = EESpecProfile.TOMCAT;

        assertEquals("jakarta.annotation", profile.convert("javax.annotation"));
        assertEquals("jakarta.ejb", profile.convert("javax.ejb"));
        assertEquals("jakarta.el", profile.convert("javax.el"));
        assertEquals("jakarta.mail", profile.convert("javax.mail"));
        assertEquals("jakarta.persistence", profile.convert("javax.persistence"));
        assertEquals("jakarta.security.auth.message", profile.convert("javax.security.auth.message"));
        assertEquals("jakarta.servlet", profile.convert("javax.servlet"));
        assertEquals("jakarta.transaction", profile.convert("javax.transaction"));
        assertEquals("jakarta.websocket", profile.convert("javax.websocket"));

        // not converted EE packages
        assertEquals("javax.activation", profile.convert("javax.activation"));
        assertEquals("javax.batch", profile.convert("javax.batch"));
        assertEquals("javax.decorator", profile.convert("javax.decorator"));
        assertEquals("javax.enterprise", profile.convert("javax.enterprise"));
        assertEquals("javax.faces", profile.convert("javax.faces"));
        assertEquals("javax.jms", profile.convert("javax.jms"));
        assertEquals("javax.json", profile.convert("javax.json"));
        assertEquals("javax.jws", profile.convert("javax.jws"));
        assertEquals("javax.interceptor", profile.convert("javax.interceptor"));
        assertEquals("javax.inject", profile.convert("javax.inject"));
        assertEquals("javax.management.j2ee", profile.convert("javax.management.j2ee"));
        assertEquals("javax.resource", profile.convert("javax.resource"));
        assertEquals("javax.security.enterprise", profile.convert("javax.security.enterprise"));
        assertEquals("javax.security.jacc", profile.convert("javax.security.jacc"));
        assertEquals("javax.validation", profile.convert("javax.validation"));
        assertEquals("javax.ws.rs", profile.convert("javax.ws.rs"));
        assertEquals("javax.xml.bind", profile.convert("javax.xml.bind"));
        assertEquals("javax.xml.rpc", profile.convert("javax.xml.rpc"));
        assertEquals("javax.xml.registry", profile.convert("javax.xml.registry"));
        assertEquals("javax.xml.soap", profile.convert("javax.xml.soap"));
        assertEquals("javax.xml.ws", profile.convert("javax.xml.ws"));

        // non EE javax packages
        assertEquals("javax.annotation.processing", profile.convert("javax.annotation.processing"));
        assertEquals("javax.management", profile.convert("javax.management"));
        assertEquals("javax.security", profile.convert("javax.security"));
        assertEquals("javax.security.auth", profile.convert("javax.security.auth"));
        assertEquals("javax.swing", profile.convert("javax.swing"));
        assertEquals("javax.transaction.xa", profile.convert("javax.transaction.xa"));
        assertEquals("javax.xml.stream", profile.convert("javax.xml.stream"));
        assertEquals("javax.xml.namespace", profile.convert("javax.xml.namespace"));
        assertEquals("javax.xml.xpath.XPathConstants", profile.convert("javax.xml.xpath.XPathConstants"));
        assertEquals("javax.xml.XMLConstants", profile.convert("javax.xml.XMLConstants"));
    }

    @Test
    public void testProfileEE() {
        EESpecProfile profile = EESpecProfile.EE;

        assertEquals("jakarta.activation", profile.convert("javax.activation"));
        assertEquals("jakarta.annotation", profile.convert("javax.annotation"));
        assertEquals("jakarta.batch", profile.convert("javax.batch"));
        assertEquals("jakarta.decorator", profile.convert("javax.decorator"));
        assertEquals("jakarta.ejb", profile.convert("javax.ejb"));
        assertEquals("jakarta.el", profile.convert("javax.el"));
        assertEquals("jakarta.enterprise", profile.convert("javax.enterprise"));
        assertEquals("jakarta.faces", profile.convert("javax.faces"));
        assertEquals("jakarta.jms", profile.convert("javax.jms"));
        assertEquals("jakarta.json", profile.convert("javax.json"));
        assertEquals("jakarta.jws", profile.convert("javax.jws"));
        assertEquals("jakarta.interceptor", profile.convert("javax.interceptor"));
        assertEquals("jakarta.inject", profile.convert("javax.inject"));
        assertEquals("jakarta.mail", profile.convert("javax.mail"));
        assertEquals("jakarta.management.j2ee", profile.convert("javax.management.j2ee"));
        assertEquals("jakarta.persistence", profile.convert("javax.persistence"));
        assertEquals("jakarta.resource", profile.convert("javax.resource"));
        assertEquals("jakarta.security.auth.message", profile.convert("javax.security.auth.message"));
        assertEquals("jakarta.security.enterprise", profile.convert("javax.security.enterprise"));
        assertEquals("jakarta.security.jacc", profile.convert("javax.security.jacc"));
        assertEquals("jakarta.servlet", profile.convert("javax.servlet"));
        assertEquals("jakarta.transaction", profile.convert("javax.transaction"));
        assertEquals("jakarta.validation", profile.convert("javax.validation"));
        assertEquals("jakarta.websocket", profile.convert("javax.websocket"));
        assertEquals("jakarta.ws.rs", profile.convert("javax.ws.rs"));
        assertEquals("jakarta.xml.bind", profile.convert("javax.xml.bind"));
        assertEquals("jakarta.xml.soap", profile.convert("javax.xml.soap"));
        assertEquals("jakarta.xml.ws", profile.convert("javax.xml.ws"));

        // non EE javax packages
        assertEquals("javax.annotation.processing", profile.convert("javax.annotation.processing"));
        assertEquals("javax.management", profile.convert("javax.management"));
        assertEquals("javax.security", profile.convert("javax.security"));
        assertEquals("javax.security.auth", profile.convert("javax.security.auth"));
        assertEquals("javax.swing", profile.convert("javax.swing"));
        assertEquals("javax.transaction.xa", profile.convert("javax.transaction.xa"));
        assertEquals("javax.xml.stream", profile.convert("javax.xml.stream"));
        assertEquals("javax.xml.namespace", profile.convert("javax.xml.namespace"));
        assertEquals("javax.xml.registry", profile.convert("javax.xml.registry"));
        assertEquals("javax.xml.rpc", profile.convert("javax.xml.rpc"));
        assertEquals("javax.xml.xpath.XPathConstants", profile.convert("javax.xml.xpath.XPathConstants"));
        assertEquals("javax.xml.XMLConstants", profile.convert("javax.xml.XMLConstants"));

    }
}
