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

/**
 * Updates Manifests.
 */
public class ManifestConverter implements Converter {

    private static final Logger logger = Logger.getLogger(ManifestConverter.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(ManifestConverter.class);

    @Override
    public boolean accepts(String filename) {
        if (filename.equals(JarFile.MANIFEST_NAME) || filename.endsWith("/" + JarFile.MANIFEST_NAME)) {
            return true;
        }

        // Reject everything else
        return false;
    }

    @Override
    public void convert(String path, InputStream src, OutputStream dest, EESpecProfile profile) throws IOException {
        Manifest srcManifest = new Manifest(src);
        Manifest destManifest = new Manifest(srcManifest);

        boolean result = false;

        result = result | removeSignatures(destManifest);
        result = result | updateValues(destManifest, profile);

        if (result) {
            destManifest.write(dest);
        } else {
            srcManifest.write(dest);
        }
    }


    private boolean removeSignatures(Manifest manifest) {
        boolean removedSignatures = manifest.getMainAttributes().remove(Attributes.Name.SIGNATURE_VERSION) != null;
        List<String> signatureEntries = new ArrayList<>();
        Map<String, Attributes> manifestAttributeEntries = manifest.getEntries();
        for (Entry<String, Attributes> entry : manifestAttributeEntries.entrySet()) {
            if (isCryptoSignatureEntry(entry.getValue())) {
                String entryName = entry.getKey();
                signatureEntries.add(entryName);
                logger.log(Level.FINE, sm.getString("migration.removeSignature", entryName));
                removedSignatures = true;
            }
        }

        for (String entry : signatureEntries) {
            manifestAttributeEntries.remove(entry);
        }

        return removedSignatures;
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
        boolean result = false;
        result = result | updateValues(manifest.getMainAttributes(), profile);
        for (Attributes attributes : manifest.getEntries().values()) {
            result = result | updateValues(attributes, profile);
        }
        return result;
    }


    private boolean updateValues(Attributes attributes, EESpecProfile profile) {
        boolean result = false;
        // Update version info
        if (attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
            String newValue = attributes.get(Attributes.Name.IMPLEMENTATION_VERSION) + "-" + Info.getVersion();
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, newValue);
            result = true;
        }
        // Update package names in values
        for (Entry<Object,Object> entry : attributes.entrySet()) {
            String newValue = profile.convert((String) entry.getValue());
            // Object comparison is deliberate
            if (newValue != entry.getValue()) {
                entry.setValue(newValue);
                result = true;
            }
        }
        return result;
    }
}
