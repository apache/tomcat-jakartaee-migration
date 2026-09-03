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
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class MigrationTaskTest {

    private Project project;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        project.init();

        File buildFile = new File("target/test-classes/testbuild.xml");
        project.setBaseDir(buildFile.getParentFile());

        final ProjectHelper helper = ProjectHelper.getProjectHelper();
        helper.parse(project, buildFile);

        redirectOutput(System.out);
    }

    /**
     * Redirects the Ant output to the specified stream.
     */
    private void redirectOutput(OutputStream out) {
        DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(new PrintStream(out));
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(logger);
    }

    @Test
    public void testInvalidProfile() {
        try {
            project.executeTarget("invalid-profile");
            fail("Should throw BuildException when profile is invalid");
        } catch (BuildException e) {
            assertTrue("Error should mention the invalid profile",
                    e.getMessage().contains("jserv"));
        }
    }

    @Test
    public void testMigrateSingleSourceFile() throws Exception {
        project.executeTarget("migrate-single-source-file");

        File migratedFile = new File("target/test-classes/HelloServlet.migrated-by-ant.java");

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrationTaskNoSource() {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setDest(new File("target/test-classes/output.java"));

        try {
            task.execute();
            fail("Should throw BuildException when source is null");
        } catch (BuildException e) {
            assertTrue("Error should mention source", e.getMessage().contains("source"));
        }
    }

    @Test
    public void testMigrationTaskNoDest() {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setSrc(new File("target/test-classes/HelloServlet.java"));

        try {
            task.execute();
            fail("Should throw BuildException when dest is null");
        } catch (BuildException e) {
            assertTrue("Error should mention destination", e.getMessage().contains("dest"));
        }
    }

    @Test
    public void testMigrationTaskSourceNotExists() {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setSrc(new File("target/test-classes/nonexistent.java"));
        task.setDest(new File("target/test-classes/output.java"));

        try {
            task.execute();
            fail("Should throw BuildException when source does not exist");
        } catch (BuildException e) {
            assertTrue("Error should mention source", e.getMessage().contains("source"));
        }
    }

    @Test
    public void testMigrationTaskWithZipInMemory() throws Exception {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setSrc(new File("target/test-classes/hellocgi.jar"));
        File destFile = tempFolder.newFile("ant-zip-memory.jar");
        task.setDest(destFile);
        task.setZipInMemory(true);
        task.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        try (JarFile jar = new JarFile(destFile)) {
            JarEntry entry = jar.getJarEntry("org/apache/tomcat/jakartaee/HelloCGI.class");
            assertNotNull("HelloCGI class not found in migrated JAR", entry);
            byte[] classBytes = IOUtils.toByteArray(jar.getInputStream(entry));
            assertTrue("Class should be migrated",
                    new String(classBytes, StandardCharsets.ISO_8859_1)
                            .contains("jakarta/servlet/CommonGatewayInterface"));
        }
    }

    @Test
    public void testMigrationTaskWithExcludes() throws Exception {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setSrc(new File("target/test-classes/HelloServlet.java"));
        File destFile = tempFolder.newFile("ant-excludes.java");
        task.setDest(destFile);
        task.setExcludes("HelloServlet.java");
        task.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        String content = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Excluded file should not be converted", content.contains("import javax.servlet"));
    }

    @Test
    public void testMigrationTaskWithMatchExcludesAgainstPathName() throws Exception {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setSrc(new File("target/test-classes/HelloServlet.java"));
        File destFile = tempFolder.newFile("ant-path-excludes.java");
        task.setDest(destFile);
        // Matches the full path but not the bare file name, so the exclude
        // only takes effect when excludes are matched against the path name
        task.setExcludes("**" + File.separator + "HelloServlet.java");
        task.setMatchExcludesAgainstPathName(true);
        task.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        String content = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Excluded file should not be converted", content.contains("import javax.servlet"));
    }

    @Test
    public void testMigrationTaskWithEeProfile() throws Exception {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setSrc(new File("target/test-classes/HelloServlet.java"));
        File destFile = tempFolder.newFile("ant-ee-profile.java");
        task.setDest(destFile);
        task.setProfile("ee");
        task.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        String migratedSource = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Imports should be migrated", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrationTaskCloneThrows() throws Exception {
        MigrationTask task = new MigrationTask();
        try {
            task.clone();
            fail("Should throw CloneNotSupportedException");
        } catch (CloneNotSupportedException e) {
            // Expected
        }
    }

    @Test
    public void testMigrationTaskDefaultProfile() throws Exception {
        MigrationTask task = new MigrationTask();
        task.setProject(project);
        task.setSrc(new File("target/test-classes/HelloServlet.java"));
        File destFile = tempFolder.newFile("ant-default-profile.java");
        task.setDest(destFile);
        task.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        String migratedSource = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Imports should be migrated with default TOMCAT profile",
                migratedSource.contains("import jakarta.servlet"));
    }
}
