#!/bin/sh

# Assumes java is on the path
java -cp "../lib/*" org.apache.tomcat.jakartaee.MigrationCLI "$@"
