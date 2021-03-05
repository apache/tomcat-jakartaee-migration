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
package org.apache.jakartaee.testdata.jms;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sample test Servlet used to verify that resource JMS queue is deployed.<br><br>
 * We cannot use the <code>org.apache.tomcat.jakartaee.testdata</code> package, else TomEE throws
 * {@link ClassNotFoundException} errors while loading the servlet.
 */
public class JmsQueueServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        Object o = null;
        try {
            o = new InitialContext().lookup("java:comp/env/jms/MyQueue");
            ((Queue) o).getQueueName();
            PrintWriter out = response.getWriter();
            out.print("Got queue!");
            out.close();
        } catch (ClassCastException e) {
            throw new ServletException("Got " + o.getClass() + " instead of JMS Queue", e);
        } catch (JMSException|NamingException e) {
            throw new ServletException(e);
        }
    }
}
