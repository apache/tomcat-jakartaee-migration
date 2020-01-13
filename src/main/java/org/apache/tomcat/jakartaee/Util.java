package org.apache.tomcat.jakartaee;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static Pattern PATTERN = Pattern.compile(
            "javax([/\\.](annotation|ejb|el|mail|persistence|security[/\\.]auth[/\\.]message|servlet|transaction|websocket))");

    public static String getExtension(String filename) {
        // Extract the extension
        int lastPeriod = filename.lastIndexOf(".");
        if (lastPeriod == -1) {
            return null;
        }
        return filename.substring(lastPeriod + 1).toLowerCase(Locale.ENGLISH);
    }


    public static String convert(String name) {
        Matcher m = PATTERN.matcher(name);
        return m.replaceAll("jakarta$1");
    }


    private Util() {
        // Hide default constructor. Utility class.
    }
}
