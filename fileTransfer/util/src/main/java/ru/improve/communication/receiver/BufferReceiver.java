package ru.improve.communication.receiver;

import lombok.Getter;
import lombok.Setter;
import ru.improve.communication.Constants;
import ru.improve.enums.ReadStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class BufferReceiver {

    @Setter
    private ReadStatus readStatus = ReadStatus.READ_SIZE;

    private ByteBuffer receiveDataSizeBuffer = ByteBuffer.allocate(Constants.INT_SIZE);

    @Getter
    private Queue<ByteBuffer> receiverDataBufferQueue = new ArrayDeque<>();

    @Setter
    private long dataSize = 0;
    private long currentReadDataBytes = 0;

    public ReadBytesReadStatusPair read(SocketChannel channel) throws IOException {
        ReadBytesReadStatusPair retPair;
        if (readStatus == ReadStatus.READ_SIZE) {
            retPair = readDataSize(channel);
        } else {
            retPair = readData(channel);
        }

        return retPair;
    }

    private ReadBytesReadStatusPair readData(SocketChannel channel) throws IOException {
        ReadBytesReadStatusPair retPair = null;
        if (currentReadDataBytes < dataSize) {
            ByteBuffer receiveDataBuffer;
            if (dataSize - currentReadDataBytes >= Constants.RECEIVE_BUFFER_SIZE) {
                receiveDataBuffer = ByteBuffer.allocate(Constants.RECEIVE_BUFFER_SIZE);
            } else {
                receiveDataBuffer = ByteBuffer.allocate((int)(dataSize - currentReadDataBytes));
            }


            int readBytesInCurrentIteration = channel.read(receiveDataBuffer);
            retPair = new ReadBytesReadStatusPair(readBytesInCurrentIteration);
            currentReadDataBytes += readBytesInCurrentIteration;
            receiveDataBuffer.rewind();
            receiverDataBufferQueue.add(receiveDataBuffer);
        }

        if (currentReadDataBytes == dataSize) {
            retPair.setDataFullRead(true);
            currentReadDataBytes = 0;
            readStatus = ReadStatus.READ_SIZE;
            receiveDataSizeBuffer.rewind();
        }

        return retPair;
    }

    private ReadBytesReadStatusPair readDataSize(SocketChannel channel) throws IOException {
        int readBytes = channel.read(receiveDataSizeBuffer);
        if (receiveDataSizeBuffer.position() == receiveDataSizeBuffer.capacity()) {
            receiveDataSizeBuffer.rewind();
            dataSize = receiveDataSizeBuffer.getInt();
            readStatus = ReadStatus.READ_DATA;
        }

        return new ReadBytesReadStatusPair(readBytes);
    }
}
