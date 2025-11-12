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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

/**
 * Cache for storing and retrieving pre-converted archive files.
 * Uses SHA-256 hashing of the pre-conversion content to identify files.
 */
public class MigrationCache {

    private static final Logger logger = Logger.getLogger(MigrationCache.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(MigrationCache.class);

    private final File cacheDir;
    private final boolean enabled;

    /**
     * Construct a new migration cache.
     *
     * @param cacheDir the directory to store cached files (null to disable caching)
     * @throws IOException if the cache directory cannot be created
     */
    public MigrationCache(File cacheDir) throws IOException {
        if (cacheDir == null) {
            this.cacheDir = null;
            this.enabled = false;
        } else {
            this.cacheDir = cacheDir;
            this.enabled = true;

            // Create cache directory if it doesn't exist
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    throw new IOException(sm.getString("cache.cannotCreate", cacheDir.getAbsolutePath()));
                }
            }

            if (!cacheDir.isDirectory()) {
                throw new IOException(sm.getString("cache.notDirectory", cacheDir.getAbsolutePath()));
            }

            logger.log(Level.INFO, sm.getString("cache.enabled", cacheDir.getAbsolutePath()));
        }
    }

    /**
     * Check if caching is enabled.
     *
     * @return true if caching is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Process an archive with caching support.
     * This method will:
     * 1. Hash the pre-conversion content
     * 2. Check if a cached post-conversion version exists
     * 3. If yes: copy cached result to output and return true
     * 4. If no: return false (caller should perform conversion and call storeCachedResult)
     *
     * @param name the archive name (for logging)
     * @param src the source input stream (will be consumed to compute hash)
     * @param dest the destination output stream
     * @return an InputStream containing the original source data, or null if cache hit
     * @throws IOException if an I/O error occurs
     */
    public InputStream checkCache(String name, InputStream src, OutputStream dest) throws IOException {
        if (!enabled) {
            return src;
        }

        // Read source into memory so we can hash it and potentially reuse it
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IOUtils.copy(src, buffer);
        byte[] sourceBytes = buffer.toByteArray();

        // Compute hash of pre-conversion content
        String hash = computeHash(sourceBytes);

        // Check if cached version exists
        File cachedFile = getCacheFile(hash);
        if (cachedFile.exists()) {
            // Cache hit! Copy cached (post-conversion) result to output
            logger.log(Level.INFO, sm.getString("cache.hit", name, hash));
            try (FileInputStream cachedInput = new FileInputStream(cachedFile)) {
                IOUtils.copy(cachedInput, dest);
            }
            return null; // Signal cache hit
        } else {
            // Cache miss - return source data for processing
            logger.log(Level.FINE, sm.getString("cache.miss", name, hash));
            return new ByteArrayInputStream(sourceBytes);
        }
    }

    /**
     * Store the result of a conversion in the cache.
     * This should be called after a successful conversion when checkCache returned false.
     *
     * @param sourceBytes the pre-conversion content (for hash computation)
     * @param convertedBytes the post-conversion content to cache
     * @throws IOException if an I/O error occurs
     */
    public void storeCachedResult(byte[] sourceBytes, byte[] convertedBytes) throws IOException {
        if (!enabled) {
            return;
        }

        String hash = computeHash(sourceBytes);
        File cachedFile = getCacheFile(hash);

        // Write converted content to cache
        try (FileOutputStream fos = new FileOutputStream(cachedFile)) {
            fos.write(convertedBytes);
        }

        logger.log(Level.FINE, sm.getString("cache.store", hash, cachedFile.length()));
    }

    /**
     * Check cache at file level for better performance.
     * This hashes the source file directly and checks for a cached result.
     *
     * @param sourceFile the source file to check
     * @param destFile the destination file to write to if cache hit
     * @return true if cache hit and file was written, false if cache miss
     * @throws IOException if an I/O error occurs
     */
    public boolean checkFileCache(File sourceFile, File destFile) throws IOException {
        if (!enabled) {
            return false;
        }

        // Compute hash of source file
        String hash = computeFileHash(sourceFile);

        // Check if cached version exists
        File cachedFile = getCacheFile(hash);
        if (cachedFile.exists()) {
            // Cache hit! Copy cached result to destination
            logger.log(Level.INFO, sm.getString("cache.hit", sourceFile.getName(), hash));
            try (FileInputStream cachedInput = new FileInputStream(cachedFile);
                    FileOutputStream destOutput = new FileOutputStream(destFile)) {
                IOUtils.copy(cachedInput, destOutput);
            }
            return true;
        } else {
            logger.log(Level.FINE, sm.getString("cache.miss", sourceFile.getName(), hash));
            return false;
        }
    }

    /**
     * Store a file-level cache entry.
     * This hashes the source file and stores the destination file in the cache.
     *
     * @param sourceFile the source file (for hash computation)
     * @param destFile the converted destination file to cache
     * @throws IOException if an I/O error occurs
     */
    public void storeFileCache(File sourceFile, File destFile) throws IOException {
        if (!enabled) {
            return;
        }

        // Compute hash of source file
        String hash = computeFileHash(sourceFile);
        File cachedFile = getCacheFile(hash);

        // Copy destination file to cache
        try (FileInputStream destInput = new FileInputStream(destFile);
                FileOutputStream cachedOutput = new FileOutputStream(cachedFile)) {
            IOUtils.copy(destInput, cachedOutput);
        }

        logger.log(Level.FINE, sm.getString("cache.store", hash, cachedFile.length()));
    }

    /**
     * Compute SHA-256 hash of a file efficiently using streaming.
     *
     * @param file the file to hash
     * @return the hash as a hex string
     * @throws IOException if hashing fails
     */
    private String computeFileHash(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(sm.getString("cache.hashError"), e);
        }
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
     * Compute SHA-256 hash of the given bytes.
     *
     * @param bytes the bytes to hash
     * @return the hash as a hex string
     * @throws IOException if hashing fails
     */
    private String computeHash(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
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
        if (!enabled) {
            return;
        }

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
        Files.deleteIfExists(dir.toPath());
    }

    /**
     * Get cache statistics.
     *
     * @return a string describing cache size and entry count
     */
    public String getStats() {
        if (!enabled) {
            return sm.getString("cache.disabled");
        }

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

        return sm.getString("cache.stats", entryCount, totalSize / 1024 / 1024);
    }
}
