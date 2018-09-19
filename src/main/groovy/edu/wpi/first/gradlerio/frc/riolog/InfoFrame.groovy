package edu.wpi.first.gradlerio.frc.riolog

import groovy.transform.CompileStatic

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

@CompileStatic
class InfoFrame implements Frames.Frame {

    @Override
    int tag() {
        return 12
    }

    @Override
    void process(ByteBuffer data, int length) {
        data.rewind()
        def timestamp = data.getFloat()
        def seq = data.getShort() & 0xffff
        def line = StandardCharsets.UTF_8.decode(data).toString();

        println "[${timestamp.round(2)}] ${line}"
    }
}
