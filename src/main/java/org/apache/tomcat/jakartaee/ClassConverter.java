package org.apache.tomcat.jakartaee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;

public class ClassConverter implements Converter {

    @Override
    public boolean accpets(String filename) {
        String extension = Util.getExtension(filename);
        if (extension == null || extension.length() == 0) {
            return false;
        }

        if ("class".equals(extension)) {
            return true;
        }

        return false;
    }


    @Override
    public void convert(InputStream src, OutputStream dest) throws IOException {

        ClassParser parser = new ClassParser(src, "unknown");
        JavaClass javaClass = parser.parse();

        // Loop through constant pool
        Constant[] constantPool = javaClass.getConstantPool().getConstantPool();
        for (short i = 0; i < constantPool.length; i++) {
            if (constantPool[i] instanceof ConstantUtf8) {
                ConstantUtf8 c = (ConstantUtf8) constantPool[i];
                String str = c.getBytes();
                c = new ConstantUtf8(Util.convert(str));
                constantPool[i] = c;
            }
        }

        javaClass.dump(dest);
    }
}
