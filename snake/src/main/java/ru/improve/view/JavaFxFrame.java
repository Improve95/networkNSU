package ru.improve.view;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.improve.model.FieldData;
import ru.improve.model.SnakeModel;
import ru.improve.model.enums.GameStatus;
import ru.improve.model.enums.ViewState;
import ru.improve.model.gameState.Coord;
import ru.improve.model.gameState.PlayerData;
import ru.improve.model.gameState.SnakeData;
import ru.improve.view.viewControllers.MainSceneController;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Log4j2
public class JavaFxFrame extends Application {

    private static SnakeModel snakeModel;

    private static int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
    private static int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

    @Setter
    private static SnakeView snakeView;

    private static Stage mainStage;
    private static Scene mainScene;
    private static StackPane stackPaneSceneLayout;

    private static Parent gameSceneParent;

    private static MainSceneController mainSceneController;

    /* текущие данные во вьюшке */
    private static int currentFieldWidth = -1;
    private static int currentFieldHeight = -1;
    private static ViewState currentViewState = ViewState.INITIALIZE;

    public static void main(String args[]) {
        launch();
    }

    public static void setSnakeModel(SnakeModel snakeModel) {
        JavaFxFrame.snakeModel = snakeModel;
    }

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        mainStage.setTitle("Snake");

        mainStage.setOnCloseRequest(windowEvent -> {
            close();
        });

        try {
            initialGameScene();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        stackPaneSceneLayout = new StackPane();
        mainScene = new Scene(stackPaneSceneLayout, 0, 0);
        mainStage.setScene(mainScene);

        mainStage.setX(screenWidth / 2);
        mainStage.setY(screenHeight / 2);

        mainScene.setOnKeyPressed(new KeyEventHandler());

        snakeView.startTickGenerator();

        /* завершение потока, который был запущен только для запуска этой функции */
        Thread.currentThread().interrupt();
    }

    private static void close() {
        snakeView.closeApplication();
        mainStage.close();
    }

    private static void initialGameScene() throws IOException {
        FXMLLoader mainSceneLoader = new FXMLLoader(JavaFxFrame.class.getResource("mainScene.fxml"));
        gameSceneParent = mainSceneLoader.load();
        mainSceneController = mainSceneLoader.getController();
        mainSceneController.setSnakeView(snakeView);
    }

    public static void updateFrame() {
        changeState();
        blockingAndEnablesElements();
        changeWindowPosAndSize();
        changeFieldSize();

        updateCurrentGameBlock();
        if (snakeModel.getGameStatus() == GameStatus.PLAY) {
            drawSnakesAndApples();
            updateRankingList();
        } else if (snakeModel.getGameStatus() == GameStatus.WAIT) {
            updateAvailableGameList();
        }
    }

    private static void changeState() {
        if (snakeModel.getViewState() != currentViewState) {
            currentViewState = snakeModel.getViewState();
            switch (currentViewState) {
                case INITIALIZE -> {}
                case MAIN_SCENE -> switchToMainScene();
            }
        }
    }

    private static void blockingAndEnablesElements() {
        if (snakeModel.getGameStatus() == GameStatus.PLAY) {
            mainSceneController.setGameFieldExistence(true);
            mainSceneController.changeRankingListExistence(true);
            mainSceneController.changeCurrentGameLabelsExistence(true);
            mainSceneController.changeGameListVboxExistence(false);

            mainSceneController.changeNewGameButtonExistence(false);
            mainSceneController.changeExitGameButtonExistence(true);
        } else {
            mainSceneController.setGameFieldExistence(false);
            mainSceneController.changeRankingListExistence(false);
            mainSceneController.changeCurrentGameLabelsExistence(false);
            mainSceneController.changeGameListVboxExistence(true);

            mainSceneController.changeNewGameButtonExistence(true);
            mainSceneController.changeExitGameButtonExistence(false);
        }
    }

    private static void switchToMainScene() {
        stackPaneSceneLayout.getChildren().removeAll();
        stackPaneSceneLayout.getChildren().add(gameSceneParent);
        changeFieldSize();
        changeWindowPosAndSize();
        mainStage.show();
    }

    private static void changeFieldSize() {
        FieldData fieldData = snakeModel.getFieldData();
        if (currentFieldWidth != fieldData.getCurrentFieldWidth() || currentFieldHeight != fieldData.getCurrentFieldHeight()) {
            currentFieldWidth = fieldData.getCurrentFieldWidth();
            currentFieldHeight = fieldData.getCurrentFieldHeight();

            int fieldBlockSize = snakeModel.getBlockSize();
            mainSceneController.setGameFieldSize(currentFieldWidth * fieldBlockSize, currentFieldHeight * fieldBlockSize);
        }
    }

    private static void changeWindowPosAndSize() {
        Pane currentPane = (Pane) stackPaneSceneLayout.getChildren().get(0);
        double currentPaneWidth = currentPane.getWidth();
        double currentPaneHeight = currentPane.getHeight();

        if (mainStage.getWidth() != currentPaneWidth || mainStage.getHeight() != currentPaneHeight) {
            double newPosX = mainStage.getX() + (mainStage.getWidth() - currentPaneWidth) / 2;
            double newPosY = mainStage.getY() + (mainStage.getHeight() - currentPaneHeight) / 2;

            mainStage.setWidth(currentPaneWidth);
            mainStage.setHeight(currentPaneHeight);
            mainStage.setX(newPosX);
            mainStage.setY(newPosY);
        }
    }

    private static void drawSnakesAndApples() {
        int fieldBlockSize = snakeModel.getBlockSize();

        List<SnakeData> snakes = snakeModel.getAllSnakesCopyOnWriteList();
        List<Coord> foods = snakeModel.getAllFoodsCopyOnWriteList();

        AnchorPane gameField = mainSceneController.getGameField();
        gameField.getChildren().clear();

        for (var snake : snakes) {
            for (var point : snake.getCopyOnWritePoints()) {
                Rectangle block = createBlock(point, fieldBlockSize, Color.GAINSBORO);
                gameField.getChildren().add(block);
            }
        }

        for (Coord food : foods) {
            Rectangle block = createBlock(food, fieldBlockSize, Color.ORANGERED);
            gameField.getChildren().add(block);
        }
    }

    private static void updateRankingList() {
        mainSceneController.updateRankingList(snakeModel);
    }

    private static void updateAvailableGameList() {
        mainSceneController.updateAvailableGameList(snakeModel);
    }

    private static void updateCurrentGameBlock() {
        if (snakeModel.getGameStatus() == GameStatus.PLAY) {
            FieldData fieldData = snakeModel.getFieldData();
            String gameSizeText = fieldData.getCurrentFieldWidth() + "x" + fieldData.getCurrentFieldHeight();
            PlayerData masterPlayer = Optional.ofNullable(snakeModel.getMasterPlayer())
                    .orElse(PlayerData.builder().name("unknown").build());
            mainSceneController.setCurrentGameLabels(masterPlayer.getName(), gameSizeText, String.valueOf(snakeModel.getTotalFoodNumber()));
        } else {
            mainSceneController.setCurrentGameLabels("", "", "");
        }
    }

    private static Rectangle createBlock(Coord point, int fieldBlockSize, Paint blockColor) {
        Rectangle block = new Rectangle();
        block.setWidth(fieldBlockSize);
        block.setHeight(fieldBlockSize);
        block.setFill(blockColor);
        block.setStroke(Color.BLACK);
        block.setStrokeWidth(1);
        block.setLayoutX(point.getX() * fieldBlockSize);
        block.setLayoutY(point.getY() * fieldBlockSize);
        return block;
    }

    private static class KeyEventHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(KeyEvent event) {
            snakeView.clickOnKey(event);
        }
    }
}
