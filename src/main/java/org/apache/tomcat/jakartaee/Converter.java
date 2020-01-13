package org.apache.tomcat.jakartaee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Converter {

    boolean accpets(String filename);

    void convert(InputStream src, OutputStream dest) throws IOException;
}
