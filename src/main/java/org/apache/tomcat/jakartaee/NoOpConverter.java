package org.apache.tomcat.jakartaee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NoOpConverter implements Converter {

    @Override
    public boolean accpets(String filename) {
        // Accepts everything
        return true;
    }

    @Override
    public void convert(InputStream src, OutputStream dest) throws IOException {
        // This simply copies the source to the destination
        byte[] buf = new byte[8192];
        int numRead;
        while ((numRead = src.read(buf)) >= 0) {
            dest.write(buf, 0, numRead);
        }
    }
}
