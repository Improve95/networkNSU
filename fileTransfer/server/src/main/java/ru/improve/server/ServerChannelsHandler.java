package ru.improve.server;

import ru.improve.model.ServerModel;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class ServerChannelsHandler implements Runnable {

    private ServerModel serverModel;

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    private boolean continueHandle = true;

    public ServerChannelsHandler(Selector selector, ServerSocketChannel serverSocketChannel, ServerModel serverModel) {
        this.selector = selector;
        this.serverSocketChannel = serverSocketChannel;
        this.serverModel = serverModel;
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
        ClientService clientService = new ClientService(serverModel);
        FileReceiver fileReceiver = new FileReceiver(serverModel);

        while (continueHandle) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            var keysIter = selectionKeys.iterator();

            while (keysIter.hasNext()) {
                SelectionKey selectionKey = keysIter.next();

                if (selectionKey.isAcceptable()) {
                    clientService.register(selector, serverSocketChannel);
                }

                if (selectionKey.isReadable()) {
                    int readBytes = fileReceiver.receiveFile((SocketChannel) selectionKey.channel());
                    if (readBytes < 0) {
                        clientService.deleteClient((SocketChannel) selectionKey.channel());
                    }
                }

                keysIter.remove();
            }
        }
    }
}
