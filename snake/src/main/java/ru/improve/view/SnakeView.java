package ru.improve.view;

import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import lombok.extern.log4j.Log4j2;
import ru.improve.controller.SnakeController;
import ru.improve.controller.gameUtils.MovementController;
import ru.improve.model.SnakeModel;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

@Log4j2
public class SnakeView {

    private SnakeModel snakeModel;

    private SnakeController snakeController;

    private MovementController movementController;

    private JavaFxFrame javaFxFrame;

    private Timer tickGenerator = new Timer();

    private TickGeneratorTask tickGeneratorTask;

    public SnakeView(SnakeModel snakeModel, SnakeController snakeController) {
        this.snakeModel = snakeModel;
        this.snakeController = snakeController;
        this.javaFxFrame = new JavaFxFrame();
        this.movementController = snakeController.getMovementController();
    }

    public void initial() {
        javaFxFrame.setSnakeModel(snakeModel);
        javaFxFrame.setSnakeView(this);
        new Thread(() -> javaFxFrame.main(null)).start();
        /*
            запуск генератора тиков и завершение потока, который вызывает javaFxFrame.main, происходит из javaFxFrame.start
        */
    }

    public void closeApplication() {
        tickGenerator.cancel();
        snakeController.stop();
    }

    public void startTickGenerator() {
        tickGeneratorTask = new TickGeneratorTask();
        tickGenerator.scheduleAtFixedRate(tickGeneratorTask, 0, 1000 / snakeModel.getFPS());
    }

    public void clickOnKey(KeyEvent event) {
        switch (event.getCode().getCode()) {
            case 'W' -> movementController.gotoUp();
            case 'A' -> movementController.gotoLeft();
            case 'S' -> movementController.gotoDown();
            case 'D' -> movementController.gotoRight();
        }
    }

    public void clickOnNewGameButton() {
        snakeController.createGame();
    }

    public void clickOnExitGameButton() {
        snakeController.exitFromGame();
    }

    public void connectToGame(InetSocketAddress gameAddress) {
        snakeController.connectToGame(gameAddress);
    }

    private class TickGeneratorTask extends TimerTask {

        @Override
        public void run() {
            Platform.runLater(() -> JavaFxFrame.updateFrame());
        }
    }
}
