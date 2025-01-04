package ru.improve.controller.gameUtils;

import lombok.extern.log4j.Log4j2;
import ru.improve.communication.message.SnakesProto.NodeRole;
import ru.improve.controller.GameController;
import ru.improve.model.FieldData;
import ru.improve.model.PlayerSnakePair;
import ru.improve.model.SnakeModel;
import ru.improve.model.gameState.Coord;
import ru.improve.model.gameState.SnakeData;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class NextFrameCalculator extends TimerTask {

    private SnakeModel snakeModel;

    private FieldData fieldData;

    private FoodsGenerator foodsGenerator;

    private GameController gameController;

    public NextFrameCalculator(SnakeModel snakeModel, FoodsGenerator foodsGenerator, GameController gameController) {
        this.snakeModel = snakeModel;
        this.fieldData = snakeModel.getFieldData();
        this.foodsGenerator = foodsGenerator;
        this.gameController = gameController;
    }

    @Override
    public void run() {
        List<PlayerSnakePair> playerSnakePairs = snakeModel.getAllPlayerSnakePairs();
        moveAndChangeDirectionSnakes(playerSnakePairs);
        checkDeadSnake(playerSnakePairs);
        foodsGenerator.generateApples();
        snakeModel.getAndIncrementGameStateOrder();
        gameController.broadcastGameField();
    }

    private void moveAndChangeDirectionSnakes(List<PlayerSnakePair> playerSnakePairs) {
        for (var pair : playerSnakePairs) {
            SnakeData snakeData = pair.getSnakeData();
            Deque<Coord> snakeBlocks = snakeData.getPoints();
            if (!snakeBlocks.isEmpty() && pair.getPlayer().getNodeRole() != NodeRole.VIEWER) {
                Coord head = snakeBlocks.getFirst();
                Coord newSnakeBlock = null;
                switch (snakeData.getHeadDirection()) {
                    case UP -> newSnakeBlock = new Coord(head.getX(), fieldData.calculateCoordY(head.getY() - 1));
                    case LEFT -> newSnakeBlock = new Coord(fieldData.calculateCoordX(head.getX() - 1), head.getY());
                    case DOWN -> newSnakeBlock = new Coord(head.getX(), fieldData.calculateCoordY(head.getY() + 1));
                    case RIGHT -> newSnakeBlock = new Coord(fieldData.calculateCoordX(head.getX() + 1), head.getY());
                }
                snakeBlocks.addFirst(newSnakeBlock);
                if (!snakeModel.getFoods().remove(newSnakeBlock)) {
                    snakeBlocks.removeLast();
                } else {
                    pair.getPlayer().incrementScore();
                }
            }
        }
    }

    private void checkDeadSnake(List<PlayerSnakePair> playerSnakePairs) {
        Map<Coord, AtomicInteger> duplicate = new HashMap<>();
        for (var pair : playerSnakePairs) {
            Deque<Coord> snakePoints = pair.getSnakeData().getPoints();
            for (var point : snakePoints) {
                if (duplicate.containsKey(point)) {
                    duplicate.get(point).getAndIncrement();
                } else {
                    duplicate.put(point, new AtomicInteger(1));
                }
            }
        }

        for (var pair : playerSnakePairs) {
            Coord head = pair.getSnakeData().getPoints().stream().findFirst().orElse(null);
            if (head != null) {
                int duplicateNumber = duplicate.get(head).get();
                if (duplicateNumber > 1) {
                    gameController.manageDeadSnake(pair);
                }
            }
        }
    }
}
