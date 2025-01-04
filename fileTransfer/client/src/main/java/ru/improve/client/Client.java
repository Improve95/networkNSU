package ru.improve.client;

import ru.improve.communication.sender.FileSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    private String serverIp;
    private int port;

    private String fileName;

    private SocketChannel clientSocketChannel;
    private Selector selector;

    private FileSender fileSender;

    private ClientChannelsHandler clientChannelsHandler;
    private Thread clientChannelsHandlerThread;

    public void sendFile(String serverIp, int port, String filePath) {
        connectToServer(serverIp, port, filePath);

        File sendFile = new File(filePath);
        long fileSize = -1;
        if ((sendFile.exists()) && (sendFile.isFile())) {
            fileSize = sendFile.length();
        }

        if (fileSize <= 0) {
            stop();
            throw new RuntimeException("zero ore less file size");
        }

        try (FileInputStream fileInputStream = new FileInputStream(sendFile)) {
            fileSender = new FileSender(clientSocketChannel, fileInputStream, fileName, fileSize);

            clientChannelsHandler = new ClientChannelsHandler(fileSender, selector);
            clientChannelsHandlerThread = new Thread(clientChannelsHandler);
            clientChannelsHandlerThread.start();

            fileSender.send();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public void stop() {
        if (clientChannelsHandler != null) {
            clientChannelsHandler.stop();
            clientChannelsHandlerThread.interrupt();
        }

        if (fileSender != null) {
            fileSender.stop();
        }

        try {
            for (SelectionKey selectionKey : selector.selectedKeys()) {
                selectionKey.channel().close();
            }
            clientSocketChannel.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private void connectToServer(String serverIp, int port, String filePath) {
        this.serverIp = serverIp;
        this.port = port;

        Pattern pattern = Pattern.compile("([a-zA-Z0-9.]+)$");
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.find()) {
            fileName = matcher.group();
        } else {
            throw new RuntimeException("illegal file path");
        }

        try {
            clientSocketChannel = SocketChannel.open(new InetSocketAddress(serverIp, port));
            selector = Selector.open();
            clientSocketChannel.configureBlocking(false);
            clientSocketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException ex) {
            stop();
            throw new RuntimeException(ex.getMessage());
        }
    }
}
