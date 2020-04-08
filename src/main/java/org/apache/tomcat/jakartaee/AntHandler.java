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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * JUL log handler redirecting the messages logged to Ant.
 */
 class AntHandler extends Handler {

    private final Task task;

    public AntHandler(Task task) {
        this.task = task;
    }

    @Override
    public void publish(LogRecord record) {
        task.log(record.getMessage(), record.getThrown(), toAntLevel(record.getLevel()));
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    /**
     * Convert the JUL level to the equivalent Ant one.
     */
    private int toAntLevel(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return Project.MSG_ERR;
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            return Project.MSG_WARN;
        } else if (level.intValue() >= Level.INFO.intValue()) {
            return Project.MSG_INFO;
        } else if (level.intValue() >= Level.FINE.intValue()) {
            return Project.MSG_VERBOSE;
        } else {
            return Project.MSG_DEBUG;
        }
    }
}
