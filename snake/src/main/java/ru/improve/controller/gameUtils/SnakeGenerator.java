package ru.improve.controller.gameUtils;

import ru.improve.communication.message.SnakesProto.Direction;
import ru.improve.exceptions.CannotCreateSnakeException;
import ru.improve.model.FieldData;
import ru.improve.model.SnakeModel;
import ru.improve.model.gameState.Coord;
import ru.improve.model.gameState.SnakeData;
import ru.improve.util.Builders;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class SnakeGenerator {

    private SnakeModel snakeModel;

    public SnakeGenerator(SnakeModel snakeModel) {
        this.snakeModel = snakeModel;
    }

    public SnakeData generateNewSnake(int playerId) throws CannotCreateSnakeException {
        FieldData fieldData = snakeModel.getFieldData();
        List<SnakeData> gameSnakes = snakeModel.getAllSnakesCopyOnWriteList();

        List<Coord> allCoords = new ArrayList<>();
        for (var gameSnakeData : gameSnakes) {
            for (var points : gameSnakeData.getPoints()) {
                allCoords.add(points);
            }
        }

        for (var food : snakeModel.getFoods()) {
            allCoords.add(food);
        }

        SnakeData newSnake = SnakeData.builder()
                .playerId(playerId)
                .build();
        List<Coord> freeSpaces = collectFreeSpaces(allCoords, fieldData);

        if (freeSpaces.isEmpty()) {
            throw new CannotCreateSnakeException();
        }

        Random random = new Random();
        Coord freeSpace = freeSpaces.get(random.nextInt(0, freeSpaces.size() - 1));
        int middleX = freeSpace.getX() + 1;
        int middleY = freeSpace.getY() + 1;

        Deque<Coord> newSnakePoints = newSnake.getPoints();
        newSnakePoints.addFirst(Builders.buildPoint(fieldData, middleX, middleY));
        switch (random.nextInt(1, 4)) {
            case 1 -> {
                newSnakePoints.addFirst(new Coord(fieldData.calculateCoordX(middleX), middleY + 1));
                newSnake.setHeadDirection(Direction.UP);
            }
            case 2 -> {
                newSnakePoints.addFirst(new Coord(fieldData.calculateCoordX(middleX + 1), middleY));
                newSnake.setHeadDirection(Direction.LEFT);
            }
            case 3 -> {
                newSnakePoints.addFirst(new Coord(fieldData.calculateCoordX(middleX), middleY - 1));
                newSnake.setHeadDirection(Direction.DOWN);
            }
            case 4 -> {
                newSnakePoints.addFirst(new Coord(fieldData.calculateCoordX(middleX - 1), middleY));
                newSnake.setHeadDirection(Direction.RIGHT);
            }
        }
        return newSnake;
    }

    private List<Coord> collectFreeSpaces(List<Coord> allCoords, FieldData fieldData) {
        List<Coord> freeSpaces = new ArrayList<>();
        for (int yi = 0; yi < fieldData.getCurrentFieldHeight() - 1; yi++) {
            for (int xj = 0; xj < fieldData.getCurrentFieldWidth() - 1; xj++) {
                boolean spaceIsFree = true;
                for (int k1 = -1; k1 < 1 && spaceIsFree; k1++) {
                    for (int k2 = -1; k2 < 1 && spaceIsFree; k2++) {
                        int x = fieldData.calculateCoordX(xj + k2);
                        int y = fieldData.calculateCoordY(yi + k1) ;
                        if (allCoords.contains(Builders.buildPoint(fieldData, x, y))) {
                            spaceIsFree = false;
                            break;
                        }
                    }
                }
                if (spaceIsFree) {
                    freeSpaces.add(Builders.buildPoint(fieldData, xj, yi));
                }
            }
        }
        return freeSpaces;
    }
}
