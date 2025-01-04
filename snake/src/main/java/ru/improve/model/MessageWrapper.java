package ru.improve.model;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.improve.communication.message.SnakesProto;
import ru.improve.communication.message.SnakesProto.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageWrapper {

    /* адрес и порт откуда пришло сообщение, если принимает, и наоборот */
    private InetAddress inetAddress;

    private int port;

    private byte[] message;

    private int messageLength;

    @Builder.Default
    private long ackResendAttempts = 5;

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(inetAddress, port);
    }

    public GameMessage getMessageData() throws InvalidProtocolBufferException {
        return SnakesProto.GameMessage.parseFrom(Arrays.copyOf(message, messageLength));
    }

    public void reduceAckResendAttempts() {
        ackResendAttempts -= 1;
    }
}
