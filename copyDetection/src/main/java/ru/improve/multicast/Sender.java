package ru.improve.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender implements Multicast {

    private String groupIdAddress;
    private int port;
    private String key;

    private DatagramSocket datagramSocket;

    private boolean continueSend = true;

    @Override
    public void initial(String groupIdAddress, int port, String key) {
        this.groupIdAddress = groupIdAddress;
        this.port = port;
        this.key = key;
    }

    @Override
    public void run() {
        try {
            sendDatagrams();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void stop() {
        continueSend = false;
        datagramSocket.close();
    }

    private void sendDatagrams() throws IOException {
        InetAddress ipGroup = InetAddress.getByName(groupIdAddress);
        datagramSocket = new DatagramSocket();
        byte[] buf = key.getBytes();

        while (continueSend) {
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, ipGroup, port);
            datagramSocket.send(datagramPacket);
        }
    }
}
