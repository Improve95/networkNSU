package ru.improve.util;

import ru.improve.communication.message.SnakesProto.Direction;
import ru.improve.communication.message.SnakesProto.GameAnnouncement;
import ru.improve.communication.message.SnakesProto.GameMessage;
import ru.improve.communication.message.SnakesProto.GameMessage.AckMsg;
import ru.improve.communication.message.SnakesProto.GameMessage.ErrorMsg;
import ru.improve.communication.message.SnakesProto.GameMessage.JoinMsg;
import ru.improve.communication.message.SnakesProto.GameMessage.PingMsg;
import ru.improve.communication.message.SnakesProto.GameMessage.RoleChangeMsg;
import ru.improve.communication.message.SnakesProto.GameMessage.StateMsg;
import ru.improve.communication.message.SnakesProto.GameMessage.SteerMsg;
import ru.improve.communication.message.SnakesProto.GamePlayer;
import ru.improve.communication.message.SnakesProto.GamePlayers;
import ru.improve.communication.message.SnakesProto.GameState;
import ru.improve.communication.message.SnakesProto.NodeRole;
import ru.improve.model.AnnounceGameData;
import ru.improve.model.FieldData;
import ru.improve.model.MessageWrapper;
import ru.improve.model.SnakeModel;
import ru.improve.model.gameState.Coord;
import ru.improve.model.gameState.PlayerData;
import ru.improve.model.gameState.SnakeData;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class Builders {

    public static Coord buildPoint(FieldData fieldData, int x, int y) {
        return new Coord(fieldData.calculateCoordX(x), fieldData.calculateCoordY(y));
    }

    public static GameMessage buildAnnouncementMsg(SnakeModel snakeModel) {
        List<GamePlayer> gamePlayers = snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                .map(entry -> {
                    PlayerData playerData = entry.getValue().getPlayer();
                    return GamePlayer.newBuilder()
                            .setName(playerData.getName())
                            .setId(playerData.getId())
                            .setRole(playerData.getNodeRole())
                            .setScore(playerData.getScore())
                            .build();
                }).collect(Collectors.toList());

        GameAnnouncement announcement = GameAnnouncement.newBuilder()
                .setPlayers(GamePlayers.newBuilder().addAllPlayers(gamePlayers).build())
                .setConfig(snakeModel.getGameConfig())
                .setCanJoin(true)
                .setGameName(snakeModel.getGameName())
                .build();

        return GameMessage.newBuilder()
                .setAnnouncement(
                        GameMessage.AnnouncementMsg.newBuilder().addGames(announcement).build()
                )
                .setMsgSeq(snakeModel.getAndIncrementMsgSeq())
                .build();
    }

    public static PlayerData buildPlayerFromProto(GamePlayer gamePlayer) {
        return PlayerData.builder()
                .id(gamePlayer.getId())
                .name(gamePlayer.getName())
                .ipAddress(gamePlayer.getIpAddress())
                .port(gamePlayer.getPort())
                .nodeRole(gamePlayer.getRole())
                .score(gamePlayer.getScore())
                .build();
    }

    public static GameMessage buildJoinMsg(AnnounceGameData announceGameData, PlayerData playerData, SnakeModel snakeModel) {
        JoinMsg joinMsg = JoinMsg.newBuilder()
                .setPlayerType(playerData.getPlayerType())
                .setPlayerName(playerData.getName())
                .setGameName(announceGameData.getGameName())
                .setRequestedRole(playerData.getNodeRole())
                .build();

        GameMessage gameMessage = GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(snakeModel.getAndIncrementMsgSeq())
                .build();

        return gameMessage;
    }

    public static GameMessage buildAckMsg(int senderId, int receiverId, long msgSeq) {
        return GameMessage.newBuilder()
                .setAck(AckMsg.newBuilder().build())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(msgSeq)
                .build();
    }

    public static GameMessage buildErrorMsg(String message, long msgSeq) {
        return GameMessage.newBuilder()
                .setError(ErrorMsg.newBuilder().setErrorMessage(message).build())
                .setMsgSeq(msgSeq)
                .build();
    }

    public static GameMessage buildGameStateMsg(SnakeModel snakeModel) {
        List<GameState.Snake> protoSnakes = snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                .map(entry -> {
                    PlayerData playerData = entry.getValue().getPlayer();
                    SnakeData snakeData = entry.getValue().getSnakeData();

                    return GameState.Snake.newBuilder()
                            .setPlayerId(playerData.getId())
                            .addAllPoints(snakeData.getPoints().stream()
                                    .map(point -> GameState.Coord.newBuilder()
                                            .setX(point.getX())
                                            .setY(point.getY())
                                            .build())
                                    .toList()
                            )
                            .setState(snakeData.getSnakeState())
                            .setHeadDirection(snakeData.getHeadDirection())
                            .build();
                }).toList();

        List<GameState.Coord> protoFoods = snakeModel.getFoods().stream()
                .map(point -> GameState.Coord.newBuilder()
                        .setX(point.getX())
                        .setY(point.getY())
                        .build())
                .toList();

        List<GamePlayer> protoPlayers = snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                .map(entry -> {
                    PlayerData playerData = entry.getValue().getPlayer();
                    return GamePlayer.newBuilder()
                            .setName(playerData.getName())
                            .setId(playerData.getId())
                            .setIpAddress(playerData.getIpAddress())
                            .setPort(playerData.getPort())
                            .setRole(playerData.getNodeRole())
                            .setType(playerData.getPlayerType())
                            .setScore(playerData.getScore())
                            .build();
                })
                .toList();

        GameState gameState = GameState.newBuilder()
                .setStateOrder(snakeModel.getGameStateOrder())
                .addAllSnakes(protoSnakes)
                .addAllFoods(protoFoods)
                .setPlayers(GamePlayers.newBuilder().addAllPlayers(protoPlayers).build())
                .build();

        return GameMessage.newBuilder()
                .setMsgSeq(snakeModel.getAndIncrementMsgSeq())
                .setState(StateMsg.newBuilder().setState(gameState).build())
                .build();
    }

    public static List<PlayerData> buildPlayersFromSnakeProto(List<GamePlayer> protoGamePlayers) {
        return protoGamePlayers.stream()
                .map(gamePlayer -> PlayerData.builder()
                        .name(gamePlayer.getName())
                        .id(gamePlayer.getId())
                        .ipAddress(gamePlayer.getIpAddress())
                        .port(gamePlayer.getPort())
                        .nodeRole(gamePlayer.getRole())
                        .playerType(gamePlayer.getType())
                        .score(gamePlayer.getScore())
                        .build()
                ).toList();
    }

    public static List<SnakeData> buildSnakesFromSnakeProto(List<GameState.Snake> protoSnakes) {
        return protoSnakes.stream()
                .map(snake -> {
                    Deque<Coord> snakePoints = new ArrayDeque<>();
                    snake.getPointsList().stream()
                            .map(protoCoord -> new Coord(protoCoord.getX(), protoCoord.getY()))
                            .forEach(coord -> snakePoints.add(coord));

                    return SnakeData.builder()
                            .playerId(snake.getPlayerId())
                            .points(snakePoints)
                            .snakeState(snake.getState())
                            .headDirection(snake.getHeadDirection())
                            .build();
                        }
                )
                .toList();
    }

    public static List<Coord> buildFoodsFromFoodsProto(List<GameState.Coord> protoFoods) {
        return protoFoods.stream()
                .map(protoCoord -> new Coord(protoCoord.getX(), protoCoord.getY()))
                .toList();
    }

    public static GameMessage buildSteerMsg(Direction direction, SnakeModel snakeModel) {
        SteerMsg steerMsg = SteerMsg.newBuilder()
                .setDirection(direction)
                .build();

        return GameMessage.newBuilder()
                .setSteer(steerMsg)
                .setSenderId(snakeModel.getLocalPlayer().getId())
                .setReceiverId(snakeModel.getMasterPlayer().getId())
                .setMsgSeq(snakeModel.getAndIncrementMsgSeq())
                .build();
    }

    public static GameMessage buildPingMsg(SnakeModel snakeModel) {
        return GameMessage.newBuilder()
                .setPing(PingMsg.newBuilder().build())
                .setMsgSeq(snakeModel.getAndIncrementMsgSeq())
                .build();
    }

    public static GameMessage buildChangeRoleMsg(NodeRole senderNodeRole, NodeRole receiverNodeRole,
                                                 int senderId, int receiverId, SnakeModel snakeModel) {

        RoleChangeMsg roleChangeMsg = RoleChangeMsg.newBuilder()
                .setSenderRole(senderNodeRole)
                .setReceiverRole(receiverNodeRole)
                .build();

        return GameMessage.newBuilder()
                .setRoleChange(roleChangeMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(snakeModel.getAndIncrementMsgSeq())
                .build();
    }

    public static MessageWrapper buildMessageWrapper(GameMessage message, InetAddress inetAddress, int port) {
        return MessageWrapper.builder()
                .inetAddress(inetAddress)
                .port(port)
                .message(message.toByteArray())
                .messageLength(message.toByteArray().length)
                .build();
    }
}