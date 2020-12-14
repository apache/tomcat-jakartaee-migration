#!/bin/sh

BIN_FOLDER=`dirname "$0"`

# Assumes java is on the path
java -cp "$BIN_FOLDER/../lib/*" org.apache.tomcat.jakartaee.MigrationCLI "$@"
