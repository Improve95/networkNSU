package ru.improve.server;

import ru.improve.model.ClientData;
import ru.improve.model.ServerModel;

import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class SpeedTest implements Runnable {

    private int speedTestDelay = 3 * 1000;
    private Timer timer;

    private ServerModel serverModel;

    public SpeedTest(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    @Override
    public void run() {
        timer = new Timer();
        TestSpeed testSpeed = new TestSpeed();
        timer.scheduleAtFixedRate(testSpeed, 0, speedTestDelay);
    }

    public void stop() {
        timer.cancel();
    }

    public static void printSpeed(SocketChannel socketChannel, ClientData clientData) {
        InetAddress inetAddress = socketChannel.socket().getInetAddress();
        float speed = clientData.getReadSpeed();
        speed /= 1024 * 1024;
        System.out.println("Client: " + inetAddress + " - speed: " + speed + " Mb/s");
    }

    private class TestSpeed extends TimerTask {
        @Override
        public void run() {
            Map<SocketChannel, ClientData> clients = new HashMap<>(serverModel.getClients());
            clients.entrySet()
                    .stream()
                    .forEach(entry -> {
                        SocketChannel socketChannel = entry.getKey();
                        ClientData clientData = entry.getValue();
                        printSpeed(socketChannel, clientData);
                    });
        }
    }
}
