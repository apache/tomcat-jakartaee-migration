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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MigrationCacheTest {

    private File tempCacheDir;

    @Before
    public void setUp() throws Exception {
        // Create a temporary cache directory for each test
        tempCacheDir = Files.createTempDirectory("migration-cache-test").toFile();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up the temporary cache directory
        if (tempCacheDir != null && tempCacheDir.exists()) {
            FileUtils.deleteDirectory(tempCacheDir);
        }
    }

    @Test
    public void testCacheEnabledWithValidDirectory() throws Exception {
        new MigrationCache(tempCacheDir, 30);
        assertTrue("Cache directory should exist", tempCacheDir.exists());
    }

    @Test
    public void testCacheCreatesDirectory() throws Exception {
        File newCacheDir = new File(tempCacheDir, "new-cache");
        assertFalse("Cache directory should not exist yet", newCacheDir.exists());

        new MigrationCache(newCacheDir, 30);
        assertTrue("Cache directory should be created", newCacheDir.exists());
    }

    @Test
    public void testCacheMiss() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        byte[] sourceData = "test source content".getBytes(StandardCharsets.UTF_8);

        // Get cache entry - should not exist
        CacheEntry entry = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        assertFalse("Cache entry should not exist", entry.exists());
        assertNotNull("Hash should be computed", entry.getHash());
    }

    @Test
    public void testCacheHit() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        byte[] sourceData = "test source content".getBytes(StandardCharsets.UTF_8);
        byte[] convertedData = "converted content".getBytes(StandardCharsets.UTF_8);

        // Store in cache
        CacheEntry entry1 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        assertFalse("Entry should not exist initially", entry1.exists());

        try (OutputStream os = entry1.beginStore()) {
            os.write(convertedData);
        }
        entry1.commitStore();

        // Now check for cache hit
        CacheEntry entry2 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        assertTrue("Entry should exist now", entry2.exists());

        ByteArrayOutputStream destOutput = new ByteArrayOutputStream();
        entry2.copyToDestination(destOutput);
        assertArrayEquals("Cached content should match",
                convertedData, destOutput.toByteArray());
    }

    @Test
    public void testCacheStoresAndRetrieves() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        byte[] sourceData = "original jar content".getBytes(StandardCharsets.UTF_8);
        byte[] convertedData = "migrated jar content".getBytes(StandardCharsets.UTF_8);

        // Store the conversion result
        CacheEntry entry1 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        try (OutputStream os = entry1.beginStore()) {
            os.write(convertedData);
        }
        entry1.commitStore();

        // Verify it was stored by trying to retrieve it
        CacheEntry entry2 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        assertTrue("Should be cached", entry2.exists());

        ByteArrayOutputStream destOutput = new ByteArrayOutputStream();
        entry2.copyToDestination(destOutput);
        assertArrayEquals("Retrieved content should match stored content",
                convertedData, destOutput.toByteArray());
    }

    @Test
    public void testCacheDifferentContent() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        byte[] sourceData1 = "content 1".getBytes(StandardCharsets.UTF_8);
        byte[] convertedData1 = "converted 1".getBytes(StandardCharsets.UTF_8);
        byte[] sourceData2 = "content 2".getBytes(StandardCharsets.UTF_8);

        // Store first conversion
        CacheEntry entry1 = cache.getCacheEntry(sourceData1, EESpecProfiles.TOMCAT);
        try (OutputStream os = entry1.beginStore()) {
            os.write(convertedData1);
        }
        entry1.commitStore();

        // Check with different source content
        CacheEntry entry2 = cache.getCacheEntry(sourceData2, EESpecProfiles.TOMCAT);
        assertFalse("Should be cache miss for different content", entry2.exists());
    }

    @Test
    public void testCacheClear() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        byte[] sourceData = "test content".getBytes(StandardCharsets.UTF_8);
        byte[] convertedData = "converted content".getBytes(StandardCharsets.UTF_8);

        // Store in cache
        CacheEntry entry1 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        try (OutputStream os = entry1.beginStore()) {
            os.write(convertedData);
        }
        entry1.commitStore();

        // Verify it's cached
        CacheEntry entry2 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        assertTrue("Should be cache hit before clear", entry2.exists());

        // Clear the cache
        cache.clear();

        // Verify it's no longer cached
        CacheEntry entry3 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        assertFalse("Should be cache miss after clear", entry3.exists());
    }

    @Test
    public void testCacheStats() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        String stats = cache.getStats();
        assertNotNull("Stats should not be null", stats);
        assertTrue("Stats should contain entry count", stats.contains("0"));
    }

    @Test
    public void testCacheStatsDisabled() throws Exception {
        MigrationCache cache = new MigrationCache(null, 30);

        String stats = cache.getStats();
        assertNotNull("Stats should not be null", stats);
        assertTrue("Stats should indicate cache is disabled", stats.toLowerCase().contains("disabled"));
    }

    @Test
    public void testCacheWithLargeContent() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        // Create large content (1MB)
        byte[] sourceData = new byte[1024 * 1024];
        for (int i = 0; i < sourceData.length; i++) {
            sourceData[i] = (byte) (i % 256);
        }
        byte[] convertedData = new byte[1024 * 1024];
        for (int i = 0; i < convertedData.length; i++) {
            convertedData[i] = (byte) ((i + 100) % 256);
        }

        // Store and retrieve
        CacheEntry entry1 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        try (OutputStream os = entry1.beginStore()) {
            os.write(convertedData);
        }
        entry1.commitStore();

        CacheEntry entry2 = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
        assertTrue("Should be cache hit for large content", entry2.exists());

        ByteArrayOutputStream destOutput = new ByteArrayOutputStream();
        entry2.copyToDestination(destOutput);
        assertArrayEquals("Large content should be retrieved correctly",
                convertedData, destOutput.toByteArray());
    }

    @Test
    public void testCacheWithMultipleEntries() throws Exception {
        MigrationCache cache = new MigrationCache(tempCacheDir, 30);

        // Store multiple different entries
        for (int i = 0; i < 5; i++) {
            byte[] sourceData = ("source " + i).getBytes(StandardCharsets.UTF_8);
            byte[] convertedData = ("converted " + i).getBytes(StandardCharsets.UTF_8);

            CacheEntry entry = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
            try (OutputStream os = entry.beginStore()) {
                os.write(convertedData);
            }
            entry.commitStore();
        }

        // Verify all can be retrieved
        for (int i = 0; i < 5; i++) {
            byte[] sourceData = ("source " + i).getBytes(StandardCharsets.UTF_8);
            byte[] expectedConverted = ("converted " + i).getBytes(StandardCharsets.UTF_8);

            CacheEntry entry = cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
            assertTrue("Should be cache hit for entry " + i, entry.exists());

            ByteArrayOutputStream destOutput = new ByteArrayOutputStream();
            entry.copyToDestination(destOutput);
            assertArrayEquals("Content should match for entry " + i,
                    expectedConverted, destOutput.toByteArray());
        }
    }

    @Test
    public void testCacheDisabledNoOperations() throws Exception {
        MigrationCache cache = new MigrationCache(null, 30);

        byte[] sourceData = "test content".getBytes(StandardCharsets.UTF_8);

        // getCacheEntry should throw exception when cache is disabled
        try {
            cache.getCacheEntry(sourceData, EESpecProfiles.TOMCAT);
            fail("Should throw exception when cache is disabled");
        } catch (IllegalStateException e) {
            // Expected
            assertTrue("Error message should mention cache not enabled",
                    e.getMessage().contains("not enabled"));
        }
    }
}
