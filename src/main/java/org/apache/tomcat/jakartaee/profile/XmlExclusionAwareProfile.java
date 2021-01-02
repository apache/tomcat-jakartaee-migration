package org.apache.tomcat.jakartaee.profile;

import org.apache.bcel.classfile.Utility;
import org.apache.tomcat.jakartaee.XmlExclusionListBuilder;

import java.util.function.Predicate;

public class XmlExclusionAwareProfile implements Predicate<String> {
    private final Predicate<String> delegate;
    private final XmlExclusionListBuilder xmlExclusions;

    public XmlExclusionAwareProfile(final Predicate<String> delegate, final XmlExclusionListBuilder xmlExclusions) {
        this.delegate = delegate;
        this.xmlExclusions = xmlExclusions;
    }

    @Override
    public boolean test(final String s) {
        return !xmlExclusions.getStayInJavax().contains(toClassName(s)) && delegate.test(s);
    }

    private String toClassName(String str) {
        if (str.startsWith("<") && str.endsWith(">")) {
            return str;
        }
        try {
            return Utility.typeSignatureToString(str, false);
        } catch (final RuntimeException re) {
            return ""; // will never match
        }
    }
}
