package ru.improve.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.improve.communication.message.SnakesProto.GameConfig;
import ru.improve.model.gameState.PlayerData;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnounceGameData {

    private GameConfig gameConfig;

    private String gameName;

    private List<PlayerData> players;

    private boolean canJoin;

    private long lastAnnounceTime;
}
