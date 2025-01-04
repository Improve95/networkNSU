package ru.improve.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.improve.communication.message.SnakesProto.GameMessage;
import ru.improve.communication.message.SnakesProto.GameState;
import ru.improve.communication.message.SnakesProto.NodeRole;
import ru.improve.controller.gameUtils.FoodsGenerator;
import ru.improve.controller.gameUtils.NextFrameCalculator;
import ru.improve.controller.gameUtils.SnakeGenerator;
import ru.improve.exceptions.CannotCreateSnakeException;
import ru.improve.model.AnnounceGameData;
import ru.improve.model.MessageWrapper;
import ru.improve.model.PlayerSnakePair;
import ru.improve.model.SnakeModel;
import ru.improve.model.enums.GameStatus;
import ru.improve.model.gameState.Coord;
import ru.improve.model.gameState.PlayerData;
import ru.improve.model.gameState.SnakeData;
import ru.improve.util.Builders;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;

import static ru.improve.model.SnakeConstants.LOCAL_PLAYER_ADDRESS;

@Log4j2
public class GameController {

    private SnakeModel snakeModel;

    private SnakeGenerator snakeGenerator;

    private NetworkController networkController;

    private FoodsGenerator foodsGenerator;

    private NextFrameCalculator nextFrameCalculator;
    private Timer frameCalculatorTimer;

    @Getter @Setter
    private long connectAckMsgSeq = -1;
    private InetSocketAddress announceAddress;
    private AnnounceGameData announceGameData;

    public GameController(SnakeModel snakeModel, NetworkController networkController) {
        this.snakeModel = snakeModel;
        this.networkController = networkController;
        this.snakeGenerator = new SnakeGenerator(snakeModel);
        this.foodsGenerator = new FoodsGenerator(snakeModel);
    }

    public void stop() {
        if (frameCalculatorTimer != null) {
            frameCalculatorTimer.cancel();
        }
    }

    public void createGame() {
        if (snakeModel.getGameStatus() == GameStatus.WAIT) {
            log.debug("createGame");

            /* устанавливаю размер поля */
            snakeModel.getFieldData().setFieldSize(snakeModel.getGameConfig().getWidth(),
                    snakeModel.getGameConfig().getHeight());

            /* инициализирую локальную змейку и игрока мастера */
            try {
                snakeModel.setLocalAddress(LOCAL_PLAYER_ADDRESS);
                snakeModel.setMasterAddress(LOCAL_PLAYER_ADDRESS);
                PlayerData localMaster = snakeModel.getLocalPlayer();
                localMaster.setNodeRole(NodeRole.MASTER);
                SnakeData masterPlayerSnake = snakeGenerator.generateNewSnake(localMaster.getId());
                snakeModel.putPlayerSnakePair(snakeModel.getLocalAddress(), new PlayerSnakePair(localMaster, masterPlayerSnake));
            } catch (CannotCreateSnakeException ex) {
                log.error(ex);
                return;
            }

            /* генерю яблочки */
            foodsGenerator.generateApples();

            /* запускаю калькулятор следующего кадра */
            if (frameCalculatorTimer == null) {
                nextFrameCalculator = new NextFrameCalculator(snakeModel, foodsGenerator, this);
                frameCalculatorTimer = new Timer();
                frameCalculatorTimer.scheduleAtFixedRate(nextFrameCalculator, 0, snakeModel.getTickDelay());
            }

            networkController.sendAnnounce();
            networkController.startSendPingMsg();
            snakeModel.setGameStatus(GameStatus.PLAY);
        }
    }

    public void connectToGame(InetSocketAddress masterAddress) {
        log.info("choosen game {}", masterAddress);
        snakeModel.getLocalPlayer().setNodeRole(NodeRole.NORMAL);

        announceAddress = masterAddress;
        announceGameData = snakeModel.getAnnounceGameDataMap().get(masterAddress);
        GameMessage joinMsg = Builders.buildJoinMsg(announceGameData, snakeModel.getLocalPlayer(), snakeModel);
        MessageWrapper joinMsgWrapper = Builders.buildMessageWrapper(joinMsg, masterAddress.getAddress(), masterAddress.getPort());
        networkController.sendMessageWithWaitAck(joinMsg.getMsgSeq(), joinMsgWrapper);
        setConnectAckMsgSeq(joinMsg.getMsgSeq());
        snakeModel.setGameStatus(GameStatus.TRY_CONNECT);
    }

    public void checkConnectWaitAckMsgSeq(long ackSeq, MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        if (connectAckMsgSeq == ackSeq) {
            GameMessage gameMessage = messageWrapper.getMessageData();
            snakeModel.setMasterAddress(messageWrapper.getInetSocketAddress());

            String masterName = announceGameData.getPlayers().stream()
                    .filter(playerData -> playerData.getId() == gameMessage.getSenderId())
                    .findFirst().orElseThrow(() -> new RuntimeException()).getName();

            PlayerData masterData = PlayerData.builder()
                    .id(gameMessage.getSenderId())
                    .name(masterName)
                    .ipAddress(messageWrapper.getInetSocketAddress().getHostName())
                    .port(messageWrapper.getInetSocketAddress().getPort())
                    .nodeRole(NodeRole.MASTER)
                    .score(0)
                    .build();
            SnakeData masterSnake = SnakeData.builder()
                    .playerId(gameMessage.getSenderId())
                    .build();
            
            snakeModel.getLocalPlayer().setId(gameMessage.getReceiverId());
//            snakeModel.getLocalSnake().setSnakeData(new SnakeData());
            snakeModel.getFieldData().setFieldSize(announceGameData.getGameConfig().getWidth(), announceGameData.getGameConfig().getHeight());
            snakeModel.putPlayerSnakePair(announceAddress, new PlayerSnakePair(masterData, masterSnake));
            snakeModel.setGameStatus(GameStatus.PLAY);
        }
    }

    public void exitFromGame() {
        if (snakeModel.getGameStatus() == GameStatus.PLAY) {

            networkController.stopOnExitGame();

            snakeModel.getFieldData().setFieldSize(0, 0);
            snakeModel.clearPlayerSnakeDataMap();
            if (frameCalculatorTimer != null) {
                frameCalculatorTimer.cancel();
                frameCalculatorTimer.purge();
                frameCalculatorTimer = null;
            }
        }
        snakeModel.setGameStatus(GameStatus.WAIT);
    }

    public void acceptRemotePlayer(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        InetSocketAddress sendAddress = messageWrapper.getInetSocketAddress();
        GameMessage ackMessage;

        boolean isAckMessage = true;

        /* проверка случая, когда затерялся ack пакет и клиент снова шлет connect request */
        if (snakeModel.containsPlayerSnakeMap(sendAddress)) {
            int existPlayerId = snakeModel.getPlayer(sendAddress).getId();
            snakeModel.getSnake(sendAddress).setSnakeState(GameState.Snake.SnakeState.ALIVE);
            ackMessage = Builders.buildAckMsg(snakeModel.getLocalPlayer().getId(), existPlayerId, gameMessage.getMsgSeq());
        } else {
            try {
                int newPlayerId = snakeModel.getAndIncrementPlayerId();

                if (gameMessage.getJoin().getRequestedRole() != NodeRole.NORMAL &&
                        gameMessage.getJoin().getRequestedRole() != NodeRole.VIEWER) {

                    log.error("конектящийся косячит, но мне пох");
                }

                NodeRole connectPlayerRole;
                if (snakeModel.getDeputyAddress() == null) {
                    snakeModel.setDeputyAddress(messageWrapper.getInetSocketAddress());
                    connectPlayerRole = NodeRole.DEPUTY;
                } else {
                    connectPlayerRole = NodeRole.NORMAL;
                }

                PlayerData newPlayer = PlayerData.builder()
                        .name(gameMessage.getJoin().getPlayerName())
                        .id(newPlayerId)
                        .ipAddress(messageWrapper.getInetAddress().getHostAddress())
                        .port(messageWrapper.getPort())
                        .nodeRole(connectPlayerRole)
                        .build();
                SnakeData newSnakeData = snakeGenerator.generateNewSnake(newPlayerId);

                snakeModel.putPlayerSnakePair(messageWrapper.getInetSocketAddress(), new PlayerSnakePair(newPlayer, newSnakeData));
                ackMessage = Builders.buildAckMsg(snakeModel.getLocalPlayer().getId(), newPlayerId, gameMessage.getMsgSeq());
                log.info("acceptRemotePlayer with seq: {}", gameMessage.getMsgSeq());
            } catch (CannotCreateSnakeException ex) {
                ackMessage = Builders.buildErrorMsg(ex.getMessage(), gameMessage.getMsgSeq());
                isAckMessage = false;
            }
        }

        MessageWrapper responseMessageWrapper = Builders.buildMessageWrapper(ackMessage,
                messageWrapper.getInetAddress(), messageWrapper.getPort());
        if (isAckMessage) {
            networkController.sendMessageWithoutAck(responseMessageWrapper);
        } else {
            networkController.sendMessageWithWaitAck(ackMessage.getMsgSeq(), responseMessageWrapper);
        }
    }

    public void broadcastGameField() {
        GameMessage gameMessage = Builders.buildGameStateMsg(snakeModel);
        snakeModel.getPlayerSnakeDataMap().keySet().stream()
                .filter(inetSocketAddress -> !inetSocketAddress.equals(snakeModel.getLocalAddress()))
                .forEach(inetSocketAddress -> {
                    MessageWrapper messageWrapper = MessageWrapper.builder()
                            .inetAddress(inetSocketAddress.getAddress())
                            .port(inetSocketAddress.getPort())
                            .message(gameMessage.toByteArray())
                            .messageLength(gameMessage.toByteArray().length)
                            .build();
                    networkController.sendMessageWithWaitAck(gameMessage.getMsgSeq(), messageWrapper);
                });
    }

    public void changeGameState(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        GameState protoGameState = gameMessage.getState().getState();
        if (snakeModel.getGameStateOrder() < protoGameState.getStateOrder()) {
            snakeModel.setGameStateOrder(protoGameState.getStateOrder());

            List<Coord> newFoods = Builders.buildFoodsFromFoodsProto(protoGameState.getFoodsList());
            List<Coord> foods = snakeModel.getFoods();
            foods.clear();
            foods.addAll(newFoods);

            List<PlayerData> players = Builders.buildPlayersFromSnakeProto(protoGameState.getPlayers().getPlayersList());
            Map<Integer, PlayerSnakePair> playerSnakeMap = players.stream()
                    .map(playerData -> {
                        if (playerData.getId() == snakeModel.getLocalPlayer().getId()) {
                            snakeModel.setLocalAddress(playerData.getPlayerAddress());
                            snakeModel.setLocalPlayer(playerData);
//                            snakeModel.getLocalPlayer().setNodeRole(playerData.getNodeRole());
                        }
                        return playerData;
                    })
                    .collect(Collectors.toMap(
                            playerData -> playerData.getId(),
                            playerData -> PlayerSnakePair.builder().player(playerData).build()
                    ));

            List<SnakeData> snakes = Builders.buildSnakesFromSnakeProto(protoGameState.getSnakesList());
            snakes.stream()
                    .forEach(snakeData -> {
                        PlayerSnakePair pair = playerSnakeMap.get(snakeData.getPlayerId());
                        pair.setSnakeData(snakeData);
                    });

            InetSocketAddress masterAddress = messageWrapper.getInetSocketAddress();
            Map<InetSocketAddress, PlayerSnakePair> playerSnakeDataMap = playerSnakeMap.values().stream()
                    .map(playerSnakePair -> {
                        PlayerData playerData = playerSnakePair.getPlayer();
                        /* устанавливаем правильный адрес мастера и заместителя */
                        if (playerData.getNodeRole() == NodeRole.MASTER) {
                            playerData.setIpAddress(masterAddress.getHostName());
                            playerData.setPort(masterAddress.getPort());
                        } else if (playerData.getNodeRole() == NodeRole.DEPUTY) {
                            snakeModel.setDeputyAddress(new InetSocketAddress(playerData.getIpAddress(), playerData.getPort()));
                        }
                        return playerSnakePair;
                    })
                    .collect(Collectors.toMap(
                            playerSnakePair -> playerSnakePair.getPlayer().getPlayerAddress(),
                            playerSnakePair -> playerSnakePair
                    ));

            var currentMap = snakeModel.getPlayerSnakeDataMap();
            /* вот тут нужно добавить synchronized блок, потому что я очищаю мапу
            * но так же нужно добавить эти синхронизированные блоки по мапе и во все остальные места
            * но т.к. там везде stream api, то оно не ломается с исключением, хотя возможно логика
            * когда то сломается, но в такие моменты я говорю:
            * "ну и что, если спутник пролетит мимо луны, с кем не бывает" */
            currentMap.clear();
            currentMap.putAll(playerSnakeDataMap);
        }
    }

    public void manageDeadSnake(PlayerSnakePair playerSnakePair) {
        InetSocketAddress addressDeadSnake = snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                .filter(entry -> entry.getValue().equals(playerSnakePair))
                .map(entry -> entry.getKey())
                .findFirst().orElse(null);

        PlayerData playerData = playerSnakePair.getPlayer();
        SnakeData snakeData = playerSnakePair.getSnakeData();
        log.info(addressDeadSnake);
        if (addressDeadSnake.equals(snakeModel.getMasterAddress())) {
            System.out.println("master dead");
            snakeData.getPoints().clear();
        } else {
            playerData.setNodeRole(NodeRole.VIEWER);
            /* отправить сообщение о смене роли на вьювера и удалить игрока */
//            snakeModel.getPlayerSnakeDataMap().remove(addressDeadSnake);
            snakeData.getPoints().clear();
        }
    }

    public void changeRoleManage(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        InetSocketAddress address = messageWrapper.getInetSocketAddress();
        GameMessage gameMessage = messageWrapper.getMessageData();
        NodeRole sendNodeRole = gameMessage.getRoleChange().getReceiverRole();
        NodeRole receiveNodeRole = gameMessage.getRoleChange().getReceiverRole();
        NodeRole localNodeRole = snakeModel.getLocalPlayer().getNodeRole();

        if (localNodeRole == NodeRole.NORMAL && localNodeRole == receiveNodeRole) {
            /* случай когда происходит смена мастера для normal */
            if (sendNodeRole == NodeRole.MASTER && !snakeModel.getMasterAddress().equals(address)) {
                replaceMaster(address);
            }
        }
    }

    public void checkPlayersOnOutOfTimeout(long lastMsgTime, InetSocketAddress address) {
        var playerSnakeMap = snakeModel.getPlayerSnakeDataMap();
        /* когда еще не пришло ни одной смены состояний поля, мапа не консистента и нельзя с ней работать */
        if (playerSnakeMap.size() <= 1 && snakeModel.getGameStatus() != GameStatus.PLAY) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMsgTime > snakeModel.getGameConfig().getStateDelayMs() * (8 / 10.0)) {
            PlayerSnakePair outOfTimeoutPair = snakeModel.getPlayerSnakePair(address);
            PlayerData outOfTimeoutPlayer = outOfTimeoutPair.getPlayer();
            if (outOfTimeoutPlayer.getNodeRole() == NodeRole.NORMAL) {

                outOfTimeoutPair.getSnakeData().setSnakeState(GameState.Snake.SnakeState.ZOMBIE);

            } else if (outOfTimeoutPlayer.getNodeRole() == NodeRole.DEPUTY) {

                if (snakeModel.getLocalPlayer().getNodeRole() == NodeRole.MASTER) {
                    deputyTimeoutOnMaster(address);
                }

            } else if (outOfTimeoutPlayer.getNodeRole() == NodeRole.MASTER) {

                if (snakeModel.getLocalPlayer().getNodeRole() == NodeRole.DEPUTY) {
                    masterTimeoutOnDeputy(address);
                } else if (snakeModel.getLocalPlayer().getNodeRole() == NodeRole.NORMAL) {
                    masterTimeoutOnNormal();
                }

            }
        }
    }

    private void deputyTimeoutOnMaster(InetSocketAddress outOfTimeoutAddress) {
        log.info("deputy disconnect by timeout on master");
    }

    private void masterTimeoutOnDeputy(InetSocketAddress outOfTimeoutAddress) {
        log.info("master disconnect by timeout on deputy");
        snakeModel.setMasterAddress(snakeModel.getLocalAddress());
        PlayerData localPlayer = snakeModel.getLocalPlayer();
        localPlayer.setNodeRole(NodeRole.MASTER);

        /* удаляю мастера, которого больше нет с нами */
        snakeModel.getPlayerSnakeDataMap().remove(outOfTimeoutAddress);

        /* устанавливаю неиспользованный id */
        int newId = 0;
        for (var entry : snakeModel.getPlayerSnakeDataMap().entrySet()) {
            int curId = entry.getValue().getPlayer().getId();
            if (curId > newId) {
                newId = curId;
            }
        }
        snakeModel.getNextFreePlayerId().set(newId + 1);

        /* выбираю нового deputy */
        var newDeputyPlayerSnake = snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(snakeModel.getLocalAddress()))
                .findFirst().orElse(null);
        if (newDeputyPlayerSnake != null) {
            GameMessage changeRoleOnDeputyMsg = Builders.buildChangeRoleMsg(NodeRole.MASTER, NodeRole.DEPUTY, localPlayer.getId(),
                    newDeputyPlayerSnake.getValue().getPlayer().getId(), snakeModel);
            InetSocketAddress newDeputyAddress = newDeputyPlayerSnake.getKey();
            MessageWrapper changeRoleMsgOnDeputyWrapper = Builders.buildMessageWrapper(changeRoleOnDeputyMsg, newDeputyAddress.getAddress(), newDeputyAddress.getPort());
            networkController.sendMessageWithWaitAck(changeRoleOnDeputyMsg.getMsgSeq(), changeRoleMsgOnDeputyWrapper);
        } else {
            snakeModel.setDeputyAddress(null);
        }

        /* делаю рассылку всем, что я новый master */
        snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(snakeModel.getLocalAddress()))
                .forEach(entry -> {
                    InetSocketAddress address = entry.getKey();
                    GameMessage changeRoleMsg = Builders.buildChangeRoleMsg(NodeRole.MASTER, NodeRole.NORMAL, localPlayer.getId(),
                            entry.getValue().getPlayer().getId(), snakeModel);
                    MessageWrapper messageWrapper = Builders.buildMessageWrapper(changeRoleMsg, address.getAddress(), address.getPort());
                    networkController.sendMessageWithWaitAck(changeRoleMsg.getMsgSeq(), messageWrapper);
                });

        if (frameCalculatorTimer == null) {
            nextFrameCalculator = new NextFrameCalculator(snakeModel, foodsGenerator, this);
            frameCalculatorTimer = new Timer();
            frameCalculatorTimer.scheduleAtFixedRate(nextFrameCalculator, 0, snakeModel.getTickDelay());
        }
        networkController.sendAnnounce();
    }

    private void masterTimeoutOnNormal() {
        log.info("master disconnect by timeout on normal");
        InetSocketAddress deputyAddress = snakeModel.getDeputyAddress();
        if (deputyAddress != null) {
            replaceMaster(deputyAddress);
        } else {
            /* ждем сообщения о смене роли */
        }
    }

    private void replaceMaster(InetSocketAddress newMasterAddress) {
        InetSocketAddress oldAddress = snakeModel.getMasterAddress();
        snakeModel.setMasterAddress(newMasterAddress);
        networkController.changeAckDestination(oldAddress, newMasterAddress);
    }
}
