package ru.improve.multicast;

public interface Multicast extends Runnable {

    void initial(String groupIdAddress, int port, String key);

    void stop();
}
