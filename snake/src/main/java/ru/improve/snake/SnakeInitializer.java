package ru.improve.snake;

import ru.improve.controller.SnakeController;
import ru.improve.model.SnakeModel;
import ru.improve.model.enums.ViewState;
import ru.improve.view.SnakeView;

public class SnakeInitializer {

    private SnakeModel snakeModel;

    private SnakeView snakeView;

    private SnakeController snakeController;

    public void initial() {
        snakeModel = new SnakeModel();
        snakeController = new SnakeController(snakeModel);
        snakeView = new SnakeView(snakeModel, snakeController);

        snakeView.initial();
        snakeModel.setViewState(ViewState.MAIN_SCENE);
    }
}
