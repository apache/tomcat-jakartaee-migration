# Tomcat Migration Tool for Jakarta EE - Changelog

## 1.0.9
- Update the JaCoCo Maven plugin to 0.8.11. (markt)
- Update Commons BCEL to 6.9.0. (markt)
- Update Commons Compress to 1.26.2. (markt)
- Update Commons IO to 2.16.1. (markt)

## 1.0.8
- Include `.ear` files in list of recognised archives. PR[#50](https://github.com/apache/tomcat-jakartaee-migration/pull/50) provided by Sammy Chu. (markt)
- Update Commons BCEL to 6.8.1. (markt)
- Update Commons Compress to 1.25.0. (markt)
- Update Commons IO to 2.15.1. (markt)
- Update Ant to 1.10.14. (markt)
- Include `.jspf` and `.tagf` files in the conversion process. (markt)


## 1.0.7

- When converting directories, rename files according to the chosen profile. (fschumacher)
- Add configuration option, `matchExcludesAgainstPathName` that can be used to configure exclusions base don path name rather than just file name.  PR[#38](https://github.com/apache/tomcat-jakartaee-migration/pull/38) provided by Réda Housni Alaoui. (markt)
- Update OSGI servlet specification versions if present in manifest file. PR[#42](https://github.com/apache/tomcat-jakartaee-migration/pull/42) provided by Ivan Furnadjiev. (markt)
- Update Commons BCEL to 6.7.0. (markt)
- Update Commons Compress to 1.23.0. (markt)
- Provided workaround for the known JDK bug identified as the cause of migration failures in issue [#46](https://github.com/apache/tomcat-jakartaee-migration/issues/46). (markt/ebourg)


## 1.0.6

- Fix handling of javax.annotation package in 1.0.5. PR [#40](https://github.com/apache/tomcat-jakartaee-migration/pull/40) provided by Danny Thomas (remm)
- Allow parallel use of ClassConverter. PR [#41](https://github.com/apache/tomcat-jakartaee-migration/pull/41) provided by Danny Thomas (remm)

## 1.0.5

- Improve manifest handling to remain the key ordering when unchanged manifests. PR [#36](https://github.com/apache/tomcat-jakartaee-migration/pull/36) provided by Danny Thomas (lihan)
- Improve the performance of conversion by avoiding `JavaClass.dump` when there are no changes. PR [#36](https://github.com/apache/tomcat-jakartaee-migration/pull/36) provided by Danny Thomas (lihan)
- Improve composability of the migration tool when using from other tools. PR [#36](https://github.com/apache/tomcat-jakartaee-migration/pull/36) provided by Danny Thomas (lihan)
- Avoid converting many classes from javax.annotation. PR [#37](https://github.com/apache/tomcat-jakartaee-migration/pull/37) provided by Danny Thomas (remm)
- Update Apache BCEL to 6.6.0. (markt)
- Update Apache Commons Compress 1.22. (markt)

## 1.0.4

- Correct a wrong implementation in the previous fix for [#29](https://github.com/apache/tomcat-jakartaee-migration/issues/29) (lihan)
- Add support for a JEE8 profile that attempts to migrate code using the Jakarta EE APIs to Java EE 8. Note that this will fail if the code uses any APIs added in Jakarta EE 10 onwards. PR #28 provided by blasss. (markt)
- Add Checkstyle to the build process. (markt)

## 1.0.3

- Fix [#32](https://github.com/apache/tomcat-jakartaee-migration/issues/32) handle conversion of manifests in exploded JARs. PR by wmccusker/ (markt)
- Fix unexpected generation of bz2 source archive in Maven distribution

## 1.0.2 (not released)

- Fix [#29](https://github.com/apache/tomcat-jakartaee-migration/issues/29) by recalculating the CRC value of the entry type is SORTED after converting (lihan)
- Update Apache Parent to 27. (markt)
- Update Maven Assembly plugin to 3.4.2. (markt)
- Update Maven Source plugin to 3.2.1. (markt)

## 1.0.1

- Fix [#19](https://github.com/apache/tomcat-jakartaee-migration/issues/19). Add support for converting `.groovy` files.
- Fix [#20](https://github.com/apache/tomcat-jakartaee-migration/issues/20) by using commons-compression instead of the Java zip code (remm)
- Remove deprecated `-verbose` command line option (remm)
- Fix [bug 66163](https://bz.apache.org/bugzilla/show_bug.cgi?id=66163). Correct the handling of references of the form `jakarta. ...` when using the class transformer when those references are to classes not provided by the container. Based on a patch by Ole Schulz-Hildebrandt. (markt)
- Update Apache Ant to 1.10.12. (markt)
- Update Apache Commons Compress to 1.21. (markt)
- Update Apache Commons IO to 2.11.0. (markt)
- Update Apache Parent to 26. (markt)
- Update JUnit to 4.13.2. (markt)
- Update Maven AntRun plugin to 3.1.0. (markt)
- Update Maven Assembly plugin to 3.4.0. (markt)

## 1.0.0

- Fix [#14](https://github.com/apache/tomcat-jakartaee-migration/issues/14). Do not migrate `javax.xml.(registry|rpc)` namespaces. (mgrigorov)
- The class transformer will now validate that the target classes in the Jakarta namespace exist in the runtime environment (remm)

## 0.2.0

- Add this changelog (markt)
- Update dependencies (Apache Commons IO 2.8.0, Apache Ant 1.10.9) (markt)
- Fix [#9](https://github.com/apache/tomcat-jakartaee-migration/issues/9). Exclude the `javax.xml.namespace` package in the EE profile (ebourg)
- Include the `javax.management.j2ee` package in the EE profile (ebourg)
- Add a test to confirm `javax.xml.xpath.XPathConstants` is not converted (ebourg)
- Update README to mention the tool is now available on Debian/Ubuntu (ebourg)
- Include the `javax.security.enterprise` package in the EE profile (ebourg)
- Include the `javax.xml.registry` package in the EE profile (ebourg)
- Include the `javax.security.jacc` package in the EE profile (ebourg)
- Include the `javax.faces` package in the EE profile (ebourg)
- Include the `javax.batch` package in the EE profile (ebourg)
- Include the `javax.jws` package in the EE profile (ebourg)
- Include the `javax.resource` package in the EE profile (ebourg)
- Fix [#7](https://github.com/apache/tomcat-jakartaee-migration/issues/7). Include the `javax.jms` package in the EE profile (alitokmen/mgirgorov)
- Make `migrate.sh` work from any path (mgrigorov)
- Add a new option `-zipInMemory` that processes archives (ZIP, JAR, WAR, etc.) in memory rather via a streaming approach. While less efficient, it allows archives to be processed when their structure means that a streaming approach will fail. (markt)
- Include the Maven Wrapper source files in the source distribution. (markt)
- Ensure that all the Manifest attributes are processed during the migration process. (markt)
- Include `.properties` and `.json` files in the conversion process. (markt)
- Replace `-verbose` with `-logLevel=` to provide more control over logging level. (markt)
- Fix [#13](https://github.com/apache/tomcat-jakartaee-migration/issues/13). Refactor mapping of log messages to log levels. (markt)
- Fix [#3](https://github.com/apache/tomcat-jakartaee-migration/issues/3). Add support for excluding files from conversion. (markt)
- Fix handling of classes with more than 32768 entries in the constant pool. (markt)
- Exclude `javax.xml.stream` and `javax.xml.XMLConstants` from the EE profile. (markt)
- Relocate dependencies under the `org.apache.tomcat.jakartaee` package to avoid clashes when integrating the shaded jar. (markt)

## 0.1.0

- Initial release (markt)
