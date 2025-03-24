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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Updates Manifests.
 */
public class ManifestConverter implements Converter {

    private static final String JAKARTA_SERVLET = "jakarta.servlet";
    private static final Pattern SERVLET_PATTERN = Pattern.compile("jakarta.servlet([^,]*);version=\"(.*?)\"");
    private static final Logger logger = Logger.getLogger(ManifestConverter.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(ManifestConverter.class);

    /**
     * Manifest converter constructor.
     */
    public ManifestConverter() {
    }

    @Override
    public boolean accepts(String filename) {
        if (filename.equals(JarFile.MANIFEST_NAME) || filename.endsWith("/" + JarFile.MANIFEST_NAME)) {
            return true;
        }

        // Reject everything else
        return false;
    }

    @Override
    public boolean convert(String path, InputStream src, OutputStream dest, EESpecProfile profile) throws IOException {
        byte[] srcBytes = IOUtils.toByteArray(src);
        Manifest srcManifest = new Manifest(new ByteArrayInputStream(srcBytes));
        Manifest destManifest = new Manifest(srcManifest);

        // Only consider profile conversions, allowing Migration.hasConverted to be true
        // only when there are actual
        // conversions made
        boolean converted = updateValues(destManifest, profile);
        removeSignatures(destManifest);

        if (srcManifest.equals(destManifest)) {
            IOUtils.writeChunked(srcBytes, dest);
            logger.log(Level.FINEST, sm.getString("manifestConverter.noConversion", path));
        } else {
            destManifest.write(dest);
            String key = converted ? "manifestConverter.converted" : "manifestConverter.updated";
            logger.log(Level.FINE, sm.getString(key, path));
        }

        return converted;
    }

    private void removeSignatures(Manifest manifest) {
        manifest.getMainAttributes().remove(Attributes.Name.SIGNATURE_VERSION);
        List<String> signatureEntries = new ArrayList<>();
        Map<String, Attributes> manifestAttributeEntries = manifest.getEntries();
        for (Entry<String, Attributes> entry : manifestAttributeEntries.entrySet()) {
            if (isCryptoSignatureEntry(entry.getValue())) {
                String entryName = entry.getKey();
                signatureEntries.add(entryName);
                logger.log(Level.FINE, sm.getString("manifestConverter.removeSignature", entryName));
            }
        }

        for (String entry : signatureEntries) {
            manifestAttributeEntries.remove(entry);
        }
    }

    private boolean isCryptoSignatureEntry(Attributes attributes) {
        for (Object attributeKey : attributes.keySet()) {
            if (attributeKey.toString().endsWith("-Digest")) {
                return true;
            }
        }
        return false;
    }

    private boolean updateValues(Manifest manifest, EESpecProfile profile) {
        boolean converted = updateValues(manifest.getMainAttributes(), profile);
        for (Attributes attributes : manifest.getEntries().values()) {
            converted = converted | updateValues(attributes, profile);
        }
        return converted;
    }

    private boolean updateValues(Attributes attributes, EESpecProfile profile) {
        boolean converted = false;
        // Update version info
        if (attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
            String newValue = attributes.get(Attributes.Name.IMPLEMENTATION_VERSION) + "-" + Info.getVersion();
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, newValue);
            logger.log(Level.FINE, sm.getString("manifestConverter.updatedVersion", newValue));
            // Purposefully avoid setting result
        }
        // Update package names in values
        for (Entry<Object, Object> entry : attributes.entrySet()) {
            String newValue = profile.convert((String) entry.getValue());
            String header = entry.getKey().toString();
            try {
                // Need to be careful with OSGI headers.
                // Specifically, Export-Package cannot specify a version range.
                // There may be other weird things as well (like directives that have
                // jakarta.servlet packages).
                if (Constants.IMPORT_PACKAGE.equals(header)) {
                    newValue = processImportPackage(newValue);
                } else if (Constants.EXPORT_PACKAGE.equals(header)) {
                    newValue = processExportPackage(newValue);
                } else {
                    newValue = replaceVersion(newValue);
                }
            } catch (BundleException e) {
                newValue = replaceVersion(newValue, !Constants.EXPORT_PACKAGE.equals(header));
            }

            // Object comparison is deliberate
            if (!newValue.equals(entry.getValue())) {
                entry.setValue(newValue);
                converted = true;
            }
        }
        return converted;
    }

    private String processExportPackage(String value) throws BundleException {
        return processOSGIHeader(value, Constants.EXPORT_PACKAGE, "5.0.0");
    }

    private String processImportPackage(String value) throws BundleException {
        return processOSGIHeader(value, Constants.IMPORT_PACKAGE, "[5.0.0,7.0.0)");
    }

    private String processOSGIHeader(String value, String header, String replacement) throws BundleException {
        List<String> packages = new ArrayList<>();
        ManifestElement[] elements = ManifestElement.parseHeader(header, value);
        for (ManifestElement element : elements) {
            if (element.getValue().startsWith(JAKARTA_SERVLET)) {
                String oldVersion = element.getAttribute(Constants.VERSION_ATTRIBUTE);
                if (oldVersion != null) {
                    packages.add(element.toString().replace(oldVersion, replacement));
                } else {
                    packages.add(element.toString());
                }
            } else {
                packages.add(element.toString());
            }
        }
        if (packages.isEmpty()) {
            return value;
        }
        return String.join(",", packages);
    }

    private String replaceVersion(String entryValue) {
        return replaceVersion(entryValue, true);
    }

    private String replaceVersion(String entryValue, boolean range) {
        if (entryValue.contains(JAKARTA_SERVLET)) {
            StringBuffer builder = new StringBuffer();
            Matcher matcher = SERVLET_PATTERN.matcher(entryValue);
            while (matcher.find()) {
                String version = range ? "[5.0.0,7.0.0)" : "5.0.0";
                matcher.appendReplacement(builder, "jakarta.servlet$1;version=\"" + version + "\"");
            }
            matcher.appendTail(builder);
            return builder.toString();
        }
        return entryValue;
    }
}
