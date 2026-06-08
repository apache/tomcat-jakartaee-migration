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

    @Test
    public void testMatchMultipleStars() {
        assertTrue(GlobMatcher.match("**", "a/b/c.txt", true));
        assertTrue(GlobMatcher.match("*/*/*.txt", "a/b/c.txt", true));
        assertTrue(GlobMatcher.match("*/*", "a/b", true));
        // Note: * matches any characters including /, so */* can match a/b/c
        assertTrue(GlobMatcher.match("*/*", "a/b/c", true));
    }

    @Test
    public void testMatchQuestionMark() {
        assertTrue(GlobMatcher.match("?.txt", "a.txt", true));
        assertTrue(GlobMatcher.match("a?.txt", "ab.txt", true));
        assertTrue(GlobMatcher.match("???.txt", "abc.txt", true));
        assertFalse(GlobMatcher.match("?.txt", "ab.txt", true));
        assertFalse(GlobMatcher.match("?.txt", ".txt", true));
    }

    @Test
    public void testMatchQuestionMarkWithStar() {
        assertTrue(GlobMatcher.match("?*.txt", "a.txt", true));
        assertTrue(GlobMatcher.match("?*.txt", "abc.txt", true));
        assertFalse(GlobMatcher.match("?*.txt", ".txt", true));
    }

    @Test
    public void testMatchCaseSensitive() {
        assertTrue(GlobMatcher.match("Test.txt", "Test.txt", true));
        assertFalse(GlobMatcher.match("Test.txt", "test.txt", true));
        assertFalse(GlobMatcher.match("TEST.TXT", "test.txt", true));
    }

    @Test
    public void testMatchCaseInsensitive() {
        assertTrue(GlobMatcher.match("Test.txt", "test.txt", false));
        assertTrue(GlobMatcher.match("TEST.TXT", "test.txt", false));
        assertTrue(GlobMatcher.match("TeSt.TxT", "test.txt", false));
    }

    @Test
    public void testMatchStarOnly() {
        assertTrue(GlobMatcher.match("*", "", true));
        assertTrue(GlobMatcher.match("*", "anything", true));
        assertTrue(GlobMatcher.match("*", "a/b/c", true));
    }

    @Test
    public void testMatchEmptyPattern() {
        assertTrue(GlobMatcher.match("", "", true));
        assertFalse(GlobMatcher.match("", "a", true));
    }

    @Test
    public void testMatchEmptyString() {
        assertTrue(GlobMatcher.match("*", "", true));
        assertFalse(GlobMatcher.match("a", "", true));
    }

    @Test
    public void testMatchStarAtEnd() {
        assertTrue(GlobMatcher.match("abc*", "abc", true));
        assertTrue(GlobMatcher.match("abc*", "abcdef", true));
        assertFalse(GlobMatcher.match("abc*", "abd", true));
    }

    @Test
    public void testMatchStarAtStart() {
        assertTrue(GlobMatcher.match("*def", "def", true));
        assertTrue(GlobMatcher.match("*def", "abcdef", true));
        assertFalse(GlobMatcher.match("*def", "abcdeg", true));
    }

    @Test
    public void testMatchStarInMiddle() {
        assertTrue(GlobMatcher.match("abc*def", "abcdef", true));
        assertTrue(GlobMatcher.match("abc*def", "abcXYZdef", true));
        assertFalse(GlobMatcher.match("abc*def", "abcXYZd", true));
    }

    @Test
    public void testMatchMultipleStarsAdjacent() {
        assertTrue(GlobMatcher.match("**", "anything", true));
        assertTrue(GlobMatcher.match("a**b", "ab", true));
        assertTrue(GlobMatcher.match("a**b", "aXXXb", true));
    }

    @Test
    public void testMatchQuestionMarkCaseInsensitive() {
        assertTrue(GlobMatcher.match("A?", "ab", false));
        assertTrue(GlobMatcher.match("A?", "AB", false));
        assertFalse(GlobMatcher.match("A?", "ab", true));
    }

    @Test
    public void testMatchComplexPattern() {
        assertTrue(GlobMatcher.match("*.*", "file.txt", true));
        assertTrue(GlobMatcher.match("*.*", "file.tar.gz", true));
        assertFalse(GlobMatcher.match("*.*", "file", true));
    }

    @Test
    public void testMatchPatternLongerThanString() {
        assertFalse(GlobMatcher.match("abcdef", "abc", true));
        assertTrue(GlobMatcher.match("abc*", "abc", true));
    }

    @Test
    public void testMatchStringLongerThanPattern() {
        assertFalse(GlobMatcher.match("abc", "abcdef", true));
        assertTrue(GlobMatcher.match("*", "abcdef", true));
    }

    @Test
    public void testMatchPatternWithOnlyQuestionMarks() {
        assertTrue(GlobMatcher.match("???", "abc", true));
        assertFalse(GlobMatcher.match("???", "ab", true));
        assertFalse(GlobMatcher.match("???", "abcd", true));
    }

    @Test
    public void testMatchMixedStarsAndQuestionMarks() {
        assertTrue(GlobMatcher.match("*?.txt", "ab.txt", true));
        assertTrue(GlobMatcher.match("*?.txt", "abc.txt", true));
        // *? means zero or more chars followed by exactly one char, so minimum 1 char before .txt
        assertTrue(GlobMatcher.match("*?.txt", "a.txt", true));
    }

    @Test
    public void testMatchPatternWithTrailingStar() {
        assertTrue(GlobMatcher.match("abc*", "abc", true));
        assertTrue(GlobMatcher.match("abc*", "abcdef", true));
        assertTrue(GlobMatcher.match("abc*def*", "abcdef", true));
        assertTrue(GlobMatcher.match("abc*def*", "abcdefXYZ", true));
    }

    @Test
    public void testMatchAllStarsRemaining() {
        assertTrue(GlobMatcher.match("a***", "a", true));
        assertTrue(GlobMatcher.match("a***", "aXXX", true));
    }

    @Test
    public void testMatchNoStarDifferentLength() {
        assertFalse(GlobMatcher.match("abc", "abcd", true));
        assertFalse(GlobMatcher.match("abcd", "abc", true));
    }

    @Test
    public void testMatchNoStarWithQuestionMark() {
        assertTrue(GlobMatcher.match("a?c", "abc", true));
        assertFalse(GlobMatcher.match("a?c", "ac", true));
        assertFalse(GlobMatcher.match("a?c", "abbc", true));
    }

    @Test
    public void testMatchNoStarExactMatch() {
        assertTrue(GlobMatcher.match("abc", "abc", true));
        assertFalse(GlobMatcher.match("abc", "ABC", true));
    }

    @Test
    public void testMatchStarWithQuestionMarkSuffix() {
        assertTrue(GlobMatcher.match("*?", "a", false));
        assertTrue(GlobMatcher.match("*?", "ab", false));
        assertFalse(GlobMatcher.match("*?", "", false));
    }

    @Test
    public void testMatchQuestionMarkPrefix() {
        assertTrue(GlobMatcher.match("?*?", "aba", true));
        // ?a? means: any char, then 'a', then any char
        assertTrue(GlobMatcher.match("?a?", "aaa", true));
        assertTrue(GlobMatcher.match("?a?", "bab", true));
        assertFalse(GlobMatcher.match("?a?", "aba", true));
        assertFalse(GlobMatcher.match("?a?", "a", true));
        assertFalse(GlobMatcher.match("?a?", "aa", true));
    }
}
