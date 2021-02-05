# Tomcat Migration Tool for Jakarta EE - Changelog

## 0.1.1 (in progress)

- Add this changelog (markt)
- Update dependencies (Apache Commons IO 2.8.0, Apache Ant 1.10.9) (markt)
- Fix #9. Exclude the `javax.xml.namespace` package in the EE profile (ebourg)
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
- Fix #7. Include the `javax.jms` package in the EE profile (alitokmen/mgirgorov)
- Make `migrate.sh` work from any path (mgrigorov)

## 0.1.0

- Initial release (markt)