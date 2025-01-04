package ru.improve.server;

import lombok.AllArgsConstructor;
import ru.improve.model.ClientData;
import ru.improve.model.ServerModel;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

@AllArgsConstructor
public class ClientService {

    private ServerModel serverModel;

    public void register(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        SocketChannel clientSocketChannel = serverSocketChannel.accept();
        clientSocketChannel.configureBlocking(false);
        clientSocketChannel.register(selector, SelectionKey.OP_READ);

        Map<SocketChannel, ClientData> clients = serverModel.getClients();
        clients.put(clientSocketChannel, new ClientData());
    }

    public void deleteClient(SocketChannel socketChannel) throws IOException {
        Map<SocketChannel, ClientData> clients = serverModel.getClients();

        ClientData clientData = clients.get(socketChannel);
        SpeedTest.printSpeed(socketChannel, clientData);

        clients.remove(socketChannel);
        socketChannel.close();
    }
}
