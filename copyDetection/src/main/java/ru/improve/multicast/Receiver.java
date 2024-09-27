package ru.improve.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;

public class Receiver implements Multicast {

    private String ipGroup;
    private int port;
    private String key;

    private MulticastSocket multicastSocket;

    private boolean continueReceive = true;

    public Receiver(String ipGroup, int port, String key) {
        this.ipGroup = ipGroup;
        this.port = port;
        this.key = key;
    }

    @Override
    public void run() {
        try {
            receiveDatagrams();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void stop() {
        continueReceive = false;
    }

    private void receiveDatagrams() throws IOException {
        byte[] buf = new byte[256];

        SocketAddress socketAddress = new InetSocketAddress(ipGroup, port);
        multicastSocket = new MulticastSocket(port);
        multicastSocket.joinGroup(socketAddress, null);

        while (continueReceive) {
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
            multicastSocket.receive(datagramPacket);

            StringBuilder receiveKey = new StringBuilder();
            receiveKey.append(datagramPacket.getAddress() + " ");
            receiveKey.append(datagramPacket.getPort() + " ");
            receiveKey.append(datagramPacket.getData() + " ");
        }
    }
}
