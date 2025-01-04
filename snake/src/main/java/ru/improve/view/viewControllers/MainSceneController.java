package ru.improve.view.viewControllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.improve.communication.message.SnakesProto;
import ru.improve.model.AnnounceGameData;
import ru.improve.model.SnakeModel;
import ru.improve.view.SnakeView;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class MainSceneController {

    @Setter
    private SnakeView snakeView;

    /* основное поле */
    @Getter
    @FXML
    private AnchorPane gameField;

    /* кнопки */
    @FXML
    private Button newGameButton;

    @FXML
    private Button exitGameButton;

    /* статистика */
    @FXML
    private VBox rankingListVBox;

    @FXML
    private ScrollPane rankingScrollPane;

    @FXML
    private AnchorPane currentGameAnchorPane;

    @FXML
    private Label masterNameLabel;

    @FXML
    private Label gameSizeLabel;

    @FXML
    private Label foodNumberLabel;

     /* доступные игры */
    @FXML
    private HBox gameListTitle;

    @FXML
    private VBox gameListVbox;

    private Map<Integer, InetSocketAddress> gamesMap = new HashMap<>();

    public void setGameFieldSize(int width, int height) {
        gameField.setPrefWidth(width);
        gameField.setPrefHeight(height);
    }

    public void setCurrentGameLabels(String masterLabelText, String gameSizeText, String foodNumberText) {
        masterNameLabel.setText(masterLabelText);
        gameSizeLabel.setText(gameSizeText);
        foodNumberLabel.setText(foodNumberText);
    }

    public void setGameFieldExistence(boolean existence) {
        gameField.setDisable(!existence);
        gameField.setManaged(existence);
        gameField.setVisible(existence);
    }

    public void changeCurrentGameLabelsExistence(boolean existence) {
        currentGameAnchorPane.setDisable(!existence);
        currentGameAnchorPane.setManaged(existence);
        currentGameAnchorPane.setVisible(existence);
    }

    public void changeRankingListExistence(boolean existence) {
        rankingScrollPane.setDisable(!existence);
        rankingScrollPane.setManaged(existence);
        rankingScrollPane.setVisible(existence);
    }

    public void changeGameListVboxExistence(boolean existence) {
        gameListVbox.setDisable(!existence);
        gameListVbox.setManaged(existence);
        gameListVbox.setVisible(existence);
    }

    public void changeNewGameButtonExistence(boolean existence) {
        newGameButton.setDisable(!existence);
        newGameButton.setManaged(existence);
        newGameButton.setVisible(existence);
    }

    public void changeExitGameButtonExistence(boolean existence) {
        exitGameButton.setDisable(!existence);
        exitGameButton.setManaged(existence);
        exitGameButton.setVisible(existence);
    }

    public void updateRankingList(SnakeModel snakeModel) {
        AtomicInteger placeInRankingList = new AtomicInteger(1);
        List<HBox> rankingListItems = snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                .map(entry -> entry.getValue().getPlayer())
                .filter(playerData -> playerData.getNodeRole() != SnakesProto.NodeRole.VIEWER)
                .sorted((p1, p2) -> p1.getScore() > p2.getScore() ? 1 : 0)
                .map(playerData -> {
                    HBox hBox = new HBox();
                    Label rankLabel = new Label();
                    rankLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    rankLabel.setText(placeInRankingList.getAndIncrement() + ".");

                    Label playerNameAndScoreLabel = new Label();
                    playerNameAndScoreLabel.setText(playerData.getName() + ": " + playerData.getScore());
                    playerNameAndScoreLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
                    playerNameAndScoreLabel.setPadding(new Insets(0, 0, 0, 8));

                    hBox.getChildren().add(rankLabel);
                    hBox.getChildren().add(playerNameAndScoreLabel);
                    return hBox;
                }).toList();
        rankingListVBox.getChildren().clear();
        rankingListVBox.getChildren().addAll(rankingListItems);
    }

    public void updateAvailableGameList(SnakeModel snakeModel) {
        if (gamesMap.size() == snakeModel.getAnnounceGameDataMap().size()) {
            return;
        }

        AtomicInteger gameId = new AtomicInteger(1);
        gamesMap.clear();
        List<HBox> availableGameListItems = snakeModel.getAnnounceGameDataMap().entrySet().stream()
                .map(entry -> {
                    InetSocketAddress inetSocketAddress = entry.getKey();
                    AnnounceGameData announceGameData = entry.getValue();
                    InetSocketAddress address = entry.getKey();

                    int currentGameId = gameId.getAndIncrement();

                    HBox gameListItem = new HBox(5);
                    createLabels(currentGameId, announceGameData, address, gameListItem);

                    Button button = new Button();
                    button.setText("Connect");
                    button.setPrefWidth(85);
                    gameListItem.getChildren().add(button);
                    button.setOnAction(event -> clickOnConnectButton(currentGameId));

                    gamesMap.put(currentGameId, inetSocketAddress);

                    return gameListItem;
                }).toList();

        gameListVbox.getChildren().clear();
        gameListVbox.getChildren().add(gameListTitle);
        gameListVbox.getChildren().addAll(availableGameListItems);
    }

    @FXML
    private void clickOnNewGameButton() {
        snakeView.clickOnNewGameButton();
    }

    @FXML
    private void clickOnExitGameButton() {
        snakeView.clickOnExitGameButton();
    }

    private void clickOnConnectButton(int gameId) {
        InetSocketAddress gameAddress = gamesMap.get(gameId);
        snakeView.connectToGame(gameAddress);
    }

    private void createLabels(int gameNumber, AnnounceGameData announceGameData,
                              InetSocketAddress address, HBox gameListItem) {

        List<Label> labels = new ArrayList<>();

        Label label1 = new Label();
        label1.setText(String.valueOf(gameNumber));
        label1.setPrefWidth(20);
        labels.add(label1);

        Label label2 = new Label();
        label2.setText(announceGameData.getGameName());
        label2.setPrefWidth(80);
        labels.add(label2);

        Label label3 = new Label();
        label3.setText(address.getAddress().getHostAddress());
        label3.setPrefWidth(110);
        labels.add(label3);

        Label label4 = new Label();
        label4.setText(announceGameData.getGameConfig().getWidth() + "x" + announceGameData.getGameConfig().getHeight());
        label4.setPrefWidth(80);
        labels.add(label4);

        Label label5 = new Label();
        label5.setText(String.valueOf(announceGameData.getGameConfig().getFoodStatic()));
        label5.setPrefWidth(112);
        labels.add(label5);

        for (Label label : labels) {
            label.setTextAlignment(TextAlignment.CENTER);
            label.setPrefWidth(Region.USE_COMPUTED_SIZE);
            label.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            gameListItem.getChildren().add(label);
        }
    }
}
