package ru.improve.controller;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.improve.communication.communicate.DatagramSenderReceiver;
import ru.improve.communication.message.SnakesProto;
import ru.improve.communication.message.SnakesProto.GameMessage;
import ru.improve.controller.network.AnnounceMsgController;
import ru.improve.model.MessageWrapper;
import ru.improve.model.SnakeModel;
import ru.improve.util.Builders;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class NetworkController {

    private SnakeModel snakeModel;

    private MulticastSocket multicastSocket;
    private DatagramSocket datagramSocket;

    @Getter
    private DatagramSenderReceiver datagramSenderReceiver;

    private AnnounceMsgController announceMsgController;

    private Timer sendMessageWhichWaitAckTimer;
    private SendMessageWhichWaitAckTask sendMessageWhichWaitAckTask;
    private Map<Long, MessageWrapper> msgSeqWaitAckMessagesMap = new ConcurrentHashMap<>();

    private Timer sendPingMsgTimer;
    private TrySendPingMsgTask sendPingMsgTask;

    public NetworkController(SnakeModel snakeModel) {
        this.snakeModel = snakeModel;
        initialSendersReceivers();
    }

    public void initial() {
        datagramSenderReceiver.initial();

        sendMessageWhichWaitAckTask = new SendMessageWhichWaitAckTask();

        sendMessageWhichWaitAckTimer = new Timer();
        sendMessageWhichWaitAckTimer.schedule(sendMessageWhichWaitAckTask, 0, snakeModel.getGameConfig().getStateDelayMs() / 10);

        announceMsgController = new AnnounceMsgController(snakeModel, datagramSenderReceiver);
        announceMsgController.startCheckReceivedAnnounce();

        sendPingMsgTask = new TrySendPingMsgTask();
        sendPingMsgTimer = null;
    }

    public void stop() {
        if (datagramSenderReceiver != null) {
            datagramSenderReceiver.stop();
        }
        stopOnExitGame();
        if (announceMsgController != null) {
            announceMsgController.stopCheckReceivedAnnounce();
        }

        if (multicastSocket != null) {
            multicastSocket.close();
        }
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    public void stopOnExitGame() {
        if (announceMsgController != null) {
            announceMsgController.stopSendAnnouncementMsg();
        }
        if (sendMessageWhichWaitAckTimer != null) {
            sendMessageWhichWaitAckTimer.cancel();
            sendMessageWhichWaitAckTimer.purge();
        }
        if (sendPingMsgTimer != null) {
            sendPingMsgTimer.cancel();
            sendPingMsgTimer.purge();
        }
    }

    public void sendAnnounce() {
        if (announceMsgController != null) {
            announceMsgController.startSendAnnouncementMsg();
        }
    }

    public void sendMessageWithWaitAck(long msgSeq, MessageWrapper messageWrapper) {
        snakeModel.getLocalPlayer().setLmst(System.currentTimeMillis());
        msgSeqWaitAckMessagesMap.put(msgSeq, messageWrapper);
        datagramSenderReceiver.sendUnicastDatagram(messageWrapper);
    }

    /* через этот метод отправляются все сообщения в том числе и сами ack */
    public void sendMessageWithoutAck(MessageWrapper messageWrapper) {
        snakeModel.getLocalPlayer().setLmst(System.currentTimeMillis());
        datagramSenderReceiver.sendUnicastDatagram(messageWrapper);
    }

    public void deleteAck(long msgSeq) {
        msgSeqWaitAckMessagesMap.remove(msgSeq);
    }

    public void startSendPingMsg() {
        if (sendPingMsgTimer == null) {
            sendPingMsgTimer = new Timer();
            sendPingMsgTimer.scheduleAtFixedRate(sendPingMsgTask, 0, snakeModel.getGameConfig().getStateDelayMs() / 10);
        }
    }

    public void changeAckDestination(InetSocketAddress oldAddress, InetSocketAddress newAddress) {
        msgSeqWaitAckMessagesMap.values().stream()
                .filter(messageWrapper -> messageWrapper.getInetSocketAddress().equals(oldAddress))
                .forEach(messageWrapper -> {
                    messageWrapper.setInetAddress(newAddress.getAddress());
                    messageWrapper.setPort(newAddress.getPort());
                });
    }

    private void initialSendersReceivers() {
        try {
            SocketAddress socketAddress = new InetSocketAddress(snakeModel.getMulticastAddress(), snakeModel.getMulticastPort());
            multicastSocket = new MulticastSocket(snakeModel.getMulticastPort());
            multicastSocket.joinGroup(socketAddress, null);

            datagramSocket = new DatagramSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        datagramSenderReceiver = new DatagramSenderReceiver(datagramSocket, multicastSocket, snakeModel);
    }

    private class SendMessageWhichWaitAckTask extends TimerTask {

        @Override
        public void run() {
            if (!msgSeqWaitAckMessagesMap.isEmpty()) {
                msgSeqWaitAckMessagesMap.entrySet().stream()
                        .forEach(entry -> {
                            MessageWrapper messageTrySend = entry.getValue();
                            if (messageTrySend.getAckResendAttempts() <= 0) {
                                msgSeqWaitAckMessagesMap.remove(entry.getKey());
                            } else {
                                messageTrySend.setAckResendAttempts(messageTrySend.getAckResendAttempts() - 1);
                                datagramSenderReceiver.sendUnicastDatagram(messageTrySend);
                            }
                        });
            }
        }
    }

    private class TrySendPingMsgTask extends TimerTask {

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - snakeModel.getLocalPlayer().getLmst() > snakeModel.getGameConfig().getStateDelayMs() / 10) {
                if (snakeModel.getLocalPlayer().getNodeRole() == SnakesProto.NodeRole.MASTER) {
                    snakeModel.getPlayerSnakeDataMap().entrySet().stream()
                            /* фильтр чтобы не отправлять самому себе ping */
                            .filter(entry -> !entry.getKey().equals(snakeModel.getLocalAddress()))
                            .forEach(entry -> send(entry.getKey()));
                } else {
                    log.info("send ping msg");
                    send(snakeModel.getMasterAddress());
                }
            }
        }

        private void send(InetSocketAddress destAddress) {
            GameMessage pingMsg = Builders.buildPingMsg(snakeModel);
            MessageWrapper messageWrapper = Builders.buildMessageWrapper(pingMsg, destAddress.getAddress(), destAddress.getPort());
            sendMessageWithWaitAck(pingMsg.getMsgSeq(), messageWrapper);
        }
    }
}
