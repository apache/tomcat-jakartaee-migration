# Apache Tomcat migration tool for Jakarta EE


[![Build Status](https://secure.travis-ci.org/apache/tomcat-jakartaee-migration.svg)](http://travis-ci.org/apache/tomcat-jakartaee-migration)
[![Coverage Status](https://coveralls.io/repos/github/apache/tomcat-jakartaee-migration/badge.svg?branch=master)](https://coveralls.io/github/apache/tomcat-jakartaee-migration?branch=master)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Overview

This tool is a work in progress.

The aim of the tool is to take a web application written for Java EE 8 that
runs on Apache Tomcat 9 and convert it automatically so it runs on Apache
Tomcat 10 which implements Jakarta EE 9.

The tool can be used from the command line or as an Ant task.

## Usage

### Build

Build the migration tool with:

    ./mvnw verify

### Migrate

Migrate your Servlet application with:

    java -jar target/jakartaee-migration-*-shaded.jar <source> <destination>

The source should be a path to a compressed archive, a folder or an individual
file. The destination will be created at the specified path as a resource of
the same type as the source.

> **INFO**
> This tool will remove cryptographic signatures from JAR files contained
> in the *source*, as the changed resources would not match them anymore.
>
> A warning will be logged for each JAR file where the signature has been removed.

This tool is also available on Debian and Ubuntu systems by installing the
[tomcat-jakartaee-migration](https://tracker.debian.org/tomcat-jakartaee-migration)
package and invoking the `javax2jakarta` command.

## Ant task

The migration tool is available as an Ant task, here is an example:

    <taskdef name="javax2jakarta" classname="org.apache.tomcat.jakartaee.MigrationTask" classpath="jakartaee-migration-*-shaded.jar"/>
    
    <javax2jakarta src="webapp.war" dest="webapp.migrated.war" profile="tomcat"/>

## Differences between Java EE 8 and Jakarta EE 9

The differences between Java EE 8 and Jakarta EE 9 are limited to
(some packages)[https://github.com/apache/tomcat-jakartaee-migration/blob/master/src/main/java/org/apache/tomcat/jakartaee/EESpecProfile.java]
moving from the `javax.*` namespace to the `jakarta.*` namespace. Some packages
have also been renamed. This migration tool performs all the package renaming
necessary to migrate an application from Java EE 8 to Jakarta EE 9.

Note: It will not be necessary to migrate any references to XML schemas. The
schemas don't directly reference `javax` packages and Jakarta EE 9 will continue
to support the use of schemas from Java EE 8 and earlier.
