package ru.improve.server;

import ru.improve.model.ServerModel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class Server {

    private ServerModel serverModel;

    private InetAddress inetAddress;
    private int port;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private ServerChannelsHandler serverChannelsHandler;
    private Thread channelsHandlerThread;

    private SpeedTest speedTest;
    private Thread speedTestThread;

    public Server(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    public void initial(int port) {
        try {
            inetAddress = InetAddress.getByName("localhost");
            System.out.println("Server initial on --- " + inetAddress.getHostAddress() + ":" + port + "\n");
            this.port = port;

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(inetAddress, port));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            stop();
            throw new RuntimeException(ex.getMessage());
        }

        serverChannelsHandler = new ServerChannelsHandler(selector, serverSocketChannel, serverModel);
        channelsHandlerThread = new Thread(serverChannelsHandler);
        channelsHandlerThread.start();

        speedTest = new SpeedTest(serverModel);
        speedTestThread = new Thread(speedTest);
        speedTestThread.start();
    }

    public void stop() {
        try {
            if (serverChannelsHandler != null) {
                serverChannelsHandler.stop();
                channelsHandlerThread.interrupt();
            }
            if (speedTest != null) {
                speedTest.stop();
                speedTestThread.interrupt();
            }

            for (SelectionKey selectionKey : selector.selectedKeys()) {
                selectionKey.channel().close();
            }
            if (selector != null) {
                selector.close();
            }
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
