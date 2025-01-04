package ru.improve.controller.network;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.improve.communication.communicate.DatagramSenderReceiver;
import ru.improve.communication.message.SnakesProto;
import ru.improve.model.AnnounceGameData;
import ru.improve.model.SnakeConstants;
import ru.improve.model.SnakeModel;
import ru.improve.util.Builders;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Log4j2
public class AnnounceMsgController {

    private SnakeModel snakeModel;

    private DatagramSenderReceiver datagramSenderReceiver;

    @Getter
    private SendAnnouncementMsgTask sendAnnouncementMsgTask = null;
    private Timer sendAnnouncementMsgTimer = null;

    @Getter
    private CheckReceivedAnnounce checkReceivedAnnounceTask = null;
    private Timer checkReceivedAnnounceTimer = null;

    public AnnounceMsgController(SnakeModel snakeModel, DatagramSenderReceiver datagramSenderReceiver) {
        this.snakeModel = snakeModel;
        this.datagramSenderReceiver = datagramSenderReceiver;
    }

    public void startSendAnnouncementMsg() {
        if (sendAnnouncementMsgTimer == null) {
            sendAnnouncementMsgTask = new SendAnnouncementMsgTask();
            sendAnnouncementMsgTimer = new Timer();
            sendAnnouncementMsgTimer.scheduleAtFixedRate(sendAnnouncementMsgTask, 0, snakeModel.getGameConfig().getStateDelayMs());
        }
    }

    public void startCheckReceivedAnnounce() {
        if (checkReceivedAnnounceTimer == null) {
            checkReceivedAnnounceTask = new CheckReceivedAnnounce();
            checkReceivedAnnounceTimer = new Timer();
            checkReceivedAnnounceTimer.scheduleAtFixedRate(checkReceivedAnnounceTask, 0, snakeModel.getGameConfig().getStateDelayMs());
        }
    }

    public void stopSendAnnouncementMsg() {
        if (sendAnnouncementMsgTimer != null) {
            sendAnnouncementMsgTimer.cancel();
            sendAnnouncementMsgTimer.purge();
            sendAnnouncementMsgTimer = null;
        }

    }

    public void stopCheckReceivedAnnounce() {
        if (checkReceivedAnnounceTimer != null) {
            checkReceivedAnnounceTimer.cancel();
            checkReceivedAnnounceTimer.purge();
            checkReceivedAnnounceTimer = null;
        }
    }

    private class SendAnnouncementMsgTask extends TimerTask {

        @Override
        public void run() {
            SnakesProto.GameMessage gameMessage = Builders.buildAnnouncementMsg(snakeModel);
            datagramSenderReceiver.sendMulticastDatagram(gameMessage.toByteArray());
            log.debug("send game announce with msgSeq {}", gameMessage.getMsgSeq());
        }
    }

    private class CheckReceivedAnnounce extends TimerTask {

        private Map<InetSocketAddress, AnnounceGameData> announceGameTableMap;

        public CheckReceivedAnnounce() {
            this.announceGameTableMap = snakeModel.getAnnounceGameDataMap();
        }

        @Override
        public void run() {
            log.debug("try remove announcement");
            if (!announceGameTableMap.isEmpty()) {
                announceGameTableMap.entrySet().stream()
                        .filter(entry -> System.currentTimeMillis() - entry.getValue().getLastAnnounceTime() > SnakeConstants.ANNOUNCE_SEND_DELAY)
                        .forEach(entry -> announceGameTableMap.remove(entry.getKey()));
            }
        }
    }
}
