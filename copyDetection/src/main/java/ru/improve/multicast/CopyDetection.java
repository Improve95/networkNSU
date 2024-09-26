package ru.improve.multicast;

public class CopyDetection {

    private Receiver receiver;
    private Sender sender;

    private Thread receiverThread;
    private Thread senderThread;

    public CopyDetection() {
        receiver = new Receiver();
        sender = new Sender();
    }

    public void start(String groupIp, int port, String key) {
        receiver.initial(groupIp, port, key);
        receiver.initial(groupIp, port, key);
        receiverThread = new Thread(() -> receiver.run());
        senderThread = new Thread(() -> sender.run());
    }

    public void stop() {
        receiver.stop();
        sender.stop();
        receiverThread.interrupt();
        senderThread.interrupt();
    }
}
