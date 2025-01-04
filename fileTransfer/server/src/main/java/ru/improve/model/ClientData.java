package ru.improve.model;

import lombok.Data;
import lombok.Setter;
import ru.improve.communication.receiver.BufferReceiver;

import java.io.FileOutputStream;

@Data
@Setter
public class ClientData {

    private BufferReceiver bufferReceiver = new BufferReceiver();

    private FileOutputStream fileOutputStream;
    private String fileName;
    private long fileSize;

    private FileReadStage fileReadStage = FileReadStage.READ_NAME;

    private long readBytesNumberInInterval = 0;
    private long lastUpdate = 0;

    public void addReadBytesNumberInInterval(int bytesNumber) {
        readBytesNumberInInterval += bytesNumber;
        lastUpdate = System.currentTimeMillis();
    }

    public float getReadSpeed() {
        float speed = readBytesNumberInInterval / (( System.currentTimeMillis() - lastUpdate) / 1000.f);

        readBytesNumberInInterval = 0;
        lastUpdate = System.currentTimeMillis();

        return speed;
    }
}
