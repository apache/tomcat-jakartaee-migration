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
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant task for the Jakarta EE migration tool.
 */
public class MigrationTask extends Task {

    private File src;
    private File dest;
    private String profile = EESpecProfile.TOMCAT.toString();
    private boolean zipInMemory = false;

    public void setSrc(File src) {
        this.src = src;
    }

    public void setDest(File dest) {
        this.dest = dest;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setZipInMemory(boolean zipInMemory) {
        this.zipInMemory = zipInMemory;
    }

    @Override
    public void execute() throws BuildException {
        // redirect the log messages to Ant
        Logger logger = Logger.getLogger(Migration.class.getCanonicalName());
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        logger.addHandler(new AntHandler(this));

        // check the parameters
        EESpecProfile profile = null;
        try {
            profile = EESpecProfile.valueOf(this.profile.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BuildException("Invalid profile specified: " + this.profile, getLocation()); // todo i18n
        }

        Migration migration = new Migration();
        migration.setSource(src);
        migration.setDestination(dest);
        migration.setEESpecProfile(profile);
        migration.setZipInMemory(zipInMemory);

        try {
            migration.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }
}
