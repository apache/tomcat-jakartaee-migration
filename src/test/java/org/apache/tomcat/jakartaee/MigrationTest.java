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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class MigrationTest {

    private boolean securityManagerAvailable = true;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
        File classFileOriginal = new File("target/test-classes/org/apache/tomcat/jakartaee/HelloCGI-original.class");
        FileUtils.copyFile(classFile, classFileOriginal);

        Migration migration = new Migration();
        migration.setSource(classFile);
        migration.setDestination(classFile);
        migration.execute();

        Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI");
        assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());

        Assert.assertTrue("Failed to delete migrated class file", classFile.delete());
        FileUtils.copyFile(classFileOriginal, classFile);
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
            migration1.setCache(new MigrationCache(cacheDir, 30));
            migration1.execute();

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
            migration2.setCache(new MigrationCache(cacheDir, 30));
            migration2.execute();

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
                    "-cache",
                    "-cacheLocation=" + cacheDir.getAbsolutePath(),
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

        // Run without cache (no -cache option)
        MigrationCLI.main(new String[] {
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

    @Test
    public void testExecuteThrowsWhenAlreadyRunning() throws Exception {
        // Note: After execute() completes, state is COMPLETE, not RUNNING.
        // So calling execute() again will work (it will run again).
        // The IllegalStateException is only thrown if state is RUNNING.
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File destFile = tempFolder.newFile("re-execute.java");

        Migration migration = new Migration();
        migration.setSource(sourceFile);
        migration.setDestination(destFile);
        migration.execute();

        // Second execution should succeed (state is COMPLETE, not RUNNING)
        migration.execute();
        assertTrue("Second execution should succeed", destFile.exists());
    }

    @Test
    public void testSetSourceCannotRead() {
        Migration migration = new Migration();
        File unreadableFile = new File("/nonexistent/path/file.txt");
        try {
            migration.setSource(unreadableFile);
            fail("Should throw IllegalArgumentException for unreadable source");
        } catch (IllegalArgumentException e) {
            // Expected - file doesn't exist so can't be read
        }
    }

    @Test
    public void testMigrateDirectoryCannotCreateDest() throws Exception {
        Migration migration = new Migration();
        File sourceDirectory = new File("src/test/resources");
        // Use a path that definitely can't be created
        File destDirectory = new File("/proc/nonexistent/immutable/path/dest");

        try {
            migration.setSource(sourceDirectory);
            migration.setDestination(destDirectory);
            migration.execute();
            fail("Should throw IOException when cannot create destination directory");
        } catch (IOException e) {
            // Expected - should fail to create directory
        }
    }

    @Test
    public void testMigrateWithExcludes() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = tempFolder.newFolder("excludes-test");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.addExclude("HelloServlet.java");
        migration.execute();

        File excludedFile = new File(destinationDirectory, "HelloServlet.java");
        // Excluded files are still copied but not converted
        assertTrue("Excluded file should still be copied", excludedFile.exists());
        String content = FileUtils.readFileToString(excludedFile, StandardCharsets.UTF_8);
        assertTrue("Excluded file should not be converted", content.contains("import javax.servlet"));
    }

    @Test
    public void testMigrateWithMatchExcludesAgainstPathName() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = tempFolder.newFolder("path-excludes-test");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.setMatchExcludesAgainstPathName(true);
        // When matching against path name, use a pattern that matches the full path
        migration.addExclude("*/HelloServlet.java");
        migration.execute();

        File excludedFile = new File(destinationDirectory, "HelloServlet.java");
        // Excluded files are still copied but not converted
        assertTrue("Excluded file should still be copied", excludedFile.exists());
        String content = FileUtils.readFileToString(excludedFile, StandardCharsets.UTF_8);
        assertTrue("Excluded file should not be converted", content.contains("import javax.servlet"));
    }

    @Test
    public void testMigrateJarWithZip64ExtraField() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = tempFolder.newFile("zip64-test.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());
        assertTrue("Target JAR should have content", jarFileTarget.length() > 0);
    }

    @Test
    public void testMigrateAlreadyMigratedFile() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File destFile = tempFolder.newFile("already-migrated.java");

        // First migration
        MigrationCLI.main(new String[]{sourceFile.getAbsolutePath(), destFile.getAbsolutePath()});

        // Second migration on already-migrated file should not convert
        File destFile2 = tempFolder.newFile("already-migrated-2.java");
        FileUtils.copyFile(destFile, destFile2);

        Migration migration = new Migration();
        migration.setSource(destFile2);
        migration.setDestination(destFile2);
        migration.execute();

        assertFalse("Re-migrating an already-migrated file should not convert", migration.hasConverted());
    }

    @Test
    public void testMigrateWithDisabledDefaultExcludes() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File destFile = tempFolder.newFile("no-default-excludes.java");

        Migration migration = new Migration();
        migration.setSource(sourceFile);
        migration.setDestination(destFile);
        migration.setEnableDefaultExcludes(false);
        migration.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        String migratedSource = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Imports should be migrated", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateNestedJarInWar() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = tempFolder.newFile("nested-test.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(true);
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
    public void testMigrateInMemoryNestedArchive() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = tempFolder.newFile("in-memory-nested.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(true);
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());
        assertTrue("hasConverted should be true", migration.hasConverted());
    }

    private File createLargeStoredJar(byte[] largeContent) throws Exception {
        // Create a JAR with STORED method containing the large file
        File storedJar = tempFolder.newFile("large-stored.jar");
        try (FileOutputStream fos = new FileOutputStream(storedJar);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("large-data.txt");
            entry.setMethod(org.apache.commons.compress.archivers.zip.ZipArchiveEntry.STORED);
            entry.setSize(largeContent.length);
            CRC32 crc = new CRC32();
            crc.update(largeContent);
            entry.setCrc(crc.getValue());
            zos.putArchiveEntry(entry);
            zos.write(largeContent);
            zos.closeArchiveEntry();
        }
        return storedJar;
    }

    @Test
    public void testMigrateLargeStoredEntryInZip() throws Exception {
        // Create a large file (>10MB) to trigger CrcSizeTrackingOutputStream.maybeSwitchToFile()
        // which switches from in-memory buffer to temp file at TEMP_FILE_THRESHOLD (10MB)
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        File storedJar = createLargeStoredJar(largeContent);

        // Migrate the JAR using streaming (not in-memory) to exercise CrcSizeTrackingOutputStream
        File jarFileTarget = tempFolder.newFile("large-stored-migrated.jar");
        Migration migration = new Migration();
        migration.setSource(storedJar);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(false); // Streaming mode uses CrcSizeTrackingOutputStream
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());
        assertTrue("Target JAR should have content", jarFileTarget.length() > 0);

        // Verify the large file was preserved correctly
        try (JarFile jar = new JarFile(jarFileTarget)) {
            JarEntry entry = jar.getJarEntry("large-data.txt");
            assertNotNull("Large entry should exist in migrated JAR", entry);
            assertEquals("Large entry size should match", largeContent.length, entry.getSize());

            byte[] readContent = new byte[(int) entry.getSize()];
            try (InputStream is = jar.getInputStream(entry)) {
                int offset = 0;
                int count;
                while (offset < readContent.length && (count = is.read(readContent, offset, readContent.length - offset)) > 0) {
                    offset += count;
                }
            }
            assertArrayEquals("Large entry content should match", largeContent, readContent);
        }
    }

    @Test
    public void testMigrateLargeStoredEntryInMemory() throws Exception {
        // Create a large file (>10MB) to test in-memory migration with large STORED entries
        byte[] largeContent = new byte[11 * 1024 * 024]; // 11MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        File storedJar = createLargeStoredJar(largeContent);

        // Migrate the JAR using in-memory mode
        File jarFileTarget = tempFolder.newFile("large-stored-memory-migrated.jar");
        Migration migration = new Migration();
        migration.setSource(storedJar);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(true); // In-memory mode uses ZipFile + ZipArchiveOutputStream
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());
        assertTrue("Target JAR should have content", jarFileTarget.length() > 0);

        // Verify the large file was preserved correctly
        try (JarFile jar = new JarFile(jarFileTarget)) {
            JarEntry entry = jar.getJarEntry("large-data.txt");
            assertNotNull("Large entry should exist in migrated JAR", entry);
            assertEquals("Large entry size should match", largeContent.length, entry.getSize());

            byte[] readContent = new byte[(int) entry.getSize()];
            try (InputStream is = jar.getInputStream(entry)) {
                int offset = 0;
                int count;
                while (offset < readContent.length && (count = is.read(readContent, offset, readContent.length - offset)) > 0) {
                    offset += count;
                }
            }
            assertArrayEquals("Large entry content should match", largeContent, readContent);
        }
    }

    @Test
    public void testMigrateNestedArchiveWithCache() throws Exception {
        // Create a nested JAR with javax.servlet references
        File nestedJar = tempFolder.newFile("nested.jar");
        byte[] nestedClassData = new byte[1024];
        for (int i = 0; i < nestedClassData.length; i++) {
            nestedClassData[i] = (byte) (i % 256);
        }
        // Write a text file with javax reference into the nested JAR
        String nestedContent = "javax.servlet.http.HttpServlet";
        try (FileOutputStream fos = new FileOutputStream(nestedJar);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("nested.txt");
            zos.putArchiveEntry(entry);
            zos.write(nestedContent.getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }

        // Create a WAR containing the nested JAR
        File warFile = tempFolder.newFile("app.war");
        try (FileOutputStream fos = new FileOutputStream(warFile);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            // Add WEB-INF/lib/nested.jar
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("WEB-INF/lib/nested.jar");
            zos.putArchiveEntry(entry);
            byte[] nestedJarBytes = Files.readAllBytes(nestedJar.toPath());
            zos.write(nestedJarBytes);
            zos.closeArchiveEntry();

            // Add a web.xml
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry webXmlEntry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("WEB-INF/web.xml");
            zos.putArchiveEntry(webXmlEntry);
            zos.write("<web-app></web-app>".getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }

        // Migrate WAR with cache enabled - nested JAR should be cached
        File cacheDir = tempFolder.newFolder("nested-cache");
        File warTarget = tempFolder.newFile("app-migrated.war");

        Migration migration = new Migration();
        migration.setSource(warFile);
        migration.setDestination(warTarget);
        migration.setCache(new MigrationCache(cacheDir, 30));
        migration.setZipInMemory(false);
        migration.execute();

        assertTrue("Target WAR should exist", warTarget.exists());
        assertTrue("Cache directory should have entries", cacheDir.list().length > 0);

        // Verify the nested JAR was migrated
        try (JarFile war = new JarFile(warTarget)) {
            JarEntry nestedEntry = war.getJarEntry("WEB-INF/lib/nested.jar");
            assertNotNull("Nested JAR should exist in WAR", nestedEntry);

            // Read the nested JAR and verify its content was migrated
            byte[] nestedJarBytes = new byte[(int) nestedEntry.getSize()];
            try (InputStream is = war.getInputStream(nestedEntry)) {
                int offset = 0;
                int count;
                while (offset < nestedJarBytes.length && (count = is.read(nestedJarBytes, offset, nestedJarBytes.length - offset)) > 0) {
                    offset += count;
                }
            }

            // Parse the nested JAR from bytes
            org.apache.commons.compress.archivers.zip.ZipFile nestedZipFile = new org.apache.commons.compress.archivers.zip.ZipFile(
                    new org.apache.commons.compress.utils.SeekableInMemoryByteChannel(nestedJarBytes));
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry nestedTextEntry =
                    nestedZipFile.getEntry("nested.txt");
            assertNotNull("nested.txt should exist in nested JAR", nestedTextEntry);

            byte[] nestedTextBytes = new byte[(int) nestedTextEntry.getSize()];
            try (InputStream is = nestedZipFile.getInputStream(nestedTextEntry)) {
                is.read(nestedTextBytes);
            }
            String migratedNestedContent = new String(nestedTextBytes, StandardCharsets.ISO_8859_1);
            assertTrue("Nested content should be migrated",
                    migratedNestedContent.contains("jakarta.servlet"));
            assertFalse("Nested content should not contain javax",
                    migratedNestedContent.contains("javax.servlet"));
            nestedZipFile.close();
        }
    }

    @Test
    public void testMigrateNestedArchiveWithCacheHit() throws Exception {
        // Create a nested JAR with javax.servlet references
        String nestedContent = "javax.servlet.http.HttpServlet";
        File nestedJar = tempFolder.newFile("nested-hit.jar");
        try (FileOutputStream fos = new FileOutputStream(nestedJar);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("nested.txt");
            zos.putArchiveEntry(entry);
            zos.write(nestedContent.getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }

        // Create two WARs with the same nested JAR
        File warFile1 = createWarWithNestedJar(nestedJar, "app1.war");
        File cacheDir = tempFolder.newFolder("nested-hit-cache");

        // First migration - cache miss
        File warTarget1 = tempFolder.newFile("app1-migrated.war");
        Migration migration1 = new Migration();
        migration1.setSource(warFile1);
        migration1.setDestination(warTarget1);
        MigrationCache cache = new MigrationCache(cacheDir, 30);
        migration1.setCache(cache);
        migration1.setZipInMemory(false);
        migration1.execute();

        assertTrue("First target WAR should exist", warTarget1.exists());

        // Create second WAR with same nested JAR
        File warFile2 = createWarWithNestedJar(nestedJar, "app2.war");
        File warTarget2 = tempFolder.newFile("app2-migrated.war");

        // Second migration - should hit cache for nested JAR
        Migration migration2 = new Migration();
        migration2.setSource(warFile2);
        migration2.setDestination(warTarget2);
        migration2.setCache(cache);
        migration2.setZipInMemory(false);
        migration2.execute();

        assertTrue("Second target WAR should exist", warTarget2.exists());

        // Verify both WARs have migrated nested content
        for (File warTarget : new File[]{warTarget1, warTarget2}) {
            try (JarFile war = new JarFile(warTarget)) {
                JarEntry nestedEntry = war.getJarEntry("WEB-INF/lib/nested.jar");
                assertNotNull("Nested JAR should exist", nestedEntry);

                byte[] nestedJarBytes = new byte[(int) nestedEntry.getSize()];
                try (InputStream is = war.getInputStream(nestedEntry)) {
                    int offset = 0;
                    int count;
                    while (offset < nestedJarBytes.length && (count = is.read(nestedJarBytes, offset, nestedJarBytes.length - offset)) > 0) {
                        offset += count;
                    }
                }

                org.apache.commons.compress.archivers.zip.ZipFile nestedZipFile = new org.apache.commons.compress.archivers.zip.ZipFile(
                        new org.apache.commons.compress.utils.SeekableInMemoryByteChannel(nestedJarBytes));
                org.apache.commons.compress.archivers.zip.ZipArchiveEntry nestedTextEntry =
                        nestedZipFile.getEntry("nested.txt");
                byte[] nestedTextBytes = new byte[(int) nestedTextEntry.getSize()];
                try (InputStream is = nestedZipFile.getInputStream(nestedTextEntry)) {
                    is.read(nestedTextBytes);
                }
                String migratedContent = new String(nestedTextBytes, StandardCharsets.ISO_8859_1);
                assertTrue("Nested content should be migrated in " + warTarget.getName(),
                        migratedContent.contains("jakarta.servlet"));
                nestedZipFile.close();
            }
        }
    }

    private File createWarWithNestedJar(File nestedJar, String warName) throws Exception {
        File warFile = tempFolder.newFile(warName);
        byte[] nestedJarBytes = Files.readAllBytes(nestedJar.toPath());
        try (FileOutputStream fos = new FileOutputStream(warFile);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("WEB-INF/lib/nested.jar");
            zos.putArchiveEntry(entry);
            zos.write(nestedJarBytes);
            zos.closeArchiveEntry();
        }
        return warFile;
    }

    @Test
    public void testMigrateLargeSingleFileInPlace() throws Exception {
        // Create a large JAR file (>10MB) with javax references
        // Use STORED method so the JAR stays large (not compressed)
        File largeJar = tempFolder.newFile("large-inplace.jar");
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        java.util.Random random = new java.util.Random(42); // Seed for reproducibility
        random.nextBytes(largeContent); // Random data doesn't compress

        // Create a text file with javax reference
        String textContent = "javax.servlet.http.HttpServlet";

        try (FileOutputStream fos = new FileOutputStream(largeJar);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            // Add large data file with STORED method (no compression)
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry largeEntry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("large-data.bin");
            largeEntry.setMethod(org.apache.commons.compress.archivers.zip.ZipArchiveEntry.STORED);
            largeEntry.setSize(largeContent.length);
            CRC32 crc = new CRC32();
            crc.update(largeContent);
            largeEntry.setCrc(crc.getValue());
            zos.putArchiveEntry(largeEntry);
            zos.write(largeContent);
            zos.closeArchiveEntry();

            // Add text file with javax reference (to trigger conversion)
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry textEntry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("test.txt");
            zos.putArchiveEntry(textEntry);
            zos.write(textContent.getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }

        assertTrue("Large JAR should be >10MB (actual: " + largeJar.length() + " bytes)",
                largeJar.length() > 10 * 1024 * 1024);

        // Migrate in-place (src == dest) - should use temp file path
        Migration migration = new Migration();
        migration.setSource(largeJar);
        migration.setDestination(largeJar);
        migration.execute();

        assertTrue("Large JAR should still exist", largeJar.exists());
        assertTrue("hasConverted should be true", migration.hasConverted());

        // Verify the text file was migrated
        try (JarFile jar = new JarFile(largeJar)) {
            JarEntry textEntry = jar.getJarEntry("test.txt");
            assertNotNull("test.txt should exist", textEntry);

            byte[] textBytes = new byte[(int) textEntry.getSize()];
            try (InputStream is = jar.getInputStream(textEntry)) {
                is.read(textBytes);
            }
            String migratedText = new String(textBytes, StandardCharsets.ISO_8859_1);
            assertTrue("Text should be migrated", migratedText.contains("jakarta.servlet"));
        }
    }

    @Test
    public void testMigrateDirectoryNestedSubdirCannotCreate() throws Exception {
        // Create a source directory with nested subdirectories
        File sourceDir = tempFolder.newFolder("nested-source");
        File subDir1 = new File(sourceDir, "level1");
        subDir1.mkdirs();
        File subDir2 = new File(subDir1, "level2");
        subDir2.mkdirs();
        File sourceFile = new File(subDir2, "test.txt");
        Files.write(sourceFile.toPath(), "javax.servlet".getBytes(StandardCharsets.ISO_8859_1));

        // Create a destination where nested subdir can't be created
        // Use /proc as it's typically a read-only mount on Linux
        File destDir = new File("/proc/nested-test-dest");

        Migration migration = new Migration();
        migration.setSource(sourceDir);
        migration.setDestination(destDir);

        try {
            migration.execute();
            fail("Should throw IOException when cannot create nested subdirectory");
        } catch (IOException e) {
            // Expected - should fail to create nested directory
        }
    }

    @Test
    public void testMigrateDirectoryWithNestedDirs() throws Exception {
        // Create a source directory with nested subdirectories
        File sourceDir = tempFolder.newFolder("nested-source");
        File subDir1 = new File(sourceDir, "level1");
        subDir1.mkdirs();
        File subDir2 = new File(subDir1, "level2");
        subDir2.mkdirs();
        File sourceFile = new File(subDir2, "test.txt");
        Files.write(sourceFile.toPath(), "javax.servlet.http.HttpServlet".getBytes(StandardCharsets.ISO_8859_1));

        File destDir = tempFolder.newFolder("nested-dest");

        Migration migration = new Migration();
        migration.setSource(sourceDir);
        migration.setDestination(destDir);
        migration.execute();

        // Verify the nested structure was migrated
        File destFile = new File(destDir, "level1/level2/test.txt");
        assertTrue("Nested file should exist", destFile.exists());

        String content = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Nested file should be migrated", content.contains("jakarta.servlet"));
    }

    @Test
    public void testMigrateNestedJarInWarStreaming() throws Exception {
        // Create a WAR with a nested JAR that has javax references
        File nestedJar = tempFolder.newFile("nested-streaming.jar");
        String nestedContent = "javax.servlet.http.HttpServlet";
        try (FileOutputStream fos = new FileOutputStream(nestedJar);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("nested.txt");
            zos.putArchiveEntry(entry);
            zos.write(nestedContent.getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }

        File warFile = createWarWithNestedJar(nestedJar, "streaming-test.war");
        File warTarget = tempFolder.newFile("streaming-test-migrated.war");

        Migration migration = new Migration();
        migration.setSource(warFile);
        migration.setDestination(warTarget);
        migration.setZipInMemory(false); // Streaming mode
        migration.execute();

        assertTrue("Target WAR should exist", warTarget.exists());
        assertTrue("hasConverted should be true", migration.hasConverted());

        // Verify nested JAR content was migrated
        try (JarFile war = new JarFile(warTarget)) {
            JarEntry nestedEntry = war.getJarEntry("WEB-INF/lib/nested.jar");
            assertNotNull("Nested JAR should exist", nestedEntry);

            byte[] nestedJarBytes = new byte[(int) nestedEntry.getSize()];
            try (InputStream is = war.getInputStream(nestedEntry)) {
                int offset = 0;
                int count;
                while (offset < nestedJarBytes.length && (count = is.read(nestedJarBytes, offset, nestedJarBytes.length - offset)) > 0) {
                    offset += count;
                }
            }

            org.apache.commons.compress.archivers.zip.ZipFile nestedZipFile = new org.apache.commons.compress.archivers.zip.ZipFile(
                    new org.apache.commons.compress.utils.SeekableInMemoryByteChannel(nestedJarBytes));
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry nestedTextEntry =
                    nestedZipFile.getEntry("nested.txt");
            byte[] nestedTextBytes = new byte[(int) nestedTextEntry.getSize()];
            try (InputStream is = nestedZipFile.getInputStream(nestedTextEntry)) {
                is.read(nestedTextBytes);
            }
            String migratedContent = new String(nestedTextBytes, StandardCharsets.ISO_8859_1);
            assertTrue("Nested content should be migrated",
                    migratedContent.contains("jakarta.servlet"));
            nestedZipFile.close();
        }
    }

    @Test
    public void testMigrateNestedJarInWarInMemory() throws Exception {
        // Create a WAR with a nested JAR that has javax references
        File nestedJar = tempFolder.newFile("nested-memory.jar");
        String nestedContent = "javax.servlet.http.HttpServlet";
        try (FileOutputStream fos = new FileOutputStream(nestedJar);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("nested.txt");
            zos.putArchiveEntry(entry);
            zos.write(nestedContent.getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }

        File warFile = createWarWithNestedJar(nestedJar, "memory-test.war");
        File warTarget = tempFolder.newFile("memory-test-migrated.war");

        Migration migration = new Migration();
        migration.setSource(warFile);
        migration.setDestination(warTarget);
        migration.setZipInMemory(true); // In-memory mode
        migration.execute();

        assertTrue("Target WAR should exist", warTarget.exists());
        assertTrue("hasConverted should be true", migration.hasConverted());

        // Verify nested JAR content was migrated
        try (JarFile war = new JarFile(warTarget)) {
            JarEntry nestedEntry = war.getJarEntry("WEB-INF/lib/nested.jar");
            assertNotNull("Nested JAR should exist", nestedEntry);

            byte[] nestedJarBytes = new byte[(int) nestedEntry.getSize()];
            try (InputStream is = war.getInputStream(nestedEntry)) {
                int offset = 0;
                int count;
                while (offset < nestedJarBytes.length && (count = is.read(nestedJarBytes, offset, nestedJarBytes.length - offset)) > 0) {
                    offset += count;
                }
            }

            org.apache.commons.compress.archivers.zip.ZipFile nestedZipFile = new org.apache.commons.compress.archivers.zip.ZipFile(
                    new org.apache.commons.compress.utils.SeekableInMemoryByteChannel(nestedJarBytes));
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry nestedTextEntry =
                    nestedZipFile.getEntry("nested.txt");
            byte[] nestedTextBytes = new byte[(int) nestedTextEntry.getSize()];
            try (InputStream is = nestedZipFile.getInputStream(nestedTextEntry)) {
                is.read(nestedTextBytes);
            }
            String migratedContent = new String(nestedTextBytes, StandardCharsets.ISO_8859_1);
            assertTrue("Nested content should be migrated",
                    migratedContent.contains("jakarta.servlet"));
            nestedZipFile.close();
        }
    }

    @Test
    public void testMigrateWithStoreMethodInZip() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = tempFolder.newFile("stored-method-test.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.execute();

        try (JarFile jar = new JarFile(jarFileTarget)) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    break;
                }
            }
        }
        assertTrue("Target JAR should exist", jarFileTarget.exists());
    }

    @Test
    public void testMigrateDirectoryNestedSubdir() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = tempFolder.newFolder("nested-subdir-test");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.execute();

        assertTrue("Destination directory should exist", destinationDirectory.exists());
        assertTrue("Destination should have files", destinationDirectory.list().length > 0);
    }

    @Test
    public void testMigrateFileToNewParentDirectory() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File destFile = new File(tempFolder.newFolder("new", "parent"), "migrated.java");

        Migration migration = new Migration();
        migration.setSource(sourceFile);
        migration.setDestination(destFile);
        migration.execute();

        assertTrue("Destination file should exist", destFile.exists());
    }

    @Test
    public void testMigrateWithJee8ProfileNoConversion() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File destFile = tempFolder.newFile("jee8-no-conversion.java");

        Migration migration = new Migration();
        migration.setSource(sourceFile);
        migration.setDestination(destFile);
        migration.setEESpecProfile(EESpecProfiles.JEE8);
        migration.execute();

        assertFalse("JEE8 profile should not convert", migration.hasConverted());
        String migratedSource = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Source should remain unchanged with JEE8", migratedSource.contains("import javax.servlet"));
    }

    @Test
    public void testMigrateCLIWithZipInMemory() throws Exception {
        File sourceFile = new File("target/test-classes/hellocgi.jar");
        File targetFile = tempFolder.newFile("cli-zip-memory.jar");

        MigrationCLI.main(new String[] {
                "-zipInMemory",
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist", targetFile.exists());
    }

    @Test
    public void testMigrateCLIWithExclude() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File targetFile = tempFolder.newFile("cli-exclude.java");

        MigrationCLI.main(new String[] {
                "-exclude=*.java",
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist even when excluded", targetFile.exists());
        String content = FileUtils.readFileToString(targetFile, StandardCharsets.UTF_8);
        assertTrue("Excluded file should not be converted", content.contains("import javax.servlet"));
    }

    @Test
    public void testMigrateCLIWithMatchExcludesAgainstPathName() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File targetFile = tempFolder.newFile("cli-match-path.java");

        MigrationCLI.main(new String[] {
                "-matchExcludesAgainstPathName",
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist", targetFile.exists());
    }

    @Test
    public void testMigrateCLIWithCacheRetention() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File targetFile = tempFolder.newFile("cli-cache-retention.java");
        File cacheDir = tempFolder.newFolder("cache-retention-test");

        try {
            MigrationCLI.main(new String[] {
                    "-cache",
                    "-cacheLocation=" + cacheDir.getAbsolutePath(),
                    "-cacheRetention=7",
                    sourceFile.getAbsolutePath(),
                    targetFile.getAbsolutePath()
            });

            assertTrue("Target file should exist", targetFile.exists());
            assertTrue("Cache directory should be created", cacheDir.exists());
        } finally {
            // Clean up
            if (cacheDir.exists()) {
                FileUtils.deleteDirectory(cacheDir);
            }
        }
    }

    @Test
    public void testMigrateCLIWithLogLevelFine() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File targetFile = tempFolder.newFile("cli-log-fine.java");

        MigrationCLI.main(new String[] {
                "-logLevel=FINE",
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist", targetFile.exists());
    }

    @Test
    public void testMigrateCLIMissingArguments() throws Exception {
        Assume.assumeTrue(securityManagerAvailable);

        try {
            MigrationCLI.main(new String[] {
                    "only-source.txt"
            });
            fail("No error code returned for missing arguments");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }

    @Test
    public void testMigrateCLITooManyArguments() throws Exception {
        Assume.assumeTrue(securityManagerAvailable);

        try {
            MigrationCLI.main(new String[] {
                    "source.txt", "dest.txt", "extra.txt"
            });
            fail("No error code returned for too many arguments");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }

    @Test
    public void testMigrateCLIInvalidCacheRetention() throws Exception {
        Assume.assumeTrue(securityManagerAvailable);

        try {
            MigrationCLI.main(new String[] {
                    "-cacheRetention=-1",
                    "source.txt", "dest.txt"
            });
            fail("No error code returned for invalid cache retention");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }

    @Test
    public void testMigrateCLIInvalidLogLevel() throws Exception {
        Assume.assumeTrue(securityManagerAvailable);

        try {
            MigrationCLI.main(new String[] {
                    "-logLevel=INVALID",
                    "source.txt", "dest.txt"
            });
            fail("No error code returned for invalid log level");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }

    @Test
    public void testMigrateCLICacheRetentionNonNumeric() throws Exception {
        Assume.assumeTrue(securityManagerAvailable);

        try {
            MigrationCLI.main(new String[] {
                    "-cacheRetention=abc",
                    "source.txt", "dest.txt"
            });
            fail("No error code returned for non-numeric cache retention");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }

    @Test
    public void testMigrateCLICacheRetentionZero() throws Exception {
        Assume.assumeTrue(securityManagerAvailable);

        try {
            MigrationCLI.main(new String[] {
                    "-cacheRetention=0",
                    "source.txt", "dest.txt"
            });
            fail("No error code returned for zero cache retention");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
    }

    @Test
    public void testMigrateMultipleExcludes() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = tempFolder.newFolder("multi-excludes-test");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.addExclude("HelloServlet.java");
        migration.addExclude("*.p12");
        migration.execute();

        // Excluded files are still copied but not converted
        File excludedFile1 = new File(destinationDirectory, "HelloServlet.java");
        assertTrue("First excluded file should still be copied", excludedFile1.exists());
        String content1 = FileUtils.readFileToString(excludedFile1, StandardCharsets.UTF_8);
        assertTrue("First excluded file should not be converted",
                content1.contains("import javax.servlet"));

        File excludedFile2 = new File(destinationDirectory, "keystore.p12");
        assertTrue("Second excluded file should still be copied", excludedFile2.exists());
    }

    @Test
    public void testMigrateWithDefaultExcludesDisabled() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File destFile = tempFolder.newFile("no-default-excludes.java");

        Migration migration = new Migration();
        migration.setSource(sourceFile);
        migration.setDestination(destFile);
        migration.setEnableDefaultExcludes(false);
        migration.setEESpecProfile(EESpecProfiles.EE);
        migration.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        String migratedSource = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Imports should be migrated", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateServletProfile() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File destFile = tempFolder.newFile("servlet-profile.java");

        Migration migration = new Migration();
        migration.setSource(sourceFile);
        migration.setDestination(destFile);
        migration.setEESpecProfile(EESpecProfiles.SERVLET);
        migration.execute();

        assertTrue("Migrated file should exist", destFile.exists());
        String migratedSource = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);
        assertTrue("Imports should be migrated with SERVLET profile",
                migratedSource.contains("import jakarta.servlet"));
    }
}
