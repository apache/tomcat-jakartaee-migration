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
package org.apache.tomcat.jakartaee.itests.jakartaee;

import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * Tests the Java EE application on a Jakarta EE container,
 * by checking that the index page returns a code 404.
 */
public class JavaEEServletsIT {
    /**
     * Base URL used in tests, includes in particular the container's port number.
     */
    private static String baseUrl;

    /**
     * Initialize tests by saving the base URL used in tests using the container's port number.
     */
    @BeforeClass
    public static void initializeTest() {
        String port = System.getProperty("servlet.port");
        JavaEEServletsIT.baseUrl = "http://localhost:" + port + "/java-ee-webapp/";
        try {
            new URL(JavaEEServletsIT.baseUrl);
        } catch (MalformedURLException e) {
            Assert.fail("Invalid base URL: " + JavaEEServletsIT.baseUrl + ": " + e);
        }
    }

    /**
     * Test a servlet on a given path.
     * @param path Servlet path.
     * @throws Exception if anything goes wrong.
     */
    private void testServlet(String path) throws Exception {
        URL url = new URL(JavaEEServletsIT.baseUrl + "test-" + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        Assert.assertEquals(404, connection.getResponseCode());
    }

    /**
     * Call the annotated test servlet.
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testAnnotatedServlet() throws Exception {
        testServlet("annotated-servlet");
    }

    /**
     * Call the datasource test servlet.
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testDataSourceServlet() throws Exception {
        testServlet("datasource");
    }

    /**
     * Call the JSM queue test servlet.
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testJmsQueueServlet() throws Exception {
        testServlet("jms-queue");
    }

    /**
     * Call the JSM topic test servlet.
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testJmsTopicServlet() throws Exception {
        testServlet("jms-topic");
    }

    /**
     * Call the mail session test servlet.
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testMailSessionServlet() throws Exception {
        testServlet("mailsession");
    }
}
