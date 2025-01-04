package ru.improve.model.gameState;

import lombok.Data;

import java.util.List;

@Data
public class GameState {

    private int stateOrder;

    private List<SnakeData> snakeDataList;

    private List<PlayerData> players;

    private List<Coord> foods;
}
