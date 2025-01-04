package ru.improve.model.gameState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.improve.communication.message.SnakesProto.Direction;
import ru.improve.communication.message.SnakesProto.GameState.Snake.SnakeState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SnakeData {

    private int playerId;

    @Builder.Default
    private Deque<Coord> points = new ArrayDeque<>();

    @Builder.Default
    private SnakeState snakeState = SnakeState.ALIVE;

    private Direction headDirection;

    public List<Coord> getCopyOnWritePoints() {
        return new CopyOnWriteArrayList<>(points);
    }
}
