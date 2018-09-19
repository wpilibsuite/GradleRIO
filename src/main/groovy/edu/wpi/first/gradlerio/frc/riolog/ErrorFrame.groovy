package edu.wpi.first.gradlerio.frc.riolog

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

@CompileStatic
class ErrorFrame implements Frames.Frame {

    @Override
    int tag() {
        return 11
    }

    def getStr(ByteBuffer data) {
        def size = data.getShort() & 0xffff;
        ByteBuffer buf = data.slice();
        buf.limit(size);
        data.position(data.position() + size);
        return StandardCharsets.UTF_8.decode(buf).toString();
    }

    def type(int flags) {
        if ((flags & 1) != 0) return 'ERROR'
        return 'WARN'
    }

    @Override
    void process(ByteBuffer data, int length) {
        data.rewind()
        def timestamp = data.getFloat()
        def seq = data.getShort() & 0xffff
        def numOcc = data.getShort() & 0xffff
        def errorCode = data.getInt()
        def flags = data.get()
        def details = getStr(data)
        def location = getStr(data)
        def callStack = getStr(data)

        println "[${timestamp.round(2)}] ${type(flags)} ${errorCode} ${details} ${location} ${callStack}"
    }
}
