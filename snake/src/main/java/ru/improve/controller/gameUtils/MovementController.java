package ru.improve.controller.gameUtils;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import ru.improve.communication.message.SnakesProto.Direction;
import ru.improve.communication.message.SnakesProto.GameMessage;
import ru.improve.communication.message.SnakesProto.GameMessage.*;
import ru.improve.communication.message.SnakesProto.NodeRole;
import ru.improve.controller.NetworkController;
import ru.improve.model.MessageWrapper;
import ru.improve.model.PlayerSnakePair;
import ru.improve.model.SnakeModel;
import ru.improve.model.gameState.SnakeData;
import ru.improve.util.Builders;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class MovementController {

    private SnakeModel snakeModel;

    private NetworkController networkController;

    private Map<InetSocketAddress, DirectionPriority> directionPriorityMap = new ConcurrentHashMap<>();

    public MovementController(SnakeModel snakeModel, NetworkController networkController) {
        this.snakeModel = snakeModel;
        this.networkController = networkController;
    }

    public void changeRemoteDirection(MessageWrapper messageWrapper) throws InvalidProtocolBufferException {
        GameMessage gameMessage = messageWrapper.getMessageData();
        SteerMsg steerMsg = gameMessage.getSteer();
        Direction newDirection = steerMsg.getDirection();
        if (snakeModel.getPlayer(messageWrapper.getInetSocketAddress()).getNodeRole() != NodeRole.VIEWER) {
            if (!directionPriorityMap.containsKey(messageWrapper.getInetSocketAddress())) {
                directionPriorityMap.put(messageWrapper.getInetSocketAddress(), new DirectionPriority(newDirection, gameMessage.getMsgSeq()));
            } else {
                DirectionPriority prevDirection = directionPriorityMap.get(messageWrapper.getInetSocketAddress());
                if (prevDirection.msgSeq <= gameMessage.getMsgSeq()) {
                    prevDirection.setDirection(newDirection);
                }
            }
            snakeModel.getSnake(messageWrapper.getInetSocketAddress()).setHeadDirection(newDirection);
        }
    }

    public void gotoUp() {
        if (snakeModel.getLocalSnake().getHeadDirection() != Direction.DOWN) {
            changeLocalDirection(Direction.UP);
        }
    }

    public void gotoLeft() {
        if (snakeModel.getLocalSnake().getHeadDirection() != Direction.RIGHT) {
            changeLocalDirection(Direction.LEFT);
        }
    }

    public void gotoDown() {
        if (snakeModel.getLocalSnake().getHeadDirection() != Direction.UP) {
            changeLocalDirection(Direction.DOWN);
        }
    }

    public void gotoRight() {
        if (snakeModel.getLocalSnake().getHeadDirection() != Direction.LEFT) {
            changeLocalDirection(Direction.RIGHT);
        }
    }

    private void changeLocalDirection(Direction direction) {
        PlayerSnakePair playerSnakePair = snakeModel.getLocalPlayerSnakePair();
        if (playerSnakePair.getPlayer().getNodeRole() == NodeRole.MASTER) {
            SnakeData snakeData = playerSnakePair.getSnakeData();
            if (snakeData.getHeadDirection() != direction) {
                snakeData.setHeadDirection(direction);
            }
        } else if (playerSnakePair.getPlayer().getNodeRole() == NodeRole.DEPUTY ||
                playerSnakePair.getPlayer().getNodeRole() == NodeRole.NORMAL) {

            GameMessage steerMsg = Builders.buildSteerMsg(direction, snakeModel);
            MessageWrapper messageWrapper = Builders.buildMessageWrapper(steerMsg,
                    snakeModel.getMasterAddress().getAddress(), snakeModel.getMasterAddress().getPort());
            networkController.sendMessageWithWaitAck(steerMsg.getMsgSeq(), messageWrapper);
        }
    }

    @Data
    private class DirectionPriority {

        public DirectionPriority(Direction direction, long msgSeq) {
            this.direction = direction;
            this.msgSeq = msgSeq;
        }

        private Direction direction;

        private long msgSeq;
    }
}
