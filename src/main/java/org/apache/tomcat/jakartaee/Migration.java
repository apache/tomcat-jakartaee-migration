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
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipShort;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

/**
 * The main class for the Migration tool.
 */
public class Migration {

    private static final Logger logger = Logger.getLogger(Migration.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(Migration.class);

    private static final Set<String> DEFAULT_EXCLUDES = new HashSet<>();

    private static final long TEMP_FILE_THRESHOLD = 10L * 1024 * 1024;
    private static final ZipShort EXTRA_FIELD_ZIP64 = new ZipShort(1);
    private static final long ZIP64_THRESHOLD_LENGTH = 0xFFFFFFFFL;

    static {
        // Apache Commons
        DEFAULT_EXCLUDES.add("commons-codec-*.jar");
        DEFAULT_EXCLUDES.add("commons-lang-*.jar");
        // Apache HTTP Components
        DEFAULT_EXCLUDES.add("httpclient-*.jar");
        DEFAULT_EXCLUDES.add("httpcore-*.jar");
        // ASM
        DEFAULT_EXCLUDES.add("asm-*.jar");
        // AspectJ
        DEFAULT_EXCLUDES.add("aspectjweaver-*.jar");
        // Bouncy Castle JCE provider
        DEFAULT_EXCLUDES.add("bcprov*.jar");
        DEFAULT_EXCLUDES.add("bcpkix*.jar");
        // Closure compiler
        DEFAULT_EXCLUDES.add("closure-compiler-*.jar");
        // Eclipse compiler for Java
        DEFAULT_EXCLUDES.add("ecj-*.jar");
        // Hystrix
        DEFAULT_EXCLUDES.add("hystrix-core-*.jar");
        DEFAULT_EXCLUDES.add("hystrix-serialization-*.jar");
        // Jackson
        DEFAULT_EXCLUDES.add("jackson-annotations-*.jar");
        DEFAULT_EXCLUDES.add("jackson-core-*.jar");
        DEFAULT_EXCLUDES.add("jackson-module-afterburner-*.jar");
        // Logging
        DEFAULT_EXCLUDES.add("jul-to-slf4j-*.jar");
        DEFAULT_EXCLUDES.add("log4j-to-slf4j-*.jar");
        DEFAULT_EXCLUDES.add("slf4j-api-*.jar");
        // Spring
        DEFAULT_EXCLUDES.add("spring-aop-*.jar");
        DEFAULT_EXCLUDES.add("spring-expression-*.jar");
        DEFAULT_EXCLUDES.add("spring-security-crypto-*.jar");
        DEFAULT_EXCLUDES.add("spring-security-rsa-*.jar");
    }

    private EESpecProfile profile = EESpecProfiles.TOMCAT;

    private boolean enableDefaultExcludes = true;
    private boolean matchExcludesAgainstPathName;
    private boolean zipInMemory;
    private boolean converted;
    private State state = State.NOT_STARTED;
    private File source;
    private File destination;
    private final List<Converter> converters;
    private final Set<String> excludes = new HashSet<>();
    private MigrationCache cache;

    /**
     * Construct a new migration tool instance.
     */
    public Migration() {
        // Initialise the converters
        converters = new ArrayList<>();

        converters.add(new TextConverter());
        converters.add(new ClassConverter());
        converters.add(new ManifestConverter());

        // Final converter is the pass-through converter
        converters.add(new PassThroughConverter());
    }

    /**
     * The tool state.
     */
    public enum State {
        /**
         * Migration not started yet.
         */
        NOT_STARTED,
        /**
         * Migration in progress.
         */
        RUNNING,
        /**
         * Migration complete.
         */
        COMPLETE
    }

    /**
     * Set the Jakarta EE specifications that should be used.
     *
     * @param profile the Jakarta EE specification profile
     */
    public void setEESpecProfile(EESpecProfile profile) {
        this.profile = profile;
    }

    /**
     * Get the Jakarta EE profile being used.
     *
     * @return the profile
     */
    public EESpecProfile getEESpecProfile() {
        return profile;
    }

    /**
     * Enable the default exclusion list for the tool.
     * @param enableDefaultExcludes true to enable the default excludes
     */
    public void setEnableDefaultExcludes(boolean enableDefaultExcludes) {
        this.enableDefaultExcludes = enableDefaultExcludes;
    }

    /**
     * Enable exclude matching against the path name.
     * @param matchExcludesAgainstPathName true to match excludes against the path name instead of the file name
     */
    public void setMatchExcludesAgainstPathName(boolean matchExcludesAgainstPathName) {
        this.matchExcludesAgainstPathName = matchExcludesAgainstPathName;
    }

    /**
     * Buffer all conversion operations for compressed archives in memory.
     * @param zipInMemory true to buffer in memory
     */
    public void setZipInMemory(boolean zipInMemory) {
        this.zipInMemory = zipInMemory;
    }

    /**
     * Add specified resource exclusion.
     * @param exclude the exclude to add
     */
    public void addExclude(String exclude) {
        this.excludes.add(exclude);
    }

    /**
     * Set source file or directory.
     * @param source the source file or directory
     */
    public void setSource(File source) {
        if (!source.canRead()) {
            throw new IllegalArgumentException(sm.getString("migration.cannotReadSource",
                    source.getAbsolutePath()));
        }
        this.source = source;
    }

    /**
     * Set destination file or directory.
     * @param destination the destination file or directory
     */
    public void setDestination(File destination) {
        this.destination = destination;
    }

    /**
     * Set the migration cache for storing pre-converted archives.
     * @param cache the migration cache instance (null to disable caching)
     */
    public void setCache(MigrationCache cache) {
        this.cache = cache;
    }


    /**
     * Returns whether any files were converted during migration.
     * Note: a return value of {@code false} means the source already
     * satisfied the selected profile and no changes were necessary.
     *
     * @return true if at least one file was converted
     * @throws IllegalStateException if migration has not completed
     */
    public boolean hasConverted() {
        if (state != State.COMPLETE) {
            throw new IllegalStateException(sm.getString("migration.notCompleted"));
        }
        return converted;
    }


    /**
     * Execute migration operation.
     * @throws IOException when an exception occurs
     * @throws IllegalStateException if migration is already running
     */
    public void execute() throws IOException {
        if (state == State.RUNNING) {
            throw new IllegalStateException(sm.getString("migration.alreadyRunning"));
        }
        state = State.RUNNING;
        converted = false;

        logger.log(Level.INFO, sm.getString("migration.execute", source.getAbsolutePath(),
                destination.getAbsolutePath(), profile.toString()));

        long t1 = System.nanoTime();
        boolean failed = false;
        try {
            if (source.isDirectory()) {
                if (!destination.exists()) {
                    if (!destination.mkdirs()) {
                        throw new IOException(sm.getString("migration.mkdirError",
                                destination.getAbsolutePath()));
                    }
                }
                if (!destination.isDirectory()) {
                    throw new IOException(sm.getString("migration.mkdirError",
                            destination.getAbsolutePath()));
                }
                migrateDirectory(source, destination);
            } else {
                // Single file
                File parentDestination = destination.getAbsoluteFile().getParentFile();
                if (!parentDestination.exists() && !parentDestination.mkdirs()) {
                    throw new IOException(sm.getString("migration.mkdirError",
                            parentDestination.getAbsolutePath()));
                }
                migrateFile(source, destination);
            }
        } catch (IOException e) {
            failed = true;
            throw e;
        } finally {
            state = failed ? State.NOT_STARTED : State.COMPLETE;

            // Finalize cache operations (save metadata and prune expired entries).
            // A failure here must not mask a migration failure or cause a
            // successful migration to be reported as failed.
            if (cache != null) {
                try {
                    cache.pruneCache();
                } catch (IOException e) {
                    logger.log(Level.WARNING, sm.getString("migration.cachePruneFailed"), e);
                }
            }
        }

        logger.log(Level.INFO, sm.getString("migration.done",
                Long.valueOf(TimeUnit.MILLISECONDS.convert(System.nanoTime() - t1, TimeUnit.NANOSECONDS))));
    }

    private void migrateDirectory(File src, File dest) throws IOException {
        // May return null if src ceases to be a directory (e.g. it is
        // removed) between the isDirectory() check and this call
        String[] files = src.list();
        if (files == null) {
            throw new IOException(sm.getString("migration.listError", src.getAbsolutePath()));
        }
        for (String file : files) {
            File srcFile = new File(src, file);
            File destFile = new File(dest, profile.convert(file));
            if (srcFile.isDirectory()) {
                if (!destFile.exists()) {
                    if (!destFile.mkdir()) {
                        throw new IOException(sm.getString("migration.mkdirError", destFile.getAbsolutePath()));
                    }
                }
                if (!destFile.isDirectory()) {
                    throw new IOException(sm.getString("migration.mkdirError", destFile.getAbsolutePath()));
                }
                migrateDirectory(srcFile, destFile);
            } else {
                migrateFile(srcFile, destFile);
            }
        }
    }

    private void migrateFile(File src, File dest) throws IOException {
        if (src.equals(dest)) {
            if (src.length() > TEMP_FILE_THRESHOLD) {
                // For very large files, use a temp file instead of memory
                File tempFile = createTempFile();
                tempFile.deleteOnExit();
                try (InputStream is = new FileInputStream(src); OutputStream os = new FileOutputStream(tempFile)) {
                    if (migrateStream(src.getAbsolutePath(), is, os)) {
                        converted = true;
                        try (InputStream tempIs = new FileInputStream(tempFile); OutputStream destOs = new FileOutputStream(dest)) {
                            Util.copy(tempIs, destOs);
                        }
                    } else {
                        return;
                    }
                } finally {
                    tempFile.delete();
                }
            } else {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.toIntExact((long) (src.length() * 1.05)));

                try (InputStream is = new FileInputStream(src)) {
                    if (migrateStream(src.getAbsolutePath(), is, buffer)) {
                        converted = true;
                    } else {
                        return;
                    }
                }

                try (OutputStream os = new FileOutputStream(dest)) {
                    os.write(buffer.toByteArray());
                }
            }
        } else {
            try (InputStream is = new FileInputStream(src);
                    OutputStream os = new FileOutputStream(dest)) {
                if (migrateStream(src.getAbsolutePath(), is, os)) {
                    converted = true;
                }
            } catch (IOException e) {
                // Remove the partially written destination file
                dest.delete();
                throw e;
            }
        }
    }


    private boolean migrateArchiveStreaming(InputStream src, OutputStream dest) throws IOException {
        boolean convertedArchive = false;
        try (ZipArchiveInputStream srcZipStream = new ZipArchiveInputStream(CloseShieldInputStream.wrap(src));
                ZipArchiveOutputStream destZipStream = new ZipArchiveOutputStream(CloseShieldOutputStream.wrap(dest))) {
            ZipArchiveEntry srcZipEntry;
            while ((srcZipEntry = srcZipStream.getNextEntry()) != null) {
                boolean convertedStream = false;
                String srcName = srcZipEntry.getName();
                if (isSignatureFile(srcName)) {
                    logger.log(Level.WARNING, sm.getString("migration.skipSignatureFile", srcName));
                    continue;
                }
                if (srcZipEntry.getSize() > ZIP64_THRESHOLD_LENGTH ||
                        srcZipEntry.getCompressedSize() > ZIP64_THRESHOLD_LENGTH) {
                    logger.log(Level.WARNING, sm.getString("migration.jdk8303866", srcName));
                } else {
                    // Avoid JDK bug - https://bugs.openjdk.org/browse/JDK-8303866
                    if (srcZipEntry.getExtraField(EXTRA_FIELD_ZIP64) != null) {
                        srcZipEntry.removeExtraField(EXTRA_FIELD_ZIP64);
                    }
                }
                String destName = profile.convert(srcName);
                if (srcZipEntry.getMethod() == ZipEntry.STORED) {
                    try (CrcSizeTrackingOutputStream trackingStream = new CrcSizeTrackingOutputStream(destZipStream)) {
                        convertedStream = migrateStream(srcName, srcZipStream, trackingStream);
                        MigrationZipArchiveEntry destZipEntry = new MigrationZipArchiveEntry(srcZipEntry);
                        destZipEntry.setName(destName);
                        destZipEntry.setSize(trackingStream.getSize());
                        destZipEntry.setCrc(trackingStream.getCrc());
                        if (convertedStream) {
                            destZipEntry.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
                        }
                        destZipStream.putArchiveEntry(destZipEntry);
                    }
                    destZipStream.closeArchiveEntry();
                } else {
                    MigrationZipArchiveEntry destZipEntry = new MigrationZipArchiveEntry(srcZipEntry);
                    destZipEntry.setName(destName);
                    destZipStream.putArchiveEntry(destZipEntry);
                    convertedStream = migrateStream(srcName, srcZipStream, destZipStream);
                    if (convertedStream) {
                        destZipEntry.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
                    }
                    destZipStream.closeArchiveEntry();
                }
                convertedArchive = convertedArchive || convertedStream;
            }
        }
        return convertedArchive;
    }


    private boolean migrateArchiveInMemory(InputStream src, OutputStream dest) throws IOException {
        boolean convertedArchive = false;
        // Read the source into memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(src, baos);
        baos.flush();
        SeekableInMemoryByteChannel srcByteChannel = new SeekableInMemoryByteChannel(baos.toByteArray());
        // Create the destination in memory
        SeekableInMemoryByteChannel destByteChannel = new SeekableInMemoryByteChannel();

        // In memory can have much simpler processing compared to the streaming version,
        // including STORED entries processing, due to the use of a seekable channel
        try (ZipFile srcZipFile = ZipFile.builder().setSeekableByteChannel(srcByteChannel).get();
                ZipArchiveOutputStream destZipStream = new ZipArchiveOutputStream(destByteChannel)) {
            Enumeration<ZipArchiveEntry> entries = srcZipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry srcZipEntry = entries.nextElement();
                String srcName = srcZipEntry.getName();
                if (isSignatureFile(srcName)) {
                    logger.log(Level.WARNING, sm.getString("migration.skipSignatureFile", srcName));
                    continue;
                }
                String destName = profile.convert(srcName);
                MigrationZipArchiveEntry destZipEntry = new MigrationZipArchiveEntry(srcZipEntry);
                destZipEntry.setName(destName);
                destZipStream.putArchiveEntry(destZipEntry);
                boolean convertedStream = migrateStream(srcName, srcZipFile.getInputStream(srcZipEntry), destZipStream);
                if (convertedStream) {
                    destZipEntry.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
                }
                destZipStream.closeArchiveEntry();
                convertedArchive = convertedArchive || convertedStream;
            }
        }

        // Write the destination back to the stream
        ByteArrayInputStream bais = new ByteArrayInputStream(destByteChannel.array(), 0, Math.toIntExact(destByteChannel.size()));
        IOUtils.copy(bais, dest);

        return convertedArchive;
    }


    private boolean isSignatureFile(String sourceName) {
        return sourceName.startsWith("META-INF/") && (
                sourceName.endsWith(".SF") ||
                sourceName.endsWith(".RSA") ||
                sourceName.endsWith(".DSA") ||
                sourceName.endsWith(".EC")
                );
    }


    private boolean migrateStream(String name, InputStream src, OutputStream dest) throws IOException {
        boolean convertedStream = false;
        if (isExcluded(name)) {
            Util.copy(src, dest);
            logger.log(Level.INFO, sm.getString("migration.skip", name));
        } else if (isArchive(name)) {
            // Only cache nested archives (e.g., JARs inside WARs), not top-level
            // files which will have absolute paths
            boolean isNestedArchive = !new File(name).isAbsolute();

            CacheEntry cacheEntry = null;
            SourceSpool sourceSpool = null;
            if (isNestedArchive && cache != null) {
                // Spool source so the cache hash can be computed and, on a cache
                // miss, the source can be re-read for conversion. Data above
                // TEMP_FILE_THRESHOLD is spooled to a temp file to avoid
                // unbounded memory usage.
                sourceSpool = new SourceSpool(profile);
                try {
                    IOUtils.copy(src, sourceSpool);
                } catch (IOException e) {
                    sourceSpool.discard();
                    throw e;
                }
                String hash = sourceSpool.getHash();

                // Get cache entry (marks as accessed)
                cacheEntry = cache.getCacheEntry(hash);

                if (cacheEntry.exists()) {
                    try {
                        // Cache hit! Copy cached result to dest and return
                        logger.log(Level.INFO, sm.getString("cache.hit", name, hash));
                        cacheEntry.copyToDestination(dest);
                    } finally {
                        sourceSpool.discard();
                    }
                    // Although it is from the cache, this still counts as converting the source
                    return true;
                }

                // Cache miss - use spooled source for conversion
                logger.log(Level.FINE, sm.getString("cache.miss", name, hash));
                src = sourceSpool.toInputStream();
            }

            // Process archive - stream directly to destination (and cache if needed)
            try {
                OutputStream targetOutputStream = dest;
                if (cacheEntry != null) {
                    // Tee output to both destination and cache temp file
                    targetOutputStream = new org.apache.commons.io.output.TeeOutputStream(dest, cacheEntry.beginStore());
                }

                if (zipInMemory) {
                    logger.log(Level.INFO, sm.getString("migration.archive.memory", name));
                    convertedStream = migrateArchiveInMemory(src, targetOutputStream);
                    logger.log(Level.INFO, sm.getString("migration.archive.complete", name));
                } else {
                    logger.log(Level.INFO, sm.getString("migration.archive.stream", name));
                    convertedStream = migrateArchiveStreaming(src, targetOutputStream);
                    logger.log(Level.INFO, sm.getString("migration.archive.complete", name));
                }

                // Commit to cache on success
                if (cacheEntry != null) {
                    cacheEntry.commitStore();
                    logger.log(Level.FINE, sm.getString("cache.store", cacheEntry.getHash(),
                            Long.valueOf(cacheEntry.getFileSize())));
                }
            } catch (Exception e) {
                // Rollback cache on error
                if (cacheEntry != null) {
                    cacheEntry.rollbackStore();
                }
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                throw e;
            } finally {
                if (sourceSpool != null) {
                    sourceSpool.discard();
                }
            }
        } else {
            for (Converter converter : converters) {
                if (converter.accepts(name)) {
                    convertedStream = converter.convert(name, src, dest, profile);
                    break;
                }
            }
        }
        return convertedStream;
    }

    private boolean isArchive(String fileName) {
        return fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".ear") ||
                fileName.endsWith(".zip");
    }


    private boolean isExcluded(String name) {
        File f = new File(name);
        String filename = f.getName();

        if (enableDefaultExcludes && GlobMatcher.matchName(DEFAULT_EXCLUDES, filename, true)) {
            return true;
        }

        if (!matchExcludesAgainstPathName && GlobMatcher.matchName(excludes, filename, true)) {
            return true;
        }
        if (matchExcludesAgainstPathName && GlobMatcher.matchName(excludes, name, true)) {
            return true;
        }

        return false;
    }

    private static class MigrationZipArchiveEntry extends ZipArchiveEntry {

        MigrationZipArchiveEntry(ZipArchiveEntry entry) throws ZipException {
            super(entry);
        }

        @Override
        public void setName(String name) {
            super.setName(name);
        }
    }

    private static File createTempFile() throws IOException {
        return File.createTempFile("jakartaee-migration-", ".tmp");
    }

    /**
     * An output stream that spools written data into an in-memory buffer,
     * switching to a temporary file once the data exceeds
     * TEMP_FILE_THRESHOLD, to avoid unbounded memory usage. Subclasses track
     * properties of the spooled data (e.g., a checksum) using
     * {@link #update(int)} and {@link #update(byte[], int, int)} and consume
     * or release the spooled data using {@link #writeTo(OutputStream)},
     * {@link #toInputStream()} or {@link #discard()}.
     */
    private abstract static class SpoolingOutputStream extends OutputStream {

        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private FileOutputStream fileOutput;
        private File tempFile;
        private FileInputStream tempFileIs;

        @Override
        public void write(int b) throws IOException {
            if (fileOutput != null) {
                fileOutput.write(b);
            } else {
                buffer.write(b);
                maybeSwitchToFile();
            }
            update(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (fileOutput != null) {
                fileOutput.write(b, off, len);
            } else {
                buffer.write(b, off, len);
                maybeSwitchToFile();
            }
            update(b, off, len);
        }

        /**
         * Track the written byte.
         * @param b the byte that was written
         * @throws IOException if an I/O error occurs
         */
        protected abstract void update(int b) throws IOException;

        /**
         * Track the written bytes.
         * @param b the bytes that were written
         * @param off the starting offset in the byte array
         * @param len the number of bytes that were written
         * @throws IOException if an I/O error occurs
         */
        protected abstract void update(byte[] b, int off, int len) throws IOException;

        private void maybeSwitchToFile() throws IOException {
            if (buffer.size() > TEMP_FILE_THRESHOLD && fileOutput == null) {
                tempFile = createTempFile();
                tempFile.deleteOnExit();
                fileOutput = new FileOutputStream(tempFile);
                buffer.writeTo(fileOutput);
                fileOutput.flush();
                buffer = null;
            }
        }

        /**
         * Write the spooled data to the specified stream and release the
         * spooled data.
         * @param dest the stream to write the spooled data to
         * @throws IOException if an I/O error occurs
         */
        protected void writeTo(OutputStream dest) throws IOException {
            if (fileOutput != null) {
                IOException closeException = null;
                try {
                    fileOutput.close();
                } catch (IOException e) {
                    closeException = e;
                } finally {
                    fileOutput = null;
                }
                try (FileInputStream fis = new FileInputStream(tempFile)) {
                    IOUtils.copy(fis, dest);
                } finally {
                    tempFile.delete();
                    tempFile = null;
                }
                if (closeException != null) {
                    throw closeException;
                }
            } else if (buffer != null) {
                try {
                    buffer.writeTo(dest);
                } finally {
                    buffer.close();
                    buffer = null;
                }
            }
        }

        /**
         * Get an input stream over the spooled data. The spooled data is
         * retained until {@link #discard()} is called.
         * @return an input stream over the spooled data
         * @throws IOException if an I/O error occurs
         */
        protected InputStream toInputStream() throws IOException {
            if (fileOutput != null) {
                fileOutput.close();
                fileOutput = null;
                tempFileIs = new FileInputStream(tempFile);
                return tempFileIs;
            }
            return new ByteArrayInputStream(buffer.toByteArray());
        }

        /**
         * Release all spooled data. Safe to call multiple times.
         */
        protected void discard() {
            if (fileOutput != null) {
                try {
                    fileOutput.close();
                } catch (IOException e) {
                    // Ignore
                }
                fileOutput = null;
            }
            if (tempFileIs != null) {
                try {
                    tempFileIs.close();
                } catch (IOException e) {
                    // Ignore
                }
                tempFileIs = null;
            }
            if (tempFile != null) {
                tempFile.delete();
                tempFile = null;
            }
            buffer = null;
        }
    }

    /**
     * Output stream that tracks the CRC32 checksum and byte count of written
     * data, spooling to a temporary file when the data exceeds
     * TEMP_FILE_THRESHOLD to avoid excessive memory usage. On close, the
     * spooled data is written to the destination stream. Used for computing
     * the CRC and size of STORED zip entries during streaming migration.
     */
    private static class CrcSizeTrackingOutputStream extends SpoolingOutputStream {

        private final CRC32 crc = new CRC32();
        private long size;
        private final OutputStream destStream;

        /**
         * Create stream that computes the CRC and size of its bytes as they
         * are written and writes those bytes to the specified stream on
         * close.
         * @param destStream the destination stream to write the bytes to
         */
        CrcSizeTrackingOutputStream(OutputStream destStream) {
            this.destStream = destStream;
        }

        @Override
        protected void update(int b) {
            crc.update(b);
            size++;
        }

        @Override
        protected void update(byte[] b, int off, int len) {
            crc.update(b, off, len);
            size += len;
        }

        public long getSize() {
            return size;
        }

        public long getCrc() {
            return crc.getValue();
        }

        @Override
        public void close() throws IOException {
            writeTo(destStream);
        }
    }

    /**
     * Spools archive source data while computing the SHA-256 hash used to
     * key the migration cache. The hash includes the profile name and must
     * be computed the same way as MigrationCache computes cache hashes.
     */
    private static class SourceSpool extends SpoolingOutputStream {

        private final MessageDigest digest;

        SourceSpool(EESpecProfile profile) throws IOException {
            try {
                digest = MessageDigest.getInstance("SHA-256");
                // Include profile name in hash to differentiate between profiles
                digest.update(profile.toString().getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(sm.getString("cache.hashError"), e);
            }
        }

        @Override
        protected void update(int b) {
            digest.update((byte) b);
        }

        @Override
        protected void update(byte[] b, int off, int len) {
            digest.update(b, off, len);
        }

        /**
         * Get the hash of the spooled data. Must only be called once all data
         * has been written.
         * @return the hash as a hex string
         */
        String getHash() {
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", Integer.valueOf(b & 0xFF)));
            }
            return sb.toString();
        }
    }
}
