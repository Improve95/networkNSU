package ru.improve.communication.sender;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferSender implements Runnable {

    private FileSender fileSender;

    private SocketChannel socketChannel;
    private ConcurrentLinkedQueue<ByteBuffer> buffersQueue = new ConcurrentLinkedQueue<>();

    private boolean continueSend = true;
    public BufferSender(FileSender fileSender, SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.fileSender = fileSender;
    }

    @Override
    public void run() {
        try {
            send();
        } catch (InterruptedException | IOException ex) {
            stop();
            throw new RuntimeException(ex.getMessage());
        }
    }

    public synchronized void stop() {
        continueSend = false;
        notifyAll();
    }

    public synchronized void addBuffer(ByteBuffer byteBuffer) {
        buffersQueue.add(byteBuffer);
        notifyAll();
    }

    private synchronized void waitNewBuffers() throws InterruptedException {
        while (buffersQueue.isEmpty()) {
            wait();
            if (!continueSend) break;
        }
    }

    private void send() throws InterruptedException, IOException {
        while (continueSend) {
            ByteBuffer sendByteBuffer = buffersQueue.peek();
            if (sendByteBuffer != null) {
                socketChannel.write(sendByteBuffer);
                if (!sendByteBuffer.hasRemaining()) {
                    buffersQueue.remove();
                }
            } else {
                fileSender.sendAnotherBuffer();
                waitNewBuffers();
            }
        }
    }
}
