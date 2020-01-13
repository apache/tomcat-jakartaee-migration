package org.apache.tomcat.jakartaee;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TextConverter implements Converter {

    private static final List<String> supportedExtensions;

    static {
        supportedExtensions = new ArrayList<>();
        supportedExtensions.add("jsp");
        supportedExtensions.add("jspx");
        supportedExtensions.add("tag");
        supportedExtensions.add("tagx");
        supportedExtensions.add("tld");
        supportedExtensions.add("txt");
        supportedExtensions.add("xml");
    }


    @Override
    public boolean accpets(String filename) {
        String extension = Util.getExtension(filename);
        if (extension == null || extension.length() == 0) {
            return false;
        }

        if (supportedExtensions.contains(extension)) {
            return true;
        }

        return false;
    }


    /*
     * This is a bit of a hack so the same Pattern can be used for text files as
     * for Strings in class files. An approach that worked directly on the
     * streams would be more efficient - but also require significantly more
     * code. Since the conversion process is intended to be a one-time process,
     * this implementation opts for simplicity of code over efficiency of
     * execution.
     */
    @Override
    public void convert(InputStream src, OutputStream dest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        flow(src, baos);

        String srcString = new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);

        String destString = Util.convert(srcString);

        ByteArrayInputStream bais = new ByteArrayInputStream(destString.getBytes(StandardCharsets.ISO_8859_1));
        flow (bais, dest);
    }


    private static void flow(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[8192];
        int numRead;
        while ( (numRead = is.read(buf) ) >= 0) {
            if (os != null) {
                os.write(buf, 0, numRead);
            }
        }
    }
}
