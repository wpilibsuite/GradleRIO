package edu.wpi.first.gradlerio.frc.riolog

import groovy.transform.CompileStatic

import java.nio.ByteBuffer

@CompileStatic
class Frames {

    @CompileStatic
    static interface Frame {
        int tag()
        void process(ByteBuffer data, int length)
    }

    static def frames = [ new ErrorFrame(), new InfoFrame() ]

    static Frame frame(int tag) {
        def matches = frames.findAll { Frame f -> f.tag() == tag }
        if (matches.empty) return null
        return matches.first()
    }
}
