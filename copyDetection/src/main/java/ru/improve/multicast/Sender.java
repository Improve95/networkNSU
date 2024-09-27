package ru.improve.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

public class Sender implements Multicast {

    private final static int timerDelay = 2000;

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

        Timer timer = new Timer();
        timer.schedule(new MyTask(ipGroup, buf), 0, timerDelay);
    }

    class MyTask extends TimerTask {

        private InetAddress ipGroup;

        private byte[] buf;

        public MyTask(InetAddress ipGroup, byte[] buf) {
            this.ipGroup = ipGroup;
            this.buf = buf;
        }

        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, ipGroup, port);
                datagramSocket.send(datagramPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
