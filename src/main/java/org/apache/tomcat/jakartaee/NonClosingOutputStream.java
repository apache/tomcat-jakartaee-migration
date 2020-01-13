package org.apache.tomcat.jakartaee;

import java.io.IOException;
import java.io.OutputStream;

public class NonClosingOutputStream extends OutputStream {

    private OutputStream wrapped;


    public NonClosingOutputStream(OutputStream wrapped) {
        this.wrapped = wrapped;
    }


    @Override
    public void write(int b) throws IOException {
        wrapped.write(b);
    }


    @Override
    public void write(byte[] b) throws IOException {
        wrapped.write(b);
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrapped.write(b, off, len);
    }


    @Override
    public void flush() throws IOException {
        wrapped.flush();
    }


    @Override
    public void close() throws IOException {
        // NO-OP
    }
}
