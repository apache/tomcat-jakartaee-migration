#!/bin/sh

mydir="$PWD"
cd `dirname "$0"`
BIN_FOLDER="$PWD"
cd "$mydir"

# Assumes java is on the path
java -cp "$BIN_FOLDER/../lib/*" org.apache.tomcat.jakartaee.MigrationCLI "$@"
