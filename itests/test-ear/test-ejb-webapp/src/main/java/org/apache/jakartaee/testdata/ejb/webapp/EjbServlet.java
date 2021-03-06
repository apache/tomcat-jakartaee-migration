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
package org.apache.jakartaee.testdata.ejb.webapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jakartaee.testdata.ejb.api.SampleEjbApi;

/**
 * Sample test Servlet with annotations which loads the test EJB.<br><br>
 * We cannot use the <code>org.apache.tomcat.jakartaee.testdata</code> package, else TomEE throws
 * {@link ClassNotFoundException} errors while loading the servlet.
 */
@WebServlet(urlPatterns = { "/" })
public class EjbServlet extends HttpServlet {
    @EJB
    private SampleEjbApi sampleEjb;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try (PrintWriter out = response.getWriter()) {
            out.print("The EJB said: " + sampleEjb.sayHello());
        }
    }
}
