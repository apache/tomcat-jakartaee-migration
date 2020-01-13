#!/bin/sh

# Assumes current layout of Maven's target directory
cd ..

# Assumes java is on the path
java -cp lib/* org.apache.tomcat.jakartaee.Migration "$@"
