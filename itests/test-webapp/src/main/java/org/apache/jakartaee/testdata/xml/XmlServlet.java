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
package org.apache.jakartaee.testdata.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Sample test Servlet which internally parses and XML.<br><br>
 * Due to the way Tomcat is designed, if you have a JEE class under a (sub)package of
 * <code>org.apache.tomcat.**</code> then Tomcat will "filter it out" - As it believes you are
 * trying to access its own internal classes. We hence need to put our integration test classes
 * under the <code>org.apache.jakartaee.testdata</code> package.
 */
@WebServlet(urlPatterns = { "/test-xml-reader" })
public class XmlServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try (PrintWriter out = response.getWriter()) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            try (InputStream is = this.getClass().getResourceAsStream("/web.xml")) {
                Document webXml = builder.parse(is);
                NodeList description = webXml.getElementsByTagName("description");
                for (int i = 0; i < description.getLength(); i++) {
                    out.println(description.item(i).getNodeValue());
                }
            } catch (SAXException e) {
                throw new ServletException(e);
            }
        } catch (ParserConfigurationException e) {
            throw new ServletException(e);
        }
    }
}
