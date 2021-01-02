package org.apache.tomcat.jakartaee.profile;

import org.apache.bcel.classfile.Utility;
import org.apache.tomcat.jakartaee.XmlExclusionListBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class XmlExclusionAwareProfile implements Predicate<String> {
    private final Predicate<String> delegate;
    private final XmlExclusionListBuilder xmlExclusions;

    public XmlExclusionAwareProfile(final Predicate<String> delegate, final XmlExclusionListBuilder xmlExclusions) {
        this.delegate = delegate;
        this.xmlExclusions = xmlExclusions;
    }

    @Override
    public boolean test(final String s) {
        return toClassNames(s).noneMatch(xmlExclusions.getStayInJavax()::contains) && delegate.test(s);
    }

    private Stream<String> toClassNames(final String str) {
        if (str.startsWith("<") && str.endsWith(">")) {
            return Stream.empty();
        }
        try {
            if (str.startsWith("(")) {
                return Stream.of(Utility.methodSignatureArgumentTypes(str, false));
            }
            return Stream.of(Utility.typeSignatureToString(str, false));
        } catch (final RuntimeException re) {
            return Stream.of();
        }
    }
}
