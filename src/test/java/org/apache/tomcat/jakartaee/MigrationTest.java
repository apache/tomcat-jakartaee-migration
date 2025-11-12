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
import java.nio.file.Files;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MigrationTest {

    private boolean securityManagerAvailable = true;

    @Before
    public void setUp() {
        try {
            System.setSecurityManager(new NoExitSecurityManager());
        } catch (Throwable t) {
            // Throws exception by default on newer Java versions
            securityManagerAvailable = false;
        }
    }

    @After
    public void tearDown() {
        try {
            System.setSecurityManager(null);
        } catch (Throwable t) {
            // Throws exception by default on newer Java versions
        }
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
        MigrationCLI.main(new String[] {"-logLevel=FINE", "-profile=EE", "target/test-classes/HelloServlet.java", migratedFile.getAbsolutePath()});

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
        Assume.assumeTrue(securityManagerAvailable);
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
        Assume.assumeTrue(securityManagerAvailable);
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
        migration.execute();

        assertTrue("Destination directory not found", destinationDirectory.exists());

        File migratedFile = new File("target/test-classes/migration/HelloServlet.java");
        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));

        File migratedSpiFile = new File("target/test-classes/migration/javax.enterprise.inject.spi.Extension");
        assertTrue("SPI file has not been migrated by renaming", migratedSpiFile.exists());

        String migratedSpiSource = FileUtils.readFileToString(migratedSpiFile, StandardCharsets.UTF_8);
        assertTrue("SPI file not copied with content", migratedSpiSource.contains("some.class.Reference"));
    }

    @Test
    public void testMigrateDirectoryWithEeProfile() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = new File("target/test-classes/migration-ee");

        Migration migration = new Migration();
        migration.setEESpecProfile(EESpecProfiles.EE);
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.execute();

        assertTrue("Destination directory not found", destinationDirectory.exists());

        File migratedFile = new File(destinationDirectory, "HelloServlet.java");
        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));

        File migratedSpiFile = new File(destinationDirectory, "jakarta.enterprise.inject.spi.Extension");
        assertTrue("SPI file not migrated by renaming", migratedSpiFile.exists());

        String migratedSpiSource = FileUtils.readFileToString(migratedSpiFile, StandardCharsets.UTF_8);
        assertTrue("SPI file not copied with content", migratedSpiSource.contains("some.class.Reference"));
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
        testMigrateJarFileInternal(false);
    }

    private void testMigrateJarFileInternal(boolean zipInMemory) throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = new File("target/test-classes/hellocgi-target.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(zipInMemory);
        migration.execute();

        File cgiapiFile = new File("target/test-classes/cgi-api.jar");
        URLClassLoader classloader = new URLClassLoader(new URL[]{jarFileTarget.toURI().toURL(), cgiapiFile.toURI().toURL()},ClassLoader.getSystemClassLoader().getParent());

        Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader);
        assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());

        // check the modification of the Implementation-Version manifest attribute
        try (JarFile jar = new JarFile(jarFileTarget)) {
            String implementationVersion = jar.getManifest().getMainAttributes().getValue("Implementation-Version");
            assertNotNull("Missing Implementation-Version manifest attribute", implementationVersion);
            assertNotEquals("Implementation-Version manifest attribute not changed", "1.2.3", implementationVersion);
            assertTrue("Implementation-Version manifest attribute doesn't match the expected pattern", implementationVersion.matches("1\\.2\\.3-migrated-[\\d\\.]+.*"));
        }

        assertTrue("hasConverted should be true", migration.hasConverted());
    }

    @Test
    public void testMigrateJarFileInMemory() throws Exception {
        testMigrateJarFileInternal(true);
    }

    @Test
    public void testHasConversionsThrowsWhenNotComplete() {
        Migration migration = new Migration();
        IllegalStateException exception = assertThrows(IllegalStateException.class, migration::hasConverted);
        assertEquals("Migration has not completed", exception.getMessage());
    }

    @Test
    public void testMigrateSignedJarFileRSA() throws Exception {
        testMigrateSignedJarFile("rsa", EESpecProfiles.TOMCAT);
    }

    @Test
    public void testMigrateSignedJarFileDSA() throws Exception {
        testMigrateSignedJarFile("dsa", EESpecProfiles.TOMCAT);
    }

    @Test
    public void testMigrateSignedJarFileEC() throws Exception {
        testMigrateSignedJarFile("ec", EESpecProfiles.TOMCAT);
    }

    @Test
    public void testNoopSignedJarFileRSA() throws Exception {
        testMigrateSignedJarFile("rsa", EESpecProfiles.JEE8);
    }

    @Test
    public void testNoopSignedJarFileDSA() throws Exception {
        testMigrateSignedJarFile("dsa", EESpecProfiles.JEE8);
    }

    @Test
    public void testNoopSignedJarFileEC() throws Exception {
        testMigrateSignedJarFile("ec", EESpecProfiles.JEE8);
    }

    private void testMigrateSignedJarFile(String algorithm, EESpecProfile profile) throws Exception {
        File jarFileSrc = new File("target/test-classes/hellocgi-signed-" + algorithm + ".jar");
        File jarFileTmp = new File("target/test-classes/hellocgi-signed-" + algorithm + "-tmp.jar");
        Files.copy(jarFileSrc.toPath(), jarFileTmp.toPath());

        Migration migration = new Migration();
        migration.setEESpecProfile(profile);
        migration.setSource(jarFileTmp);
        migration.setDestination(jarFileTmp);
        migration.execute();

        try (JarFile jar = new JarFile(jarFileTmp)) {
            if (profile == EESpecProfiles.JEE8) {
                assertNotNull("Digest removed from the manifest", jar.getManifest().getAttributes("org/apache/tomcat/jakartaee/HelloCGI.class"));
                assertNotNull("Signature key removed", jar.getEntry("META-INF/" + algorithm.toUpperCase() + "." + algorithm.toUpperCase()));
                assertNotNull("Signed manifest removed", jar.getEntry("META-INF/" + algorithm.toUpperCase() + ".SF"));
                assertFalse("The JAR was converted", migration.hasConverted());
            } else {
                assertNull("Digest not removed from the manifest", jar.getManifest().getAttributes("org/apache/tomcat/jakartaee/HelloCGI.class"));
                assertNull("Signature key not removed", jar.getEntry("META-INF/" + algorithm.toUpperCase() + "." + algorithm.toUpperCase()));
                assertNull("Signed manifest not removed", jar.getEntry("META-INF/" + algorithm.toUpperCase() + ".SF"));
                assertTrue("The JAR was not converted", migration.hasConverted());
            }
        } finally {
            assertTrue("Unable to delete " + jarFileTmp.getAbsolutePath(), jarFileTmp.delete());
        }
    }

    @Test
    public void testMigrateJarWithCache() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = new File("target/test-classes/hellocgi-cached.jar");
        File cacheDir = new File("target/test-classes/cache-test");

        try {
            // Clean up cache directory
            if (cacheDir.exists()) {
                FileUtils.deleteDirectory(cacheDir);
            }

            // First migration - cache miss
            Migration migration1 = new Migration();
            migration1.setSource(jarFile);
            migration1.setDestination(jarFileTarget);
            migration1.setCache(cacheDir);
            long startTime1 = System.currentTimeMillis();
            migration1.execute();
            long duration1 = System.currentTimeMillis() - startTime1;

            assertTrue("Target JAR should exist after first migration", jarFileTarget.exists());
            assertTrue("Cache directory should be created", cacheDir.exists());

            // Verify the migrated JAR works
            File cgiapiFile = new File("target/test-classes/cgi-api.jar");
            URLClassLoader classloader1 = new URLClassLoader(
                    new URL[]{jarFileTarget.toURI().toURL(), cgiapiFile.toURI().toURL()},
                    ClassLoader.getSystemClassLoader().getParent());
            Class<?> cls1 = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader1);
            assertEquals("jakarta.servlet.CommonGatewayInterface", cls1.getSuperclass().getName());

            // Delete target and migrate again - cache hit
            jarFileTarget.delete();
            assertFalse("Target should be deleted", jarFileTarget.exists());

            Migration migration2 = new Migration();
            migration2.setSource(jarFile);
            migration2.setDestination(jarFileTarget);
            migration2.setCache(cacheDir);
            long startTime2 = System.currentTimeMillis();
            migration2.execute();
            long duration2 = System.currentTimeMillis() - startTime2;

            assertTrue("Target JAR should exist after second migration", jarFileTarget.exists());

            // Verify the cached JAR works
            URLClassLoader classloader2 = new URLClassLoader(
                    new URL[]{jarFileTarget.toURI().toURL(), cgiapiFile.toURI().toURL()},
                    ClassLoader.getSystemClassLoader().getParent());
            Class<?> cls2 = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader2);
            assertEquals("jakarta.servlet.CommonGatewayInterface", cls2.getSuperclass().getName());

            // Note: We don't assert that duration2 < duration1 because the times are too short
            // and can vary. The important thing is both migrations work correctly.
        } finally {
            // Clean up
            if (cacheDir.exists()) {
                FileUtils.deleteDirectory(cacheDir);
            }
        }
    }

    @Test
    public void testMigrateJarWithCacheDisabled() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = new File("target/test-classes/hellocgi-nocache.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        // Don't set cache - should work without caching
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());

        File cgiapiFile = new File("target/test-classes/cgi-api.jar");
        URLClassLoader classloader = new URLClassLoader(
                new URL[]{jarFileTarget.toURI().toURL(), cgiapiFile.toURI().toURL()},
                ClassLoader.getSystemClassLoader().getParent());
        Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader);
        assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());
    }

    @Test
    public void testMigrateCLIWithCacheOption() throws Exception {
        File sourceFile = new File("target/test-classes/hellocgi.jar");
        File targetFile = new File("target/test-classes/hellocgi-cli-cached.jar");
        File cacheDir = new File("target/test-classes/cache-cli-test");

        try {
            // Clean up
            if (cacheDir.exists()) {
                FileUtils.deleteDirectory(cacheDir);
            }
            if (targetFile.exists()) {
                targetFile.delete();
            }

            // Run with custom cache
            MigrationCLI.main(new String[] {
                    "-cache=" + cacheDir.getAbsolutePath(),
                    sourceFile.getAbsolutePath(),
                    targetFile.getAbsolutePath()
            });

            assertTrue("Target file should exist", targetFile.exists());
            assertTrue("Cache directory should be created", cacheDir.exists());

            // Verify the migrated JAR works
            File cgiapiFile = new File("target/test-classes/cgi-api.jar");
            URLClassLoader classloader = new URLClassLoader(
                    new URL[]{targetFile.toURI().toURL(), cgiapiFile.toURI().toURL()},
                    ClassLoader.getSystemClassLoader().getParent());
            Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader);
            assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());
        } finally {
            // Clean up
            if (cacheDir.exists()) {
                FileUtils.deleteDirectory(cacheDir);
            }
        }
    }

    @Test
    public void testMigrateCLIWithNoCacheOption() throws Exception {
        File sourceFile = new File("target/test-classes/hellocgi.jar");
        File targetFile = new File("target/test-classes/hellocgi-cli-nocache.jar");

        if (targetFile.exists()) {
            targetFile.delete();
        }

        // Run with -noCache
        MigrationCLI.main(new String[] {
                "-noCache",
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist", targetFile.exists());

        // Verify the migrated JAR works
        File cgiapiFile = new File("target/test-classes/cgi-api.jar");
        URLClassLoader classloader = new URLClassLoader(
                new URL[]{targetFile.toURI().toURL(), cgiapiFile.toURI().toURL()},
                ClassLoader.getSystemClassLoader().getParent());
        Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader);
        assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());
    }
}
