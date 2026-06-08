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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AntHandlerTest {

    private TestTask testTask;

    @Before
    public void setUp() {
        testTask = new TestTask();
    }

    @Test
    public void testPublishSevereLevel() {
        AntHandler handler = new AntHandler(testTask);
        LogRecord record = new LogRecord(Level.SEVERE, "Severe message");
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals((int) Project.MSG_ERR, (int) testTask.logLevels.get(0));
        assertEquals("Severe message", testTask.logMessages.get(0));
    }

    @Test
    public void testPublishWarningLevel() {
        AntHandler handler = new AntHandler(testTask);
        LogRecord record = new LogRecord(Level.WARNING, "Warning message");
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals((int) Project.MSG_WARN, (int) testTask.logLevels.get(0));
        assertEquals("Warning message", testTask.logMessages.get(0));
    }

    @Test
    public void testPublishInfoLevel() {
        AntHandler handler = new AntHandler(testTask);
        LogRecord record = new LogRecord(Level.INFO, "Info message");
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals((int) Project.MSG_INFO, (int) testTask.logLevels.get(0));
        assertEquals("Info message", testTask.logMessages.get(0));
    }

    @Test
    public void testPublishFineLevel() {
        AntHandler handler = new AntHandler(testTask);
        LogRecord record = new LogRecord(Level.FINE, "Fine message");
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals((int) Project.MSG_VERBOSE, (int) testTask.logLevels.get(0));
        assertEquals("Fine message", testTask.logMessages.get(0));
    }

    @Test
    public void testPublishFinerLevel() {
        AntHandler handler = new AntHandler(testTask);
        LogRecord record = new LogRecord(Level.FINER, "Finer message");
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals((int) Project.MSG_DEBUG, (int) testTask.logLevels.get(0));
        assertEquals("Finer message", testTask.logMessages.get(0));
    }

    @Test
    public void testPublishFinestLevel() {
        AntHandler handler = new AntHandler(testTask);
        LogRecord record = new LogRecord(Level.FINEST, "Finest message");
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals((int) Project.MSG_DEBUG, (int) testTask.logLevels.get(0));
        assertEquals("Finest message", testTask.logMessages.get(0));
    }

    @Test
    public void testPublishWithNullMessageAndThrown() {
        AntHandler handler = new AntHandler(testTask);
        RuntimeException exception = new RuntimeException("Test exception");
        LogRecord record = new LogRecord(Level.SEVERE, null);
        record.setThrown(exception);
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertTrue("Message should contain exception info",
                testTask.logMessages.get(0).contains("Test exception"));
        assertNotNull("Thrown should be set", testTask.logThrown.get(0));
    }

    @Test
    public void testPublishWithNullMessage() {
        AntHandler handler = new AntHandler(testTask);
        LogRecord record = new LogRecord(Level.INFO, null);
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals("", testTask.logMessages.get(0));
    }

    @Test
    public void testPublishMultipleMessages() {
        AntHandler handler = new AntHandler(testTask);

        handler.publish(new LogRecord(Level.SEVERE, "Message 1"));
        handler.publish(new LogRecord(Level.WARNING, "Message 2"));
        handler.publish(new LogRecord(Level.INFO, "Message 3"));

        assertEquals(3, testTask.logMessages.size());
        assertEquals("Message 1", testTask.logMessages.get(0));
        assertEquals("Message 2", testTask.logMessages.get(1));
        assertEquals("Message 3", testTask.logMessages.get(2));
    }

    @Test
    public void testFlush() {
        AntHandler handler = new AntHandler(testTask);
        handler.flush();
        assertTrue("Flush should not throw", true);
    }

    @Test
    public void testClose() {
        AntHandler handler = new AntHandler(testTask);
        handler.close();
        assertTrue("Close should not throw", true);
    }

    @Test
    public void testPublishWithExceptionInRecord() {
        AntHandler handler = new AntHandler(testTask);
        IOException exception = new IOException("IO error");
        LogRecord record = new LogRecord(Level.WARNING, "IO warning");
        record.setThrown(exception);
        handler.publish(record);

        assertEquals(1, testTask.logMessages.size());
        assertEquals("IO warning", testTask.logMessages.get(0));
        assertNotNull("Thrown should be set", testTask.logThrown.get(0));
        assertEquals(exception, testTask.logThrown.get(0));
    }

    static class TestTask extends Task {
        List<String> logMessages = new ArrayList<>();
        List<Integer> logLevels = new ArrayList<>();
        List<Throwable> logThrown = new ArrayList<>();

        @Override
        public void log(String message, int level) {
            logMessages.add(message);
            logLevels.add(level);
            logThrown.add(null);
        }

        @Override
        public void log(String message, Throwable throwable, int level) {
            logMessages.add(message);
            logLevels.add(level);
            logThrown.add(throwable);
        }
    }
}
