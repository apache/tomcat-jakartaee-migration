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

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class MigrationTest {

    @Test
    public void testMigrateSingleSourceFile() throws Exception {
        File migratedFile = new File("target/test-classes/HelloServlet.migrated.java");
        Migration.main(new String[] {"target/test-classes/HelloServlet.java", migratedFile.getAbsolutePath()});

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateSingleSourceFileWithProfile() throws Exception {
        File migratedFile = new File("target/test-classes/HelloServlet.migrated.java");
        Migration.main(new String[] {"-profile=EE", "target/test-classes/HelloServlet.java", migratedFile.getAbsolutePath()});

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }
}
