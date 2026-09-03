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
import java.nio.file.StandardCopyOption;
import java.util.zip.CRC32;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.compress.archivers.zip.ZipFile;
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
        File migratedFile = tempFolder.newFile("HelloServlet.migrated.java");
        MigrationCLI.main(new String[] {"target/test-classes/HelloServlet.java", migratedFile.getAbsolutePath()});

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateSingleSourceFileWithProfile() throws Exception {
        File migratedFile = tempFolder.newFile("HelloServlet.migrated.java");
        MigrationCLI.main(new String[] {"-logLevel=FINE", "-profile=EE", "target/test-classes/HelloServlet.java", migratedFile.getAbsolutePath()});

        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));
    }

    @Test
    public void testMigrateSingleSourceFileInPlace() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File migratedFile = tempFolder.newFile("HelloServlet.inplace.java");
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
        assertCliError("-invalid", sourceFile.getAbsolutePath(), migratedFile.getAbsolutePath());
    }

    @Test
    public void testInvalidProfile() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File migratedFile = new File("target/test-classes/HelloServlet.migrated.java");
        assertCliError("-profile=JSERV", sourceFile.getAbsolutePath(), migratedFile.getAbsolutePath());
    }

    @Test
    public void testMigrateDirectory() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = new File(tempFolder.getRoot(), "migration");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.execute();

        assertTrue("Destination directory not found", destinationDirectory.exists());

        File migratedFile = new File(destinationDirectory, "HelloServlet.java");
        assertTrue("Migrated file not found", migratedFile.exists());

        String migratedSource = FileUtils.readFileToString(migratedFile, StandardCharsets.UTF_8);
        assertFalse("Imports not migrated", migratedSource.contains("import javax.servlet"));
        assertTrue("Migrated imports not found", migratedSource.contains("import jakarta.servlet"));

        File migratedSpiFile = new File(destinationDirectory, "javax.enterprise.inject.spi.Extension");
        assertTrue("SPI file has not been migrated by renaming", migratedSpiFile.exists());

        String migratedSpiSource = FileUtils.readFileToString(migratedSpiFile, StandardCharsets.UTF_8);
        assertTrue("SPI file not copied with content", migratedSpiSource.contains("some.class.Reference"));
    }

    @Test
    public void testMigrateDirectoryWithEeProfile() throws Exception {
        File sourceDirectory = new File("src/test/resources");
        File destinationDirectory = new File(tempFolder.getRoot(), "migration-ee");

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
        File classFileOriginal = new File(tempFolder.getRoot(), "HelloCGI-original.class");
        FileUtils.copyFile(classFile, classFileOriginal);

        try {
            Migration migration = new Migration();
            migration.setSource(classFile);
            migration.setDestination(classFile);
            migration.execute();

            Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI");
            assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());
        } finally {
            // Always restore the original class file, otherwise a failure in
            // this test would leave the (shared) class file migrated and
            // break other tests on the next non-clean build.
            Assert.assertTrue("Failed to delete migrated class file", classFile.delete());
            FileUtils.copyFile(classFileOriginal, classFile);
        }
    }

    @Test
    public void testMigrateJarFile() throws Exception {
        testMigrateJarFileInternal(false);
    }

    private void testMigrateJarFileInternal(boolean zipInMemory) throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = tempFolder.newFile("hellocgi-target.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(zipInMemory);
        migration.execute();

        verifyHelloCGIMigrated(jarFileTarget);

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
        File jarFileTmp = tempFolder.newFile("hellocgi-signed-" + algorithm + "-tmp.jar");
        Files.copy(jarFileSrc.toPath(), jarFileTmp.toPath(), StandardCopyOption.REPLACE_EXISTING);

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
        File jarFileTarget = tempFolder.newFile("hellocgi-cached.jar");
        // Not pre-created: the MigrationCache constructor must create it
        File cacheDir = new File(tempFolder.getRoot(), "cache-test");

        // Note: top-level archives are not cached (only nested archives are),
        // so the cache is only set up, not exercised, by this test.
        Migration migration1 = new Migration();
        migration1.setSource(jarFile);
        migration1.setDestination(jarFileTarget);
        migration1.setCache(new MigrationCache(cacheDir, 30));
        migration1.execute();

        assertTrue("Target JAR should exist after first migration", jarFileTarget.exists());
        assertTrue("Cache directory should be created", cacheDir.exists());

        // Verify the migrated JAR works
        verifyHelloCGIMigrated(jarFileTarget);

        // Delete target and migrate again
        jarFileTarget.delete();
        assertFalse("Target should be deleted", jarFileTarget.exists());

        Migration migration2 = new Migration();
        migration2.setSource(jarFile);
        migration2.setDestination(jarFileTarget);
        migration2.setCache(new MigrationCache(cacheDir, 30));
        migration2.execute();

        assertTrue("Target JAR should exist after second migration", jarFileTarget.exists());

        // Verify the migrated JAR works
        verifyHelloCGIMigrated(jarFileTarget);
    }

    @Test
    public void testMigrateJarWithCacheDisabled() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = tempFolder.newFile("hellocgi-nocache.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        // Don't set cache - should work without caching
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());

        verifyHelloCGIMigrated(jarFileTarget);
    }

    @Test
    public void testMigrateCLIWithCacheOption() throws Exception {
        File sourceFile = new File("target/test-classes/hellocgi.jar");
        File targetFile = tempFolder.newFile("hellocgi-cli-cached.jar");
        // Not pre-created: the MigrationCache constructor must create it
        File cacheDir = new File(tempFolder.getRoot(), "cache-cli-test");

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
        verifyHelloCGIMigrated(targetFile);
    }

    @Test
    public void testMigrateCLIWithNoCacheOption() throws Exception {
        File sourceFile = new File("target/test-classes/hellocgi.jar");
        File targetFile = tempFolder.newFile("hellocgi-cli-nocache.jar");

        // Run without cache (no -cache option)
        MigrationCLI.main(new String[] {
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist", targetFile.exists());

        // Verify the migrated JAR works
        verifyHelloCGIMigrated(targetFile);
    }

    @Test
    public void testReExecuteAfterCompletion() throws Exception {
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
        // Use an existing regular file as the destination so that the
        // destination directory cannot be created (portable across platforms)
        File destDirectory = tempFolder.newFile("immutable-dest");

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
        // Create a JAR whose entry carries a ZIP64 extra field (ID 0x0001)
        File jarFile = tempFolder.newFile("zip64-test.jar");
        try (FileOutputStream fos = new FileOutputStream(jarFile);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry("test.txt");
            // ZIP64 extended information extra field: header 0x0001, 16 bytes of data
            entry.setExtra(new byte[] { 0x01, 0x00, 0x10, 0x00,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
            zos.putArchiveEntry(entry);
            zos.write("javax.servlet.http.HttpServlet".getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }

        File jarFileTarget = tempFolder.newFile("zip64-migrated.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(false); // Streaming mode removes the ZIP64 extra field
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());

        try (org.apache.commons.compress.archivers.zip.ZipFile jar =
                org.apache.commons.compress.archivers.zip.ZipFile.builder().setFile(jarFileTarget).get()) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    (org.apache.commons.compress.archivers.zip.ZipArchiveEntry) jar.getEntry("test.txt");
            assertNotNull("Entry should exist in migrated JAR", entry);
            assertNull("ZIP64 extra field should have been removed",
                    entry.getExtraField(new org.apache.commons.compress.archivers.zip.ZipShort(1)));

            byte[] content = readAllBytes(jar.getInputStream(entry), (int) entry.getSize());
            assertTrue("Entry content should be migrated",
                    new String(content, StandardCharsets.ISO_8859_1).contains("jakarta.servlet"));
        }
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
        // A valid archive whose file name matches one of the default exclude
        // patterns (commons-lang-*.jar). With the default excludes disabled
        // it must be processed (and converted) as a normal archive.
        File sourceDirectory = tempFolder.newFolder("no-default-excludes-test");
        createNestedJarWithContent(sourceDirectory, "commons-lang-3.12.0.jar", "nested.txt",
                "javax.servlet.http.HttpServlet");
        File destinationDirectory = tempFolder.newFolder("no-default-excludes-dest");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.setEnableDefaultExcludes(false);
        migration.execute();

        assertTrue("Archive should have been converted", migration.hasConverted());
        verifyArchiveEntryContent(new File(destinationDirectory, "commons-lang-3.12.0.jar"),
                "nested.txt", "jakarta.servlet");
    }

    @Test
    public void testMigrateJarFileInMemoryBasic() throws Exception {
        File jarFile = new File("target/test-classes/hellocgi.jar");
        File jarFileTarget = tempFolder.newFile("nested-test.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(true);
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());

        verifyHelloCGIMigrated(jarFileTarget);
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

            byte[] readContent = readAllBytes(jar.getInputStream(entry), (int) entry.getSize());
            assertArrayEquals("Large entry content should match", largeContent, readContent);
        }
    }

    @Test
    public void testMigrateLargeStoredEntryInMemory() throws Exception {
        // Create a large file (>10MB) to test in-memory migration with large STORED entries
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
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

            byte[] readContent = readAllBytes(jar.getInputStream(entry), (int) entry.getSize());
            assertArrayEquals("Large entry content should match", largeContent, readContent);
        }
    }

    @Test
    public void testMigrateNestedArchiveWithCache() throws Exception {
        // Create a nested JAR with javax.servlet references
        File nestedJar = createNestedJarWithContent(tempFolder.getRoot(), "nested.jar", "nested.txt",
                "javax.servlet.http.HttpServlet");

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
        verifyNestedJarContentMigrated(warTarget, "WEB-INF/lib/nested.jar", "jakarta.servlet");
    }

    @Test
    public void testMigrateNestedArchiveWithCacheHit() throws Exception {
        // Create a nested JAR with javax.servlet references
        File nestedJar = createNestedJarWithContent(tempFolder.getRoot(), "nested-hit.jar", "nested.txt",
                "javax.servlet.http.HttpServlet");

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

        // The nested JAR should now be stored in the cache
        File cachedJar = null;
        File[] subdirs = cacheDir.listFiles();
        if (subdirs != null) {
            for (File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    File[] files = subdir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.getName().endsWith(".jar")) {
                                cachedJar = file;
                            }
                        }
                    }
                }
            }
        }
        assertNotNull("Nested JAR should be cached after first migration", cachedJar);

        // Create second WAR with same nested JAR
        File warFile2 = createWarWithNestedJar(nestedJar, "app2.war");
        File warTarget2 = tempFolder.newFile("app2-migrated.war");

        // Replace the cached content with a canary: if the second migration
        // hits the cache, the nested JAR in the second WAR must be a byte
        // for byte copy of the canary.
        byte[] canary = "cached nested jar".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(cachedJar.toPath(), canary);

        // Second migration - should hit cache for nested JAR
        Migration migration2 = new Migration();
        migration2.setSource(warFile2);
        migration2.setDestination(warTarget2);
        migration2.setCache(cache);
        migration2.setZipInMemory(false);
        migration2.execute();

        assertTrue("Second target WAR should exist", warTarget2.exists());

        // First WAR must have the migrated nested content
        verifyNestedJarContentMigrated(warTarget1, "WEB-INF/lib/nested.jar", "jakarta.servlet");

        // Second WAR's nested JAR must be served from the cache (the canary)
        try (JarFile war = new JarFile(warTarget2)) {
            JarEntry nestedEntry = war.getJarEntry("WEB-INF/lib/nested.jar");
            assertNotNull("Nested JAR should exist", nestedEntry);
            byte[] nestedJarBytes = readAllBytes(war.getInputStream(nestedEntry), (int) nestedEntry.getSize());
            assertArrayEquals("Nested JAR should be served from the cache", canary, nestedJarBytes);
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

    private File createNestedJarWithContent(File parentDir, String jarName, String entryName, String content) throws Exception {
        File nestedJar = new File(parentDir, jarName);
        try (FileOutputStream fos = new FileOutputStream(nestedJar);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry(entryName);
            zos.putArchiveEntry(entry);
            zos.write(content.getBytes(StandardCharsets.ISO_8859_1));
            zos.closeArchiveEntry();
        }
        return nestedJar;
    }

    private void verifyHelloCGIMigrated(File jarFileTarget) throws Exception {
        File cgiapiFile = new File("target/test-classes/cgi-api.jar");
        try (URLClassLoader classloader = new URLClassLoader(
                new URL[]{jarFileTarget.toURI().toURL(), cgiapiFile.toURI().toURL()},
                ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> cls = Class.forName("org.apache.tomcat.jakartaee.HelloCGI", true, classloader);
            assertEquals("jakarta.servlet.CommonGatewayInterface", cls.getSuperclass().getName());
        }
    }

    private void verifyNestedJarContentMigrated(File warFile, String nestedEntryName, String expectedContent) throws Exception {
        try (JarFile war = new JarFile(warFile)) {
            JarEntry nestedEntry = war.getJarEntry(nestedEntryName);
            assertNotNull("Nested JAR should exist", nestedEntry);

            byte[] nestedJarBytes = readAllBytes(war.getInputStream(nestedEntry), (int) nestedEntry.getSize());

            File tempNestedJar = File.createTempFile("nested", ".jar");
            tempNestedJar.deleteOnExit();
            Files.write(tempNestedJar.toPath(), nestedJarBytes);
            try (org.apache.commons.compress.archivers.zip.ZipFile nestedZipFile =
                    ZipFile.builder().setFile(tempNestedJar).get()) {
                org.apache.commons.compress.archivers.zip.ZipArchiveEntry nestedTextEntry =
                        nestedZipFile.getEntry("nested.txt");
                assertNotNull("nested.txt should exist in nested JAR", nestedTextEntry);

                byte[] nestedTextBytes = readAllBytes(nestedZipFile.getInputStream(nestedTextEntry),
                        (int) nestedTextEntry.getSize());
                String migratedContent = new String(nestedTextBytes, StandardCharsets.ISO_8859_1);
                assertTrue("Nested content should be migrated in " + warFile.getName(),
                        migratedContent.contains(expectedContent));
            }
        }
    }

    private byte[] readAllBytes(InputStream is, int expectedSize) throws IOException {
        byte[] data = new byte[expectedSize];
        int offset = 0;
        int count;
        while (offset < data.length && (count = is.read(data, offset, data.length - offset)) > 0) {
            offset += count;
        }
        return data;
    }

    private void assertCliError(String... args) throws Exception {
        Assume.assumeTrue(securityManagerAvailable);
        try {
            MigrationCLI.main(args);
            fail("No error code returned");
        } catch (SecurityException e) {
            assertEquals("error code", "1", e.getMessage());
        }
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

        // Verify the text file was migrated and the large entry was preserved
        try (JarFile jar = new JarFile(largeJar)) {
            JarEntry textEntry = jar.getJarEntry("test.txt");
            assertNotNull("test.txt should exist", textEntry);

            byte[] textBytes = readAllBytes(jar.getInputStream(textEntry), (int) textEntry.getSize());
            String migratedText = new String(textBytes, StandardCharsets.ISO_8859_1);
            assertTrue("Text should be migrated", migratedText.contains("jakarta.servlet"));

            JarEntry largeEntry = jar.getJarEntry("large-data.bin");
            assertNotNull("large-data.bin should exist", largeEntry);
            byte[] largeBytes = readAllBytes(jar.getInputStream(largeEntry), (int) largeEntry.getSize());
            assertArrayEquals("Large entry content should be preserved", largeContent, largeBytes);
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

        // Create a regular file where a nested subdirectory is expected so
        // that the subdirectory cannot be created (portable across platforms)
        File destDir = tempFolder.newFolder("nested-test-dest");
        File blockedSubDir = new File(destDir, "level1");
        Files.createFile(blockedSubDir.toPath());

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
        File nestedJar = createNestedJarWithContent(tempFolder.getRoot(), "nested-streaming.jar", "nested.txt",
                "javax.servlet.http.HttpServlet");

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
        verifyNestedJarContentMigrated(warTarget, "WEB-INF/lib/nested.jar", "jakarta.servlet");
    }

    @Test
    public void testMigrateNestedJarInWarInMemory() throws Exception {
        // Create a WAR with a nested JAR that has javax references
        File nestedJar = createNestedJarWithContent(tempFolder.getRoot(), "nested-memory.jar", "nested.txt",
                "javax.servlet.http.HttpServlet");

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
        verifyNestedJarContentMigrated(warTarget, "WEB-INF/lib/nested.jar", "jakarta.servlet");
    }

    @Test
    public void testMigrateWithStoreMethodInZip() throws Exception {
        // Create a JAR with a STORED (uncompressed) entry
        File jarFile = createStoredEntryJar("stored-method-test.jar", "test.txt",
                "javax.servlet.http.HttpServlet");
        File jarFileTarget = tempFolder.newFile("stored-method-migrated.jar");

        Migration migration = new Migration();
        migration.setSource(jarFile);
        migration.setDestination(jarFileTarget);
        migration.setZipInMemory(false); // Streaming mode handles STORED entries
        migration.execute();

        assertTrue("Target JAR should exist", jarFileTarget.exists());

        try (JarFile jar = new JarFile(jarFileTarget)) {
            JarEntry entry = jar.getJarEntry("test.txt");
            assertNotNull("Stored entry should exist in migrated JAR", entry);
            assertEquals("Stored entry should remain stored", java.util.zip.ZipEntry.STORED, entry.getMethod());

            byte[] content = readAllBytes(jar.getInputStream(entry), (int) entry.getSize());
            assertTrue("Stored entry content should be migrated",
                    new String(content, StandardCharsets.ISO_8859_1).contains("jakarta.servlet"));
        }
    }

    private File createStoredEntryJar(String jarName, String entryName, String content) throws Exception {
        File jarFile = tempFolder.newFile(jarName);
        byte[] data = content.getBytes(StandardCharsets.ISO_8859_1);
        try (FileOutputStream fos = new FileOutputStream(jarFile);
                org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                        new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(fos)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                    new org.apache.commons.compress.archivers.zip.ZipArchiveEntry(entryName);
            entry.setMethod(org.apache.commons.compress.archivers.zip.ZipArchiveEntry.STORED);
            entry.setSize(data.length);
            CRC32 crc = new CRC32();
            crc.update(data);
            entry.setCrc(crc.getValue());
            zos.putArchiveEntry(entry);
            zos.write(data);
            zos.closeArchiveEntry();
        }
        return jarFile;
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
        // Parent directory is not pre-created: execute() must create it
        File destFile = new File(tempFolder.getRoot(), "new/parent/migrated.java");

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
                "-exclude=*/HelloServlet.java",
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist", targetFile.exists());
        String content = FileUtils.readFileToString(targetFile, StandardCharsets.UTF_8);
        assertTrue("Excluded file should not be converted", content.contains("import javax.servlet"));
    }

    @Test
    public void testMigrateCLIWithCacheRetention() throws Exception {
        File sourceFile = new File("target/test-classes/HelloServlet.java");
        File targetFile = tempFolder.newFile("cli-cache-retention.java");
        File cacheDir = tempFolder.newFolder("cache-retention-test");

        MigrationCLI.main(new String[] {
                "-cache",
                "-cacheLocation=" + cacheDir.getAbsolutePath(),
                "-cacheRetention=7",
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath()
        });

        assertTrue("Target file should exist", targetFile.exists());
        assertTrue("Cache directory should be created", cacheDir.exists());
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
        assertCliError("only-source.txt");
    }

    @Test
    public void testMigrateCLITooManyArguments() throws Exception {
        assertCliError("source.txt", "dest.txt", "extra.txt");
    }

    @Test
    public void testMigrateCLIInvalidCacheRetention() throws Exception {
        assertCliError("-cacheRetention=-1", "source.txt", "dest.txt");
    }

    @Test
    public void testMigrateCLIInvalidLogLevel() throws Exception {
        assertCliError("-logLevel=INVALID", "source.txt", "dest.txt");
    }

    @Test
    public void testMigrateCLICacheRetentionNonNumeric() throws Exception {
        assertCliError("-cacheRetention=abc", "source.txt", "dest.txt");
    }

    @Test
    public void testMigrateCLICacheRetentionZero() throws Exception {
        assertCliError("-cacheRetention=0", "source.txt", "dest.txt");
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
    public void testMigrateWithDefaultExcludes() throws Exception {
        // A valid archive whose file name matches one of the default exclude
        // patterns (commons-lang-*.jar). With the default excludes enabled
        // (the default) it is copied unchanged and not converted.
        File sourceDirectory = tempFolder.newFolder("default-excludes-test");
        createNestedJarWithContent(sourceDirectory, "commons-lang-3.12.0.jar", "nested.txt",
                "javax.servlet.http.HttpServlet");
        File destinationDirectory = tempFolder.newFolder("default-excludes-dest");

        Migration migration = new Migration();
        migration.setSource(sourceDirectory);
        migration.setDestination(destinationDirectory);
        migration.execute();

        File destArchive = new File(destinationDirectory, "commons-lang-3.12.0.jar");
        assertTrue("Excluded archive should still be copied", destArchive.exists());
        assertFalse("Excluded archive should not be converted", migration.hasConverted());
        verifyArchiveEntryContent(destArchive, "nested.txt", "javax.servlet");
    }

    private void verifyArchiveEntryContent(File archiveFile, String entryName, String expectedContent) throws Exception {
        try (JarFile archive = new JarFile(archiveFile)) {
            JarEntry entry = archive.getJarEntry(entryName);
            assertNotNull("Entry should exist in " + archiveFile.getName(), entry);
            byte[] content = readAllBytes(archive.getInputStream(entry), (int) entry.getSize());
            assertTrue("Entry content in " + archiveFile.getName() + " should contain " + expectedContent,
                    new String(content, StandardCharsets.ISO_8859_1).contains(expectedContent));
        }
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
