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
        if (JarFile.MANIFEST_NAME.equals(filename)) {
            return true;
        }

        // Reject everything else
        return false;
    }

    @Override
    public void convert(InputStream src, OutputStream dest, EESpecProfile profile) throws IOException {
        Manifest srcManifest = new Manifest(src);
        Manifest destManifest = new Manifest(srcManifest);

        removeSignatures(destManifest);
        updateVersion(destManifest);
        updateValues(destManifest, profile);

        destManifest.write(dest);
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


    private void updateValues(Manifest manifest, EESpecProfile profile) {
        updateValues(manifest.getMainAttributes(), profile);
        for (Attributes attributes : manifest.getEntries().values()) {
            updateValues(attributes, profile);
        }
    }


    private void updateValues(Attributes attributes, EESpecProfile profile) {
        for (Entry<Object,Object> entry : attributes.entrySet()) {
            entry.setValue(profile.convert((String) entry.getValue()));
        }
        if (attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
            String newValue = attributes.get(Attributes.Name.IMPLEMENTATION_VERSION) + "-" + Info.getVersion();
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, newValue);
        }
    }
}
