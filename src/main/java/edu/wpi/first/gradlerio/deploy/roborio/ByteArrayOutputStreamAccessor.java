package edu.wpi.first.gradlerio.deploy.roborio;

import java.io.ByteArrayOutputStream;

public class ByteArrayOutputStreamAccessor extends ByteArrayOutputStream {
    public byte[] getBackingArray() {
        return this.buf;
    }

    public int getBackingLength() {
        return this.count;
    }
}
