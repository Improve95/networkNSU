package ru.improve.communication.sender;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.improve.communication.Constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@AllArgsConstructor
@NoArgsConstructor
public class FileSender {

    private SocketChannel socketChannel;

    private BufferSender bufferSender;
    private Thread bufferSenderThread;

    private FileInputStream fileInputStream;
    private String fileName;
    private long fileSize;

    @Getter @Setter
    private boolean fileUpload = false;

    public FileSender(SocketChannel socketChannel, FileInputStream fileInputStream,
                      String fileName, long fileSize) {

        this.socketChannel = socketChannel;
        this.fileInputStream = fileInputStream;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public void send() throws IOException, InterruptedException {
        bufferSender = new BufferSender(this, socketChannel);
        bufferSenderThread = new Thread(bufferSender);
        bufferSenderThread.start();

        byte[] nameBuffer = fileName.getBytes();
        ByteBuffer dataByteBufferWithDataSize = ByteBuffer.allocate(4 + nameBuffer.length);
        dataByteBufferWithDataSize.putInt(nameBuffer.length);
        dataByteBufferWithDataSize.put(Constants.INT_SIZE, nameBuffer);
        dataByteBufferWithDataSize.rewind();
        bufferSender.addBuffer(dataByteBufferWithDataSize);

        ByteBuffer fileSizeBuffer = ByteBuffer.allocate(Constants.INT_SIZE + Constants.LONG_SIZE);
        fileSizeBuffer.putInt(Constants.LONG_SIZE);
        fileSizeBuffer.putLong(Constants.INT_SIZE, fileSize);
        fileSizeBuffer.rewind();
        bufferSender.addBuffer(fileSizeBuffer);

        sendFile();
        stop();

        Thread.sleep(5000);
        if (isFileUpload()) {
            System.out.println("Файл загружен на сервер");
        } else {
            System.out.println("Файл не был загружен на сервер");
        }
    }

    public void stop() {
        if (bufferSender != null) {
            bufferSender.stop();
            bufferSenderThread.interrupt();
        }
    }

    public synchronized void sendAnotherBuffer() {
        notifyAll();
    }

    public synchronized void sendFile() throws InterruptedException, IOException {
        while (true) {
            for (int i = 0; i < 5; i++) {
                ByteBuffer fileBuffer = ByteBuffer.allocate(Constants.RECEIVE_BUFFER_SIZE);
                int readBytes = fileInputStream.read(fileBuffer.array());
                if (readBytes <= 0) {
                    return;
                }
                bufferSender.addBuffer(fileBuffer);
            }
            wait();
        }
    }
}
