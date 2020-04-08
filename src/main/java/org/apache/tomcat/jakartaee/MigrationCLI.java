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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MigrationCLI {

    private static final Logger logger = Logger.getLogger(MigrationCLI.class.getCanonicalName());
    private static final StringManager sm = StringManager.getManager(MigrationCLI.class);

    private static final String PROFILE_ARG = "-profile=";

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");

        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        if (arguments.contains("-verbose")) {
            Logger.getGlobal().getParent().getHandlers()[0].setLevel(Level.FINE);
            Logger.getGlobal().getParent().setLevel(Level.FINE);
            arguments.remove("-verbose");
        }

        Migration migration = new Migration();

        boolean valid = false;
        String source = null;
        String dest = null;
        if (arguments.size() == 3) {
            if (arguments.get(0).startsWith(PROFILE_ARG)) {
                source = arguments.get(1);
                dest = arguments.get(2);
                valid = true;
                try {
                    migration.setEESpecProfile(EESpecProfile.valueOf(arguments.get(0).substring(PROFILE_ARG.length())));
                } catch (IllegalArgumentException e) {
                    // Invalid profile value
                    valid = false;
                }
            }
        }
        if (arguments.size() == 2) {
            source = arguments.get(0);
            dest = arguments.get(1);
            valid = true;
        }
        if (!valid) {
            usage();
            System.exit(1);
        }

        migration.setSource(new File(source));
        migration.setDestination(new File(dest));
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
    
}
