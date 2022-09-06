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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

public class GlobMatcherTest {

    private static final String FILE_A1 = "fileA1.txt";
    private static final String FILE_A2 = "fileA2.txt";
    private static final Set<String> SET_A = new HashSet<>();
    
    private static final String FILE_A_ANY = "fileA?.txt";
    private static final Set<String> SET_A_ANY = new HashSet<>();

    private static final String OTHER = "other.txt"; 

    private static final Set<String> SET_ALL = new HashSet<>();

    static {
        SET_A.add(FILE_A1);
        SET_A.add(FILE_A2);
        
        SET_A_ANY.add(FILE_A_ANY);
        
        SET_ALL.add("*");
    }
    
    @Test
    public void testMatchSimple() {
        assertTrue(GlobMatcher.matchName(SET_A, FILE_A1, false));
    }
    
    @Test
    public void testMatchLowerCase() {
        assertTrue(GlobMatcher.matchName(SET_A, FILE_A1.toLowerCase(Locale.ENGLISH), false));
    }

    @Test
    public void testMatchUpperCase() {
        assertTrue(GlobMatcher.matchName(SET_A, FILE_A1.toUpperCase(Locale.ENGLISH), false));
    }

    @Test
    public void testNonMatchLowerCase() {
        assertFalse(GlobMatcher.matchName(SET_A, FILE_A1.toLowerCase(Locale.ENGLISH), true));
    }

    @Test
    public void testNonMatchUpperCase() {
        assertFalse(GlobMatcher.matchName(SET_A, FILE_A1.toUpperCase(Locale.ENGLISH), true));
    }

    @Test
    public void testNonMatchSimple() {
        assertFalse(GlobMatcher.matchName(SET_A, OTHER, true));
    }

    @Test
    public void testMatchAny() {
        assertTrue(GlobMatcher.matchName(SET_A_ANY, FILE_A1, false));
    }

    @Test
    public void testMatchAll() {
        assertTrue(GlobMatcher.matchName(SET_ALL, FILE_A1, false));
    }
}
