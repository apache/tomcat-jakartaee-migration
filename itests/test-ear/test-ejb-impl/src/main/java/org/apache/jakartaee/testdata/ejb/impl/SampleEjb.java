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
package org.apache.jakartaee.testdata.ejb.impl;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.jakartaee.testdata.ejb.api.SampleEjbApi;

/**
 * Implementation of the sample EJB.<br><br>
 * Due to the way Tomcat is designed, if you have a JEE class under a (sub)package of
 * <code>org.apache.tomcat.**</code> then Tomcat will "filter it out" - As it believes you are
 * trying to access its own internal classes. We hence need to put our integration test classes
 * under the <code>org.apache.jakartaee.testdata</code> package.
 */
@Local
@Remote
@Stateless
public class SampleEjb implements SampleEjbApi {
    @Override
    public String sayHello() {
        return "Hello, World!";
    }
}
