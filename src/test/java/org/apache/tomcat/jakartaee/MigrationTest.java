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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;

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

    @Test
    public void testInvalidProfile() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File migratedFile = new File("target/test-classes/HelloServlet.migrated.java");

        try {
            MigrationCLI.main(new String[] {"-profile=JSERV", sourceFile.getAbsolutePath(), migratedFile.getAbsolutePath()});
            fail("No error code returned");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }

    @Test
    public void testMigrateDirectory() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = new File("target/test-classes/migration");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        boolean success = migration.execute();

        assertTrue("Migration failed", success);
        assertTrue("Destination directory not found", destinationDirectory.exists());

        File migratedFile = new File("target/test-classes/migration/HelloServlet.java");
        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateClassFile() throws Exception {
        File classFile = new File("target/test-classes/org/apache/tomcat/jakartaee/HelloCGI.class");

        Migration migration = new Migration();
        migration.setSource(classFile);
        migration.setDestination(classFile);
        migration.execute();

        Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI");
        assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());
    }

    @Test
    public void testMigrateJarFile() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFile);
        migration.execute();

        File cgiapiFile = new File("target/test-classes/cgi-api.jar");
        URLClassLoader classloader = new URLClassLoader(new URL[]{jarFile.toURI().toURL(), cgiapiFile.toURI().toURL()},ClassLoader.getSystemClassLoader().getParent());

        Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader);
        assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());

        // check the modification of the Implementation-Version manifest attribute
        JarFile jar = new JarFile(jarFile);
        String implementationVersion = jar.getManifest().getMainAttributes().getValue("Implementation-Version");
        assertNotNull("Missing Implementation-Version manifest attribute", implementationVersion);
        assertNotEquals("Implementation-Version manifest attribute not changed", "1.2.3", implementationVersion);
        assertTrue("Implementation-Version manifest attribute doesn't match the expected pattern", implementationVersion.matches("1\\.2\\.3-migrated-[\\d\\.]+.*"));
    }

    @Test
    public void testMigrateSignedJarFileRSA() throws Exception {
        testMigrateSignedJarFile("rsa");
    }

    @Test
    public void testMigrateSignedJarFileDSA() throws Exception {
        testMigrateSignedJarFile("dsa");
    }

    @Test
    public void testMigrateSignedJarFileEC() throws Exception {
        testMigrateSignedJarFile("ec");
    }

    private void testMigrateSignedJarFile(String algorithm) throws Exception {
        File jarFile = new File("target/test-classes/hellocgi-signed-" + algorithm + ".jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFile);
        migration.execute();

        JarFile jar = new JarFile(jarFile);
        assertNull("Digest not removed from the manifest", jar.getManifest().getAttributes("org/apache/tomcat/jakartaee/HelloCGI.class"));
        assertNull("Signature key not removed", jar.getEntry("META-INF/" + algorithm.toUpperCase() + "." + algorithm.toUpperCase()));
        assertNull("Signed manifest not removed", jar.getEntry("META-INF/" + algorithm.toUpperCase() + ".SF"));
    }
}
