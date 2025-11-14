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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Represents a single cache entry with operations for reading and writing.
 * Package-private - only created by MigrationCache.
 */
class CacheEntry {
    private final String hash;
    private final boolean exists;
    private final File cacheFile;
    private final File tempFile;

    CacheEntry(String hash, boolean exists, File cacheFile, File tempFile) {
        this.hash = hash;
        this.exists = exists;
        this.cacheFile = cacheFile;
        this.tempFile = tempFile;
    }

    /**
     * Check if this entry exists in the cache.
     * @return true if cached
     */
    public boolean exists() {
        return exists;
    }

    /**
     * Get the hash for this cache entry.
     * @return the hash string
     */
    public String getHash() {
        return hash;
    }

    /**
     * Copy cached content to destination output stream.
     * @param dest the destination output stream
     * @throws IOException if an I/O error occurs
     */
    public void copyToDestination(OutputStream dest) throws IOException {
        if (!exists) {
            throw new IllegalStateException("Cannot copy - cache entry does not exist");
        }
        try (FileInputStream fis = new FileInputStream(cacheFile)) {
            IOUtils.copy(fis, dest);
        }
    }

    /**
     * Begin storing to cache - returns an output stream to a temp file.
     * @return output stream to write converted content to
     * @throws IOException if an I/O error occurs
     */
    public OutputStream beginStore() throws IOException {
        return new FileOutputStream(tempFile);
    }

    /**
     * Commit the store operation - move temp file to final cache location.
     * @throws IOException if an I/O error occurs
     */
    public void commitStore() throws IOException {
        if (!tempFile.exists()) {
            throw new IOException("Temp file does not exist: " + tempFile);
        }
        // Ensure parent directory exists
        File parentDir = cacheFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        // Atomic rename
        if (!tempFile.renameTo(cacheFile)) {
            throw new IOException("Failed to rename temp file to cache file: " + tempFile + " -> " + cacheFile);
        }
    }

    /**
     * Get the size of the cached file in bytes.
     * @return the file size in bytes
     */
    public long getFileSize() {
        return cacheFile.length();
    }

    /**
     * Rollback the store operation - delete temp file.
     */
    public void rollbackStore() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }
}
