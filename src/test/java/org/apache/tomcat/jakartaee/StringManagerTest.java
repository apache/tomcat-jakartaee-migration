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

import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringManagerTest {

    @Test
    public void testGetStringWithNullKey() {
        StringManager sm = StringManager.getManager(StringManagerTest.class);
        try {
            sm.getString(null);
            fail("Should throw IllegalArgumentException for null key");
        } catch (IllegalArgumentException e) {
            assertEquals("key may not have a null value", e.getMessage());
        }
    }

    @Test
    public void testGetStringExistingKey() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("migration.notCompleted");
        assertEquals("Migration has not completed", result);
    }

    @Test
    public void testGetStringMissingKey() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("nonexistent.key.12345");
        assertNull("Missing key should return null", result);
    }

    @Test
    public void testGetStringWithArgsExistingKey() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("migration.execute",
                "/source/path", "/dest/path", "TOMCAT");
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain source path", result.contains("/source/path"));
        assertTrue("Result should contain dest path", result.contains("/dest/path"));
        assertTrue("Result should contain profile", result.contains("TOMCAT"));
    }

    @Test
    public void testGetStringWithArgsMissingKey() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("nonexistent.key.12345", "arg1", "arg2");
        assertEquals("Missing key with args should return the key",
                "nonexistent.key.12345", result);
    }

    @Test
    public void testGetStringWithArgsNoArgs() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("migration.notCompleted");
        assertEquals("Migration has not completed", result);
    }

    @Test
    public void testGetManagerByClass() {
        StringManager sm1 = StringManager.getManager(Migration.class);
        StringManager sm2 = StringManager.getManager(Migration.class);
        assertSame("Same package should return same manager", sm1, sm2);
    }

    @Test
    public void testGetManagerByPackageName() {
        StringManager sm1 = StringManager.getManager("org.apache.tomcat.jakartaee");
        StringManager sm2 = StringManager.getManager("org.apache.tomcat.jakartaee");
        assertSame("Same package should return same manager", sm1, sm2);
    }

    @Test
    public void testGetManagerByPackageAndLocale() {
        StringManager sm1 = StringManager.getManager("org.apache.tomcat.jakartaee", Locale.ENGLISH);
        StringManager sm2 = StringManager.getManager("org.apache.tomcat.jakartaee", Locale.ENGLISH);
        assertSame("Same package and locale should return same manager", sm1, sm2);
    }

    @Test
    public void testGetManagerDifferentLocale() {
        StringManager sm1 = StringManager.getManager("org.apache.tomcat.jakartaee", Locale.ENGLISH);
        StringManager sm2 = StringManager.getManager("org.apache.tomcat.jakartaee", Locale.FRANCE);
        // May or may not be the same depending on available bundles
        assertNotNull("Manager should not be null", sm1);
        assertNotNull("Manager should not be null", sm2);
    }

    @Test
    public void testGetManagerNonExistentPackage() {
        StringManager sm = StringManager.getManager("nonexistent.package.test");
        assertNotNull("Manager should not be null for non-existent package", sm);
        assertNull("Missing key should return null for non-existent package",
                sm.getString("any.key"));
    }

    @Test
    public void testGetStringFormatWithMultipleArgs() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("migration.cannotReadSource", "/path/to/file");
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain file path", result.contains("/path/to/file"));
    }

    @Test
    public void testGetStringWithEmptyArgs() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("migration.notCompleted", new Object[0]);
        assertEquals("Migration has not completed", result);
    }

    @Test
    public void testGetStringFormatWithNullArg() {
        StringManager sm = StringManager.getManager(Migration.class);
        String result = sm.getString("migration.cannotReadSource", (String) null);
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain null representation",
                result.contains("null") || result.contains("{0}"));
    }
}
