package ru.improve.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.improve.model.gameState.PlayerData;
import ru.improve.model.gameState.SnakeData;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerSnakePair {

    private PlayerData player;

    private SnakeData snakeData;
}
