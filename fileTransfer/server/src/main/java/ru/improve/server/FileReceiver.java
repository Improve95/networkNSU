package ru.improve.server;

import ru.improve.communication.Constants;
import ru.improve.communication.receiver.BufferReceiver;
import ru.improve.communication.receiver.ReadBytesReadStatusPair;
import ru.improve.enums.ReadStatus;
import ru.improve.model.ClientData;
import ru.improve.model.FileReadStage;
import ru.improve.model.ServerModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;

public class FileReceiver {

    private ServerModel serverModel;

    public FileReceiver(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    public int receiveFile(SocketChannel socketChannel) throws IOException {
        Map<SocketChannel, ClientData> clients = serverModel.getClients();
        ClientData clientData = clients.get(socketChannel);

        BufferReceiver bufferReceiver = clientData.getBufferReceiver();
        FileReadStage fileReadStage = clientData.getFileReadStage();

        if (clientData.getFileReadStage() == FileReadStage.READ_COMPLETE) {
            return 0;
        }

        ReadBytesReadStatusPair retPair;
        retPair = bufferReceiver.read(socketChannel);
        Queue<ByteBuffer> byteBuffers = bufferReceiver.getReceiverDataBufferQueue();

        if (fileReadStage == FileReadStage.READ_NAME) {
            if (retPair.isDataFullRead()) {
                ByteBuffer fileNameBuffer = byteBuffers.remove();
                clientData.setFileName(new String(fileNameBuffer.array()));
                clientData.setFileReadStage(FileReadStage.READ_FILE_SIZE);
            }
            return retPair.getReadBytes();
        }

        if (fileReadStage == FileReadStage.READ_FILE_SIZE) {
            if (retPair.isDataFullRead()) {
                ByteBuffer fileSizeBuffer = byteBuffers.remove();
                clientData.setFileSize(fileSizeBuffer.getLong());
                clientData.setFileReadStage(FileReadStage.READ_FILE);
                openFile(clientData);
                bufferReceiver.setDataSize(clientData.getFileSize());
                bufferReceiver.setReadStatus(ReadStatus.READ_DATA);
            }
            return retPair.getReadBytes();
        }

        if (fileReadStage == FileReadStage.READ_FILE) {
            ByteBuffer filePartBuffer = byteBuffers.poll();
            if (filePartBuffer != null) {
                clientData.getFileOutputStream().write(filePartBuffer.array());
                clientData.addReadBytesNumberInInterval(retPair.getReadBytes());
                if (retPair.isDataFullRead()) {
                    clientData.getFileOutputStream().close();
                    clientData.setFileReadStage(FileReadStage.READ_COMPLETE);
                    socketChannel.write(ByteBuffer.wrap(new byte[]{10}));
                    return -1;
                }
            }
            return retPair.getReadBytes();
        }

        return retPair.getReadBytes();
    }

    private void openFile(ClientData clientData) throws IOException {
        String workingDirectory = System.getProperty("user.dir");
        String uploadPath = workingDirectory + Constants.UPLOAD_PATH;

        File uploadDir = new File(uploadPath);
        uploadDir.mkdir();

        File fileOut = new File(uploadPath,  clientData.getFileName());
        fileOut.createNewFile();
        clientData.setFileOutputStream(new FileOutputStream(fileOut));
    }
}
