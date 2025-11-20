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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache for storing and retrieving pre-converted archive files.
 *
 * <h2>Cache Structure</h2>
 * <p>The cache organizes files in a directory structure based on hash values:</p>
 * <pre>
 * {cacheDir}/
 *   ├── cache-metadata.txt      # Metadata file tracking access times
 *   ├── {XX}/                    # Subdirectory named by first 2 chars of hash
 *   │   └── {hash}.jar          # Cached converted archive (full SHA-256 hash)
 *   ├── {YY}/
 *   │   └── {hash}.jar
 *   └── temp-{uuid}.tmp          # Temporary files during conversion
 * </pre>
 *
 * <h2>Cache Key</h2>
 * <p>Each cache entry is keyed by a SHA-256 hash computed from:</p>
 * <ul>
 *   <li>The migration profile name (e.g., "TOMCAT", "EE")</li>
 *   <li>The pre-conversion archive content (as bytes)</li>
 * </ul>
 * <p>This ensures that the same archive converted with different profiles
 * produces different cache entries.</p>
 *
 * <h2>Metadata Format</h2>
 * <p>The {@code cache-metadata.txt} file tracks access times for cache pruning:</p>
 * <pre>
 * # Migration cache metadata - hash|last_access_date
 * {hash}|{YYYY-MM-DD}
 * {hash}|{YYYY-MM-DD}
 * </pre>
 *
 * <h2>Temporary Files</h2>
 * <p>During conversion, output is written to temporary files named {@code temp-{uuid}.tmp}.
 * These files are cleaned up on startup to handle crashes or unexpected shutdowns.</p>
 */
public class MigrationCache {

    private static final Logger logger = Logger.getLogger(MigrationCache.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(MigrationCache.class);
    private static final String METADATA_FILE = "cache-metadata.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final File cacheDir;
    private final int retentionDays;
    private final Map<String, LocalDate> cacheMetadata;
    private final File metadataFile;

    /**
     * Construct a new migration cache.
     *
     * @param cacheDir the directory to store cached files (null to disable caching)
     * @param retentionDays the number of days to retain cached files
     * @throws IOException if the cache directory cannot be created
     */
    public MigrationCache(File cacheDir, int retentionDays) throws IOException {
        this.retentionDays = retentionDays;
        this.cacheMetadata = new HashMap<>();
        this.cacheDir = cacheDir;
        this.metadataFile = cacheDir == null ? null : new File(cacheDir, METADATA_FILE);

        if (cacheDir == null) {
            throw new IllegalStateException(sm.getString("cache.nullDirectory"));
        }

        // Create cache directory if it doesn't exist
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new IOException(sm.getString("cache.cannotCreate", cacheDir.getAbsolutePath()));
            }
        }

        if (!cacheDir.isDirectory()) {
            throw new IOException(sm.getString("cache.notDirectory", cacheDir.getAbsolutePath()));
        }

        // Load existing metadata
        loadMetadata();

        // Clean up any orphaned temp files from previous crashes
        cleanupTempFiles();

        logger.log(Level.INFO,
                sm.getString("cache.enabled", cacheDir.getAbsolutePath(), Integer.valueOf(retentionDays)));
    }

    /**
     * Clean up any temporary files left over from previous crashes or unexpected shutdowns.
     * Scans the cache directory for temp-*.tmp files and deletes them.
     */
    private void cleanupTempFiles() {
        File[] files = cacheDir.listFiles();
        if (files != null) {
            int cleanedCount = 0;
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("temp-") && file.getName().endsWith(".tmp")) {
                    if (file.delete()) {
                        cleanedCount++;
                        logger.log(Level.FINE, sm.getString("cache.tempfile.cleaned", file.getName()));
                    } else {
                        logger.log(Level.WARNING, sm.getString("cache.tempfile.cleanFailed", file.getName()));
                    }
                }
            }
            if (cleanedCount > 0) {
                logger.log(Level.INFO, sm.getString("cache.tempfiles.cleaned", Integer.valueOf(cleanedCount)));
            }
        }
    }

    /**
     * Load cache metadata from disk.
     * Format: hash|YYYY-MM-DD
     * If file doesn't exist or is corrupt, assumes all existing cached jars were accessed today.
     */
    private void loadMetadata() {
        LocalDate today = LocalDate.now();

        if (!metadataFile.exists()) {
            // Metadata file doesn't exist - scan cache directory and assume all files accessed today
            logger.log(Level.FINE, sm.getString("cache.metadata.notFound"));
            scanCacheDirectory(today);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String hash = parts[0];
                    try {
                        LocalDate lastAccessed = LocalDate.parse(parts[1], DATE_FORMATTER);
                        cacheMetadata.put(hash, lastAccessed);
                    } catch (DateTimeParseException e) {
                        logger.log(Level.WARNING, sm.getString("cache.metadata.invalidDate", line));
                    }
                } else {
                    logger.log(Level.WARNING, sm.getString("cache.metadata.invalidLine", line));
                }
            }

            // Check for any cached files not in metadata and add them with today's date
            Set<String> existingHashes = scanCacheDirectory(null);
            for (String hash : existingHashes) {
                if (!cacheMetadata.containsKey(hash)) {
                    cacheMetadata.put(hash, today);
                }
            }

            logger.log(Level.FINE, sm.getString("cache.metadata.loaded", Integer.valueOf(cacheMetadata.size())));
        } catch (IOException e) {
            // Corrupt or unreadable - assume all cached files accessed today
            logger.log(Level.WARNING, sm.getString("cache.metadata.loadError"), e);
            cacheMetadata.clear();
            scanCacheDirectory(today);
        }
    }

    /**
     * Scan cache directory for existing cache files and return their hashes.
     * If accessDate is not null, adds all found hashes to metadata with that date.
     *
     * @param accessDate the date to use for all found files (null to not update metadata)
     * @return set of hashes found in cache directory
     */
    private Set<String> scanCacheDirectory(LocalDate accessDate) {
        Set<String> hashes = new HashSet<>();

        File[] subdirs = cacheDir.listFiles();
        if (subdirs != null) {
            for (File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    File[] files = subdir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.getName().endsWith(".jar")) {
                                String hash = file.getName().substring(0, file.getName().length() - 4);
                                hashes.add(hash);
                                if (accessDate != null) {
                                    cacheMetadata.put(hash, accessDate);
                                }
                            }
                        }
                    }
                }
            }
        }

        return hashes;
    }

    /**
     * Get a cache entry for the given source bytes and profile.
     * This computes the hash, checks if cached, and marks the entry as accessed.
     *
     * @param sourceBytes the pre-conversion content
     * @param profile the migration profile being used
     * @return a CacheEntry object with all operations for this entry
     * @throws IOException if an I/O error occurs
     */
    public CacheEntry getCacheEntry(byte[] sourceBytes, EESpecProfile profile) throws IOException {
        // Compute hash once (includes profile)
        String hash = computeHash(sourceBytes, profile);

        // Get cache file location
        File cachedFile = getCacheFile(hash);
        boolean exists = cachedFile.exists();

        // Create temp file for storing
        File tempFile = new File(cacheDir, "temp-" + UUID.randomUUID() + ".tmp");

        // Mark as accessed now
        updateAccessTime(hash);

        return new CacheEntry(hash, exists, cachedFile, tempFile);
    }


    /**
     * Get the cache file for a given hash.
     *
     * @param hash the hash string
     * @return the cache file
     */
    private File getCacheFile(String hash) {
        // Use subdirectories based on first 2 chars of hash to avoid too many files in one directory
        String subdir = hash.substring(0, 2);
        File subdirFile = new File(cacheDir, subdir);
        if (!subdirFile.exists()) {
            subdirFile.mkdirs();
        }
        return new File(subdirFile, hash + ".jar");
    }

    /**
     * Compute SHA-256 hash of the given bytes combined with the profile name.
     * The profile is included to ensure different profiles produce different cache entries.
     *
     * @param bytes the bytes to hash
     * @param profile the migration profile
     * @return the hash as a hex string
     * @throws IOException if hashing fails
     */
    private String computeHash(byte[] bytes, EESpecProfile profile) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Include profile name in hash to differentiate between profiles
            digest.update(profile.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update(bytes);
            byte[] hashBytes = digest.digest();

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", Byte.valueOf(b)));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(sm.getString("cache.hashError"), e);
        }
    }

    /**
     * Clear the cache directory.
     *
     * @throws IOException if an I/O error occurs
     */
    public void clear() throws IOException {
        deleteDirectory(cacheDir);
        cacheDir.mkdirs();
        logger.log(Level.INFO, sm.getString("cache.cleared"));
    }

    /**
     * Recursively delete a directory.
     *
     * @param dir the directory to delete
     * @throws IOException if an I/O error occurs
     */
    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!Files.deleteIfExists(dir.toPath()) && dir.exists()) {
            throw new IOException(sm.getString("cache.deleteFailed", dir.getAbsolutePath()));
        }
    }

    /**
     * Update the access time for a cache entry.
     *
     * @param hash the hash of the cache entry
     */
    private void updateAccessTime(String hash) {
        cacheMetadata.put(hash, LocalDate.now());
    }

    /**
     * Save cache metadata to disk.
     * Format: hash|YYYY-MM-DD
     *
     * @throws IOException if an I/O error occurs
     */
    private void saveMetadata() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile))) {
            writer.write("# Migration cache metadata - hash|last_access_date\n");
            for (Map.Entry<String, LocalDate> entry : cacheMetadata.entrySet()) {
                writer.write(entry.getKey());
                writer.write("|");
                writer.write(entry.getValue().format(DATE_FORMATTER));
                writer.write("\n");
            }
        }

        logger.log(Level.FINE, sm.getString("cache.metadata.saved", Integer.valueOf(cacheMetadata.size())));
    }

    /**
     * Prune cache entries that haven't been accessed within the retention period.
     * This should be called after migration completes.
     *
     * @throws IOException if an I/O error occurs
     */
    public void pruneCache() throws IOException {
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        int prunedCount = 0;
        long prunedSize = 0;

        Set<String> toRemove = new HashSet<>();

        for (Map.Entry<String, LocalDate> entry : cacheMetadata.entrySet()) {
            String hash = entry.getKey();
            LocalDate lastAccessed = entry.getValue();

            if (lastAccessed.isBefore(cutoffDate)) {
                File cachedFile = getCacheFile(hash);
                if (cachedFile.exists()) {
                    long fileSize = cachedFile.length();
                    if (cachedFile.delete()) {
                        prunedSize += fileSize;
                        prunedCount++;
                        toRemove.add(hash);
                        logger.log(Level.FINE, sm.getString("cache.pruned.entry", hash, lastAccessed));
                    } else {
                        logger.log(Level.WARNING, sm.getString("cache.pruned.failed", hash));
                    }
                } else {
                    // File doesn't exist, remove from metadata anyway
                    toRemove.add(hash);
                }
            }
        }

        // Remove pruned entries from metadata
        for (String hash : toRemove) {
            cacheMetadata.remove(hash);
        }

        // Save updated metadata
        saveMetadata();

        if (prunedCount > 0) {
            logger.log(Level.INFO, sm.getString("cache.pruned.summary", Integer.valueOf(prunedCount),
                    Long.valueOf(prunedSize / 1024 / 1024), Integer.valueOf(retentionDays)));
        } else {
            logger.log(Level.FINE, sm.getString("cache.pruned.none", Integer.valueOf(retentionDays)));
        }
    }

    /**
     * Finalize cache operations - save metadata and perform cleanup.
     * Should be called after migration completes.
     *
     * @throws IOException if an I/O error occurs
     */
    public void finalizeCacheOperations() throws IOException {
        // Save updated metadata
        saveMetadata();

        // Prune expired entries
        pruneCache();
    }

    /**
     * Get cache statistics.
     *
     * @return a string describing cache size and entry count
     */
    public String getStats() {
        long totalSize = 0;
        int entryCount = 0;

        File[] subdirs = cacheDir.listFiles();
        if (subdirs != null) {
            for (File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    File[] files = subdir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                totalSize += file.length();
                                entryCount++;
                            }
                        }
                    }
                }
            }
        }

        return sm.getString("cache.stats", Integer.valueOf(entryCount), Long.valueOf(totalSize / 1024 / 1024));
    }
}
