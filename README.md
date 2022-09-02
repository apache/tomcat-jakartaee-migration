# Apache Tomcat migration tool for Jakarta EE


[![Coverage Status](https://codecov.io/gh/apache/tomcat-jakartaee-migration/branch/main/graph/badge.svg)](https://app.codecov.io/gh/apache/tomcat-jakartaee-migration/branch/main)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Overview

The purpose of the tool is to take a web application written for Java EE 8 that
runs on Apache Tomcat 9 and convert it automatically so it runs on Apache
Tomcat 10 which implements Jakarta EE 9.

The tool can be used from the command line or as an Ant task.

## Usage

### Download

Download a source or binary distribution from
[https://tomcat.apache.org/download-migration.cgi](https://tomcat.apache.org/download-migration.cgi)

### Build

Build the migration tool from source with:

    ./mvnw verify

To run the migration tool locally, you are most likely to want:

    target/jakartaee-migration-*-shaded.jar

### Migrate

Migrate your Servlet application with:

    java -jar jakartaee-migration-*-shaded.jar <source> <destination>

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

The difference between Java EE 8 and Jakarta EE 9 is that all the
[Java EE 8 packages](https://github.com/apache/tomcat-jakartaee-migration/blob/main/src/main/java/org/apache/tomcat/jakartaee/EESpecProfile.java#L37)
in the `javax.*` namespace have moved to the `jakarta.*` namespace.
Some sub-packages have also been renamed. 
This migration tool performs all the necessary changes to migrate an application
from Java EE 8 to Jakarta EE 9 by renaming each Java EE 8 package to its Jakarta
EE 9 replacement. This includes package references in classes, String constants,
configuration files, JSPs, TLDs etc.

Note: Not all `javax.*` packages are part of Java EE. Only those defined by Java
EE have moved to the `jakarta.*` namespace.

Note: It is not necessary to migrate any references to XML schemas. The schemas
don't directly reference javax packages and Jakarta EE 9 will continue to
support the use of schemas from Java EE 8 and earlier.

## Legal

This tool modifies web application content as described in the previous section.
This may include modification of third-party provided content. It is strongly
recommended that you confirm that the license(s) associated with any third-party
content permit such modifications, especially if you intend to distribute the
result.