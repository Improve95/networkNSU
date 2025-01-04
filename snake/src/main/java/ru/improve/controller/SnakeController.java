package ru.improve.controller;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.improve.controller.gameUtils.MovementController;
import ru.improve.controller.gameUtils.SnakeGenerator;
import ru.improve.controller.network.ReceiveMsgController;
import ru.improve.model.PlayerSnakePair;
import ru.improve.model.SnakeModel;
import ru.improve.model.gameState.PlayerData;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

@Log4j2
public class SnakeController {

    private SnakeModel snakeModel;

    @Getter
    private MovementController movementController;
    private SnakeGenerator snakeGenerator;

    private GameController gameController;

    private NetworkController networkController;

    private ReceiveMsgController receiveMsgController;
    private Thread receiverMsgControllerThread;

    private Timer checkLastMessageTimeTimer;
    private CheckLastMessageTime checkLastMessageTimeTask;

    public SnakeController(SnakeModel snakeModel) {
        this.snakeModel = snakeModel;
        this.snakeGenerator = new SnakeGenerator(snakeModel);
        initialSnakeUtils();
    }

    public void initialSnakeUtils() {
        networkController = new NetworkController(snakeModel);
        networkController.initial();

        gameController = new GameController(snakeModel, networkController);
        movementController = new MovementController(snakeModel, networkController);

        receiveMsgController = new ReceiveMsgController(snakeModel, networkController, gameController, movementController);
        receiverMsgControllerThread = new Thread(receiveMsgController);
        receiverMsgControllerThread.start();

        checkLastMessageTimeTask = new CheckLastMessageTime();
        checkLastMessageTimeTimer = new Timer();
        checkLastMessageTimeTimer.scheduleAtFixedRate(checkLastMessageTimeTask, 0, snakeModel.getGameConfig().getStateDelayMs() / 2);

        PlayerData newLocalPlayer = PlayerData.builder()
                .name(snakeModel.getLocalPlayerName())
                .id(snakeModel.getAndIncrementPlayerId())
                .ipAddress(snakeModel.getLocalAddress().getHostName())
                .port(snakeModel.getLocalAddress().getPort())
                .build();
        snakeModel.setLocalPlayer(newLocalPlayer);
    }

    public void stop() {
        log.debug("close application");
        gameController.exitFromGame();
        if (receiverMsgControllerThread != null) {
            receiverMsgControllerThread.interrupt();
        }
        if (networkController != null) {
            networkController.stop();
        }
        if (gameController != null) {
            gameController.stop();
        }
        if (checkLastMessageTimeTimer != null) {
            checkLastMessageTimeTimer.cancel();
        }
        if (receiverMsgControllerThread != null) {
            receiveMsgController.stop();
            receiverMsgControllerThread.interrupt();
        }
    }

    public void createGame() {
        gameController.createGame();
    }

    public void connectToGame(InetSocketAddress gameAddress) {
        gameController.connectToGame(gameAddress);
    }

    public void exitFromGame() {
        gameController.exitFromGame();
    }

    private class CheckLastMessageTime extends TimerTask {

        @Override
        public void run() {
            snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                    /* фильтр чтобы не проверять самого себя на время получения последнего сообщения */
                    .filter(entry -> !entry.getKey().equals(snakeModel.getLocalAddress()))
                    .forEach(entry -> {
                        PlayerSnakePair pair = entry.getValue();
                        gameController.checkPlayersOnOutOfTimeout(pair.getPlayer().getLmrt(), entry.getKey());
                    });
        }
    }
}
