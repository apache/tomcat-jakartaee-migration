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

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MigrationTest {

    @Before
    public void setUp() {
        System.setSecurityManager(new NoExitSecurityManager());
    }

    @After
    public void tearDown() {
        System.setSecurityManager(null);
    }

    @Test
    public void testMigrateSingleSourceFile() throws Exception {
        File migratedFile = new File("target/test-classes/HelloServlet.migrated.java");
        MigrationCLI.main(new String[] {"target/test-classes/HelloServlet.java", migratedFile.getAbsolutePath()});

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateSingleSourceFileWithProfile() throws Exception {
        File migratedFile = new File("target/test-classes/HelloServlet.migrated.java");
        MigrationCLI.main(new String[] {"-verbose", "-profile=EE", "target/test-classes/HelloServlet.java", migratedFile.getAbsolutePath()});

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateSingleSourceFileInPlace() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File migratedFile = new File("target/test-classes/HelloServlet.inplace.java");
        FileUtils.copyFile(sourceFile, migratedFile);

        MigrationCLI.main(new String[] {"-profile=EE", migratedFile.getAbsolutePath(), migratedFile.getAbsolutePath()});

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testInvalidOption() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File migratedFile = new File("target/test-classes/HelloServlet.migrated.java");

        try {
            MigrationCLI.main(new String[] {"-invalid", sourceFile.getAbsolutePath(), migratedFile.getAbsolutePath()});
            fail("No error code returned");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }
}
