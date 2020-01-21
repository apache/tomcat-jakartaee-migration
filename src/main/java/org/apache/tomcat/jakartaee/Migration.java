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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Migration {

    private static final Logger logger = Logger.getLogger(Migration.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(Migration.class);

    private File source;
    private File destination;
    private final List<Converter> converters;

    public Migration() {
        // Initialise the converters
        converters = new ArrayList<>();

        converters.add(new TextConverter());
        converters.add(new ClassConverter());

        // Final converter is the NoOpConverter
        converters.add(new NoOpConverter());
    }


    public void setSource(File source) {
        if (!source.canRead()) {
            throw new IllegalArgumentException(sm.getString("migration.cannotReadSource",
                    source.getAbsolutePath()));
        }
        this.source = source;
    }


    public void setDestination(File destination) {
        this.destination = destination;
    }


    public boolean execute() throws IOException {
        logger.log(Level.INFO, sm.getString("migration.execute", source.getAbsolutePath(),
                destination.getAbsolutePath()));
        boolean result = true;
        long t1 = System.nanoTime();
        if (source.isDirectory()) {
            if (destination.mkdirs()) {
                result = result && migrateDirectory(source, destination);
            } else {
                logger.log(Level.WARNING, sm.getString("migration.mkdirError", destination.getAbsolutePath()));
                result = false;
            }
        } else {
            // Single file
            File parentDestination = destination.getParentFile();
            if (parentDestination.exists() || parentDestination.mkdirs()) {
                result = result && migrateFile(source, destination);
            } else {
                logger.log(Level.WARNING, sm.getString("migration.mkdirError", parentDestination.getAbsolutePath()));
                result = false;
            }
        }
        logger.log(Level.INFO, sm.getString("migration.done",
                Long.valueOf(TimeUnit.MILLISECONDS.convert(System.nanoTime() - t1, TimeUnit.NANOSECONDS)),
                Boolean.valueOf(result)));
        return result;
    }


    private boolean migrateDirectory(File src, File dest) throws IOException {
        boolean result = true;
        String[] files = src.list();
        for (String file : files) {
            File srcFile = new File(src, file);
            File destFile = new File(dest, file);
            if (srcFile.isDirectory()) {
                if (destFile.mkdir()) {
                    result = result && migrateDirectory(srcFile, destFile);
                } else {
                    logger.log(Level.WARNING, sm.getString("migration.mkdirError", destFile.getAbsolutePath()));
                    result = false;
                }
            } else {
                result = result && migrateFile(srcFile, destFile);
            }
        }
        return result;
    }


    private boolean migrateFile(File src, File dest) throws IOException {
        try (InputStream is = new FileInputStream(src);
                OutputStream os = new FileOutputStream(dest)) {
            return migrateStream(src.getName(), is, os);
        }
    }


    private boolean migrateArchive(InputStream src, OutputStream dest) throws IOException {
        boolean result = true;
        try (JarInputStream jarIs = new JarInputStream(new NonClosingInputStream(src));
                JarOutputStream jarOs = new JarOutputStream(new NonClosingOutputStream(dest))) {
            Manifest manifest = jarIs.getManifest();
            if (manifest != null) {
                updateVersion(manifest);
                JarEntry manifestEntry = new JarEntry(JarFile.MANIFEST_NAME);
                jarOs.putNextEntry(manifestEntry);
                manifest.write(jarOs);
            }
            JarEntry jarEntry;
            while ((jarEntry = jarIs.getNextJarEntry()) != null) {
                String sourceName = jarEntry.getName();
                logger.log(Level.FINE, sm.getString("migration.entry", sourceName));
                String destName = Util.convert(sourceName);
                JarEntry destEntry = new JarEntry(destName);
                jarOs.putNextEntry(destEntry);
                result = result && migrateStream(destEntry.getName(), jarIs, jarOs);
            }
        }
        return result;
    }


    private boolean migrateStream(String name, InputStream src, OutputStream dest) throws IOException {
        if (isArchive(name)) {
            logger.log(Level.INFO, sm.getString("migration.archive", name));
            return migrateArchive(src, dest);
        } else {
            logger.log(Level.FINE, sm.getString("migration.stream", name));
            for (Converter converter : converters) {
                if (converter.accpets(name)) {
                    converter.convert(src, dest);
                    break;
                }
            }
            return true;
        }
    }


    private void updateVersion(Manifest manifest) {
        updateVersion(manifest.getMainAttributes());
        for (Attributes attributes : manifest.getEntries().values()) {
            updateVersion(attributes);
        }
    }


    private void updateVersion(Attributes attributes) {
        if (attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
            String newValue = attributes.get(Attributes.Name.IMPLEMENTATION_VERSION) + "-" + Info.getVersion();
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, newValue);
        }
    }


    public static void main(String[] args) {
        if (args.length != 2) {
            usage();
            System.exit(1);
        }
        Migration migration = new Migration();
        migration.setSource(new File(args[0]));
        migration.setDestination(new File(args[1]));
        boolean result = false;
        try {
            result = migration.execute();
        } catch (IOException e) {
            logger.log(Level.SEVERE, sm.getString("migration.error"), e);
            result = false;
        }

        // Signal caller that migration failed
        if (!result) {
            System.exit(1);
        }
    }


    private static void usage() {
        System.out.println(sm.getString("migration.usage"));
    }


    private static boolean isArchive(String fileName) {
        return fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".zip");
    }
}
