package ru.improve.multicast;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class Receiver implements Multicast {

    private final static int BUFFER_SIZE = 256;
    private final static int TIMER_TIMEOUT = 2000;
    private final static int COPY_TIMEOUT = 10 * 1000;

    private String ipGroup;
    private int port;
    private String key;

    Timer timer;
    Map<ReceiveData, Long> copyList = new ConcurrentHashMap<>();

    MulticastSocket multicastSocket;

    private boolean continueReceive = true;

    public Receiver(String ipGroup, int port, String key) {
        this.ipGroup = ipGroup;
        this.port = port;
        this.key = key;
    }

    @Override
    public void run() {
        timer = new Timer();
        CheckCopyTimeoutTask checkCopyTimeoutTask = new CheckCopyTimeoutTask();
        timer.scheduleAtFixedRate(checkCopyTimeoutTask, 0, TIMER_TIMEOUT);

        receiveDatagrams();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
        continueReceive = false;
        multicastSocket.close();
    }

    private void receiveDatagrams() {
        try {
            byte[] buf = new byte[BUFFER_SIZE];

            SocketAddress socketAddress = new InetSocketAddress(ipGroup, port);
            multicastSocket = new MulticastSocket(port);
            multicastSocket.joinGroup(socketAddress, null);

            System.out.println("Initial on " + socketAddress);

            while (continueReceive) {
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(datagramPacket);

                ReceiveData receiveData = new ReceiveData(datagramPacket.getAddress(), datagramPacket.getPort(), datagramPacket.getData());

                if (copyList.containsKey(receiveData)) {
                    copyList.replace(receiveData, System.currentTimeMillis());
                } else {
                    copyList.put(receiveData, System.currentTimeMillis());
                }
            }
        } catch (SocketException ex) {

        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private class CheckCopyTimeoutTask extends TimerTask {
        @Override
        public void run() {
            for (var entry : copyList.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() > COPY_TIMEOUT) {
                    copyList.remove(entry.getKey());
                }
            }

            for (var entry : copyList.entrySet()) {
                ReceiveData receiveData = entry.getKey();
                System.out.println(String.format("%s %d", receiveData.getInetAddress(), receiveData.getPort()));
            }
        }
    }

    @Data
    @AllArgsConstructor
    private class ReceiveData {

        private InetAddress inetAddress;

        private int port;

        private byte[] data;
    }
}
