package ru.improve.multicast;

public class CopyDetection {

    private Receiver receiver;
    private Sender sender;

    private Thread receiverThread;
    private Thread senderThread;

    public void start(String groupIp, int port, String key) {
        receiver = new Receiver(groupIp, port, key);
        sender = new Sender(groupIp, port, key);

        receiverThread = new Thread(receiver);
        senderThread = new Thread(sender);

        receiverThread.start();
        senderThread.start();
    }

    public void stop() {
        receiver.stop();
        sender.stop();
        receiverThread.interrupt();
        senderThread.interrupt();
    }
}
