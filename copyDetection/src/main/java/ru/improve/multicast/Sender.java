package ru.improve.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class Sender implements Multicast {

    private final static int TIMER_DELAY = 1000;
    private Timer timer;

    private String ipGroup;
    private int port;
    private String key;

    public Sender(String ipGroup, int port, String key) {
        this.ipGroup = ipGroup;
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
        timer.cancel();
    }

    private void sendDatagrams() throws IOException {
        try(DatagramSocket datagramSocket = new DatagramSocket()) {
            InetAddress inetAddress = InetAddress.getByName(ipGroup);
            byte[] buf = key.getBytes(StandardCharsets.UTF_8);

            timer = new Timer();
            SendDatagramTask sendDatagramTask = new SendDatagramTask(datagramSocket, inetAddress, buf);
            timer.schedule(sendDatagramTask, 0, TIMER_DELAY);
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private class SendDatagramTask extends TimerTask {

        private DatagramSocket datagramSocket;

        private InetAddress ipGroup;
        private byte[] buf;

        public SendDatagramTask(DatagramSocket datagramSocket, InetAddress ipGroup, byte[] buf) {
            this.datagramSocket = datagramSocket;
            this.ipGroup = ipGroup;
            this.buf = buf;
        }

        @Override
        public void run() {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, ipGroup, port);
                datagramSocket.send(datagramPacket);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
