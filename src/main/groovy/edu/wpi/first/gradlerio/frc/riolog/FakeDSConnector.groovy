package edu.wpi.first.gradlerio.frc.riolog

import groovy.transform.CompileStatic

@CompileStatic
class FakeDSConnector {

    String targetAddress
    SocketAddress udpSocketAddr, tcpSocketAddr
    DatagramSocket udpSocket
    Socket tcpSocket
    Thread udpThread, tcpThread

    FakeDSConnector(String addr) {
        targetAddress = addr
        udpSocket = new DatagramSocket(1150)
        udpSocketAddr = new InetSocketAddress(targetAddress, 1110)

        tcpSocket = new Socket()
        tcpSocketAddr = new InetSocketAddress(targetAddress, 1740)
    }

    void start() {
        udpThread = new Thread({
            ByteArrayOutputStream baos = new ByteArrayOutputStream(6)
            def dos = new DataOutputStream(baos)
            int seq = 0
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.currentThread().sleep(20)
                    seq++
                    baos.reset()
                    dos.writeByte(seq & 0xff)
                    dos.writeByte((seq >> 8) & 0xff)
                    dos.writeByte(0x01)
                    dos.writeByte(0)
                    dos.writeByte(0)
                    dos.writeByte(0)
                    def pack = new DatagramPacket(baos.toByteArray(), 0, 6, udpSocketAddr)
                    udpSocket.send(pack)

                    byte[] temp = new byte[255];
                    def pack2 = new DatagramPacket(temp, 0, 255)
                    udpSocket.receive(pack2)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt()
                }
            }
        })
        udpThread.start()

        tcpThread = new Thread({
            tcpSocket.connect(tcpSocketAddr)
            def dis = new DataInputStream(tcpSocket.inputStream)
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    dis.readUnsignedByte()
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt()
                }
            }
        })
        tcpThread.start()
    }

    void interrupt() {
        udpThread.interrupt()
        tcpThread.interrupt()
        if (tcpSocket.isConnected()) tcpSocket.close()
    }
}
