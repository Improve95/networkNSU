package ru.improve.multicast;

public class Receiver implements Multicast {

    private String groupIdAddress;
    private int port;
    private String key;

    private boolean continueReceive = true;

    @Override
    public void initial(String groupIdAddress, int port, String key) {
        this.groupIdAddress = groupIdAddress;
        this.port = port;
        this.key = key;
    }

    @Override
    public void run() {
        receiveDatagrams();
    }

    public void stop() {
        continueReceive = false;
    }

    private void receiveDatagrams() {
        while (continueReceive) {

        }
    }
}
