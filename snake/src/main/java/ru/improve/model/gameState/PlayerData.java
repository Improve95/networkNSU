package ru.improve.model.gameState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.improve.communication.message.SnakesProto.PlayerType;
import ru.improve.communication.message.SnakesProto.NodeRole;

import java.net.InetSocketAddress;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PlayerData {

    private String name;

    private int id;

    private String ipAddress;

    private int port;

    @Builder.Default
    private NodeRole nodeRole = NodeRole.VIEWER;

    @Builder.Default
    private PlayerType playerType = PlayerType.HUMAN;

    @Builder.Default
    private int score = 0;

    @Builder.Default
    private long lmst = -1; // last message send time

    @Builder.Default
    private long lmrt = -1; // last message receive time

    public void incrementScore() {
        score += 1;
    }

    public InetSocketAddress getPlayerAddress() {
        return new InetSocketAddress(ipAddress, port);
    }
}
