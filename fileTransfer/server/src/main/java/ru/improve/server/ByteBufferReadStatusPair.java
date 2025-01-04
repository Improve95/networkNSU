package ru.improve.server;

import lombok.Data;
import lombok.Setter;

import java.nio.ByteBuffer;

@Data
@Setter
public class ByteBufferReadStatusPair {

    private ByteBuffer byteBuffer;

    private boolean idAllBytesRead = false;
}
