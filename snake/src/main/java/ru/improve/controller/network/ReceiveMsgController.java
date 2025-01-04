package ru.improve.controller.network;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.log4j.Log4j2;
import ru.improve.communication.communicate.DatagramSenderReceiver;
import ru.improve.communication.message.SnakesProto.GameAnnouncement;
import ru.improve.communication.message.SnakesProto.GameMessage;
import ru.improve.communication.message.SnakesProto.NodeRole;
import ru.improve.controller.GameController;
import ru.improve.controller.NetworkController;
import ru.improve.controller.gameUtils.MovementController;
import ru.improve.model.AnnounceGameData;
import ru.improve.model.MessageWrapper;
import ru.improve.model.SnakeModel;
import ru.improve.model.enums.GameStatus;
import ru.improve.model.gameState.PlayerData;
import ru.improve.util.Builders;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Map;

@Log4j2
public class ReceiveMsgController implements Runnable {

    private SnakeModel snakeModel;

    private DatagramSenderReceiver datagramSenderReceiver;

    private NetworkController networkController;

    private GameController gameController;
    private MovementController movementController;

    private boolean continueManageReceivePackage = true;

    public ReceiveMsgController(SnakeModel snakeModel, NetworkController networkController,
                                GameController gameController, MovementController movementController) {

        this.snakeModel = snakeModel;
        this.datagramSenderReceiver = networkController.getDatagramSenderReceiver();
        this.networkController = networkController;
        this.gameController = gameController;
        this.movementController = movementController;
    }

    public void stop() {
        continueManageReceivePackage = false;
    }

    @Override
    public void run() {
        takeReceivePackage();
    }

    private void takeReceivePackage() {
        while (continueManageReceivePackage) {
            DatagramPacket datagramPacket;
            try {
                datagramPacket = datagramSenderReceiver.getReceiverDatagramPacket();
            } catch (InterruptedException ex) {
                break;
            }

            if (datagramPacket == null) continue;

            MessageWrapper messageWrapper = MessageWrapper.builder()
                    .inetAddress(datagramPacket.getAddress())
                    .port(datagramPacket.getPort())
                    .message(datagramPacket.getData())
                    .messageLength(datagramPacket.getLength())
                    .build();
            manageReceivePackage(messageWrapper);
        }
    }

    private void manageReceivePackage(MessageWrapper messageWrapper) {
        try {
            GameMessage gameMessage = messageWrapper.getMessageData();

            log.debug("receive message {} {}", messageWrapper.getInetAddress(), messageWrapper.getPort());

            switch (gameMessage.getTypeCase()) {
                case ANNOUNCEMENT -> manageAnnouncementMsg(messageWrapper);
                case ACK -> manageAckMsg(messageWrapper);
                case PING -> managePingMsg(messageWrapper);
                case JOIN -> manageJoinMsg(messageWrapper);
                case ERROR -> manageErrorMsg(messageWrapper);
                case STATE -> manageStateMsg(messageWrapper);
                case STEER -> manageSteerMsg(messageWrapper);
                case ROLE_CHANGE -> manageRoleChangeMsg(messageWrapper);
                case DISCOVER -> log.info("присоединению и так происходит хорошо, так что это не работает");
                case TYPE_NOT_SET -> log.info("отправитель косячит");
            }
        } catch (InvalidProtocolBufferException ex) {
            ex.printStackTrace();
        }
    }

    private void manageAnnouncementMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        if (snakeModel.getGameStatus() != GameStatus.PLAY) {
            log.debug("receive announcement message with msgSeq {}", gameMessage.getMsgSeq());
            Map<InetSocketAddress, AnnounceGameData> gameDataMap = snakeModel.getAnnounceGameDataMap();
            GameAnnouncement gameAnnouncement = gameMessage.getAnnouncement().getGames(0);
            if (!gameDataMap.containsKey(messageWrapper.getInetSocketAddress())) {
                AnnounceGameData announceGameData = AnnounceGameData.builder()
                        .gameConfig(gameAnnouncement.getConfig())
                        .gameName(gameAnnouncement.getGameName())
                        .players(gameAnnouncement.getPlayers().getPlayersList()
                                .stream()
                                .map(gamePlayer -> Builders.buildPlayerFromProto(gamePlayer))
                                .toList()
                        )
                        .canJoin(gameAnnouncement.getCanJoin())
                        .lastAnnounceTime(System.currentTimeMillis())
                        .build();
                gameDataMap.put(messageWrapper.getInetSocketAddress(), announceGameData);
            } else {
                gameDataMap.get(messageWrapper.getInetSocketAddress()).setLastAnnounceTime(System.currentTimeMillis());
            }
        }
    }

    private void manageAckMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        log.debug("receive ack msg with seq: {}", gameMessage.getMsgSeq());
        gameController.checkConnectWaitAckMsgSeq(gameMessage.getMsgSeq(), messageWrapper);
        networkController.deleteAck(gameMessage.getMsgSeq());
        updatePlayerLastMessageReceiveTime(messageWrapper);
    }

    private void managePingMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();

        PlayerData sendPlayer = snakeModel.getPlayer(messageWrapper.getInetSocketAddress());
        PlayerData playerData = snakeModel.getLocalPlayer();
        /* эта проверка нужна, когда мы вышли из игры, вся модель очистилась, но пакеты все равно приходят */
        /* вообще, было бы неплохо это добавить везде, то там другие проверки есть */
        if (sendPlayer != null && playerData != null) {
            log.debug("receive ping msg with seq: {}", gameMessage.getMsgSeq());
            sendPlayer.setLmrt(System.currentTimeMillis());
            GameMessage ackMessage = Builders.buildAckMsg(playerData.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq());
            MessageWrapper ackMessageWrapper = Builders.buildMessageWrapper(ackMessage, messageWrapper.getInetAddress(), messageWrapper.getPort());
            networkController.sendMessageWithoutAck(ackMessageWrapper);
        }
    }

    private void manageJoinMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        if (snakeModel.getGameStatus() == GameStatus.PLAY && snakeModel.getLocalPlayer().getNodeRole() == NodeRole.MASTER) {
            log.info("receive join message with seq {}", gameMessage.getMsgSeq());
            gameController.acceptRemotePlayer(messageWrapper);
            updatePlayerLastMessageReceiveTime(messageWrapper);
        }
    }

    private void manageErrorMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        log.info("надеюсь, я никогда сюда не попаду receive error msg with seq: {}", gameMessage.getMsgSeq());
        /* что я дожен отправить на ошибку, когда мне не еще выдали айдишник */
        GameMessage ackMessage = Builders.buildAckMsg(-1, gameMessage.getSenderId(), gameMessage.getMsgSeq());
        MessageWrapper ackMessageWrapper = Builders.buildMessageWrapper(ackMessage, messageWrapper.getInetAddress(), messageWrapper.getPort());
        networkController.sendMessageWithoutAck(ackMessageWrapper);
    }

    private void manageStateMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        if (snakeModel.getGameStatus() == GameStatus.PLAY && snakeModel.getLocalPlayer().getNodeRole() != NodeRole.MASTER) {
            gameController.changeGameState(messageWrapper);

            GameMessage gameMessage = messageWrapper.getMessageData();
            GameMessage ackMessage = Builders.buildAckMsg(snakeModel.getLocalPlayer().getId(),
                    snakeModel.getPlayer(messageWrapper.getInetSocketAddress()).getId(),
                    gameMessage.getMsgSeq());

            MessageWrapper ackMessageWrapper = Builders.buildMessageWrapper(ackMessage, messageWrapper.getInetAddress(), messageWrapper.getPort());
            networkController.sendMessageWithoutAck(ackMessageWrapper);
            updatePlayerLastMessageReceiveTime(messageWrapper);
        }
    }

    private void manageSteerMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        if (snakeModel.getGameStatus() == GameStatus.PLAY && snakeModel.getLocalPlayer().getNodeRole() == NodeRole.MASTER) {
            movementController.changeRemoteDirection(messageWrapper);

            GameMessage gameMessage = messageWrapper.getMessageData();
            log.info("receive steer msg with seq: {}", gameMessage.getMsgSeq());
            GameMessage ackMessage = Builders.buildAckMsg(snakeModel.getLocalPlayer().getId(),
                    snakeModel.getPlayer(messageWrapper.getInetSocketAddress()).getId(),
                    gameMessage.getMsgSeq());

            MessageWrapper ackMessageWrapper = Builders.buildMessageWrapper(ackMessage, messageWrapper.getInetAddress(), messageWrapper.getPort());
            networkController.sendMessageWithoutAck(ackMessageWrapper);
            updatePlayerLastMessageReceiveTime(messageWrapper);
        }
    }

    private void manageRoleChangeMsg(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        log.info("receive roleChange msg with seq: {}", gameMessage.getMsgSeq());
        gameController.changeRoleManage(messageWrapper);
        updatePlayerLastMessageReceiveTime(messageWrapper);
    }

    private void updatePlayerLastMessageReceiveTime(MessageWrapper messageWrapper) {
        snakeModel.getPlayer(messageWrapper.getInetSocketAddress()).setLmrt(System.currentTimeMillis());
    }
}
