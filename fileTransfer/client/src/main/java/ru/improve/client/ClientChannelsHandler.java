package ru.improve.client;

import ru.improve.communication.sender.FileSender;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ClientChannelsHandler implements Runnable {

    private FileSender fileSender;

    private Selector selector;

    private boolean continueHandle = true;

    public ClientChannelsHandler(FileSender fileSender, Selector selector) {
        this.fileSender = fileSender;
        this.selector = selector;
    }

    @Override
    public void run() {
        try {
            channelsHandle();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public void stop() {
        continueHandle = false;
    }

    private void channelsHandle() throws IOException {
        while (continueHandle) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keysIter = selectionKeys.iterator();

            while (keysIter.hasNext()) {
                SelectionKey selectionKey = keysIter.next();

                if (selectionKey.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

                    ByteBuffer byteBuffer = ByteBuffer.allocate(1);
                    socketChannel.read(byteBuffer);
                    if (byteBuffer.get(0) == 10) {
                        fileSender.setFileUpload(true);
                    }
                }

                keysIter.remove();
            }
        }
    }
}
