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

    /**
     * Default constructor.
     */
    public MigrationTask() {}

    private File src;
    private File dest;
    private String profile = EESpecProfiles.TOMCAT.toString();
    private boolean zipInMemory = false;
    private String excludes;
    private boolean matchExcludesAgainstPathName;

    /**
     * Set the source file.
     * @param src the source file
     */
    public void setSrc(File src) {
        this.src = src;
    }

    /**
     * Set the destination file.
     * @param dest the destination file
     */
    public void setDest(File dest) {
        this.dest = dest;
    }

    /**
     * Set the profile that should be used.
     * @param profile the profile to be used
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Set the option to handle compressed archive entries in memory.
     * @param zipInMemory true to buffer in memory
     */
    public void setZipInMemory(boolean zipInMemory) {
        this.zipInMemory = zipInMemory;
    }

    /**
     * Set exclusion patterns.
     *
     * @param excludes  Comma separated, case sensitive list of glob patterns
     *                  for files to exclude
     */
    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    /**
     * Enable exclude matching against the path name.
     * @param matchExcludesAgainstPathName true to match excludes against the path name instead of the file name
     */
    public void setMatchExcludesAgainstPathName(boolean matchExcludesAgainstPathName) {
        this.matchExcludesAgainstPathName = matchExcludesAgainstPathName;
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
            profile = EESpecProfiles.valueOf(this.profile.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BuildException("Invalid profile specified: " + this.profile, getLocation()); // todo i18n
        }

        Migration migration = new Migration();
        migration.setSource(src);
        migration.setDestination(dest);
        migration.setEESpecProfile(profile);
        migration.setZipInMemory(zipInMemory);
        migration.setMatchExcludesAgainstPathName(matchExcludesAgainstPathName);
        if (this.excludes != null) {
            String[] excludes= this.excludes.split(",");
            for (String exclude : excludes) {
                migration.addExclude(exclude);
            }
        }

        try {
            migration.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }
}
