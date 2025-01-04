package ru.improve.communication.receiver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReadBytesReadStatusPair {

    private boolean dataFullRead;

    private int readBytes;

    public ReadBytesReadStatusPair(int readBytes) {
        dataFullRead = false;
        this.readBytes = readBytes;
    }
}
