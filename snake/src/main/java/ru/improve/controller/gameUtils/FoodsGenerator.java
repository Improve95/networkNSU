package ru.improve.controller.gameUtils;

import ru.improve.model.FieldData;
import ru.improve.model.SnakeModel;
import ru.improve.model.gameState.Coord;
import ru.improve.model.gameState.SnakeData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FoodsGenerator {

    private SnakeModel snakeModel;

    private FieldData fieldData;

    public FoodsGenerator(SnakeModel snakeModel) {
        this.snakeModel = snakeModel;
        this.fieldData = snakeModel.getFieldData();
    }

    public void generateApples() {
        List<SnakeData> gameSnakes = snakeModel.getAllSnakesCopyOnWriteList();

        List<Coord> allCoords = new ArrayList<>();
        for (var gameSnakeData : gameSnakes) {
            for (var point : gameSnakeData.getPoints()) {
                allCoords.add(point);
            }
        }

        for (var food : snakeModel.getFoods()) {
            allCoords.add(food);
        }

        List<Coord> freeSpaces = new ArrayList<>();
        for (int i = 0; i < fieldData.getCurrentFieldHeight(); ++i) {
            for (int j = 0; j < fieldData.getCurrentFieldWidth(); ++j) {
                Coord checkPoint = new Coord(fieldData.calculateCoordX(j), fieldData.calculateCoordY(i));
                if (!allCoords.contains(checkPoint)) {
                    freeSpaces.add(checkPoint);
                }
            }
        }

        Random random = new Random();

        List<Coord> foods = snakeModel.getFoods();
        int appleNumber = snakeModel.getGameConfig().getFoodStatic() + gameSnakes.size() - foods.size();
        for (int i = 0; i < appleNumber; ++i) {
            if (freeSpaces.size() > 0) {
                int randomPlace = random.nextInt(0, freeSpaces.size() - 1);

                foods.add(freeSpaces.remove(randomPlace));
            }
        }
    }
}
