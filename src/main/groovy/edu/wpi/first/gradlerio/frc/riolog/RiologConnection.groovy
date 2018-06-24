package edu.wpi.first.gradlerio.frc.riolog

import groovy.transform.CompileStatic

import java.nio.ByteBuffer

@CompileStatic
class RiologConnection {

    Socket socket
    DataInputStream instream
    String targetAddress
    Thread keepaliveThread, readThread
    boolean connected = false

    RiologConnection(String addr) {
        targetAddress = addr
    }

    void start() {
        keepaliveThread = new Thread({
            reconnect()
            while (!Thread.interrupted()) {
                try {
                    Thread.currentThread().sleep(2000)
                    socket.outputStream.write([0, 0] as byte[])
                    socket.outputStream.flush()
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt()
                } catch (IOException e) {
                    println("Connection lost! Attempting reconnection!")
                    reconnect()
                }
            }
        }, 'RiologKeepalive')
        keepaliveThread.start()

        startRead()
    }

    void startRead() {
        def segdata = ByteBuffer.allocate(65536)
        readThread = new Thread({
            while (!Thread.currentThread().interrupted()) {
                if (!keepaliveThread.isAlive()) break
                if (connected) {
                    try {
                        def len = instream.readUnsignedShort()
                        def tag = instream.readUnsignedByte()
                        len -= 1

                        segdata.clear()
                        segdata.limit(len)

                        def data = segdata.array()
                        def bytesRead = 0
                        while(bytesRead < len) {
                            def nread = instream.read(data, bytesRead, len-bytesRead)
                            bytesRead += nread
                        }

                        def frame = Frames.frame(tag)
                        if (frame == null) {
                            println " -> ERR: Unknown Tag ${tag}. Ignoring... "
                        } else {
                            frame.process(segdata, len)
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt()
                    } catch (SocketTimeoutException e) { }       // Keepalive will handle it
                } else {
                    Thread.currentThread().sleep(500)       // I could use a condition variable, but not really worth
                }
            }
        }, 'RiologRead')
        readThread.start()
    }

    void reconnect() {
        connected = false
        while (!Thread.currentThread().interrupted()) {
            try {
                socket = new Socket()
                socket.connect(new InetSocketAddress(targetAddress, 1741), 3000)
                socket.setTcpNoDelay(true)
                println "Connected!"
                instream = new DataInputStream(socket.inputStream)
                connected = true
                break
            } catch (ConnectException e) {
                println "Could not connect! Retrying..."
                println e
                Thread.currentThread().sleep(3000)
            }
        }
    }

    void join() {
        readThread.join()
    }

    void interrupt() {
        readThread.interrupt()
        keepaliveThread.interrupt()
    }
}
