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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MigrationCLI {

    private static final StringManager sm = StringManager.getManager(MigrationCLI.class);

    private static final String EXCLUDE_ARG = "-exclude=";
    private static final String LOGLEVEL_ARG = "-logLevel=";
    private static final String PROFILE_ARG = "-profile=";
    private static final String ZIPINMEMORY_ARG = "-zipInMemory";

    public static void main(String[] args) throws IOException {

        // Defaults
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
        Migration migration = new Migration();

        // Process argumnets
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        // Process the custom log level if present
        // Use an iterator so we can remove the log level argument if found
        Iterator<String> iter = arguments.iterator();
        while (iter.hasNext()) {
            String argument = iter.next();
            if (argument.startsWith(EXCLUDE_ARG)) {
                iter.remove();
                String exclude = argument.substring(EXCLUDE_ARG.length());
                migration.addExclude(exclude);
            } else if (argument.startsWith(LOGLEVEL_ARG)) {
                iter.remove();
                String logLevelName = argument.substring(LOGLEVEL_ARG.length());
                Level level = null;
                try {
                    level = Level.parse(logLevelName.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException iae) {
                    invalidArguments();
                }
                // Configure the explicit level
                Logger.getGlobal().getParent().getHandlers()[0].setLevel(level);
                Logger.getGlobal().getParent().setLevel(level);
            } else if (argument.startsWith(PROFILE_ARG)) {
                iter.remove();
                String profileName = argument.substring(PROFILE_ARG.length());
                try {
                    EESpecProfile profile = EESpecProfiles.valueOf(profileName.toUpperCase(Locale.ENGLISH));
                    migration.setEESpecProfile(profile);
                } catch (IllegalArgumentException e) {
                    // Invalid profile value
                    invalidArguments();
                }
            } else if (argument.equals(ZIPINMEMORY_ARG)) {
                iter.remove();
                migration.setZipInMemory(true);
            }
        }

        if (arguments.size() != 2) {
            invalidArguments();
        }

        String source = arguments.get(0);
        String dest = arguments.get(1);

        migration.setSource(new File(source));
        migration.setDestination(new File(dest));

        migration.execute();
    }

    private static void invalidArguments() {
        System.out.println(sm.getString("migration.usage"));
        System.exit(1);
    }
}
