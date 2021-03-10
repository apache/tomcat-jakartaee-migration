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
package org.apache.jakartaee.testdata.datasource;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Sample test Servlet used to verify that jdbc datasource is deployed.<br><br>
 * Due to the way Tomcat is designed, if you have a JEE class under a (sub)package of
 * <code>org.apache.tomcat.**</code> then Tomcat will "filter it out" - As it believes you are
 * trying to access its own internal classes. We hence need to put our integration test classes
 * under the <code>org.apache.jakartaee.testdata</code> package.
 */
public class DataSourceServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        Connection c = null;
        try {
            DataSource ds =
                (DataSource) new InitialContext().lookup("java:comp/env/jdbc/TestDS");
            c = ds.getConnection();
            PrintWriter out = response.getWriter();
            out.print("Got connection!");
            out.close();
        } catch (NamingException e) {
            throw new ServletException(e);
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
    }
}
