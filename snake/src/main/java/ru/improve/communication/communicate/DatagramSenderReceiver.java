package ru.improve.communication.communicate;

import lombok.extern.log4j.Log4j2;
import ru.improve.model.MessageWrapper;
import ru.improve.model.SnakeConstants;
import ru.improve.model.SnakeModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static ru.improve.model.SnakeConstants.DATAGRAM_BUFFER_SIZE;

@Log4j2
public class DatagramSenderReceiver {

    private SnakeModel snakeModel;

    private InetAddress multicastAddress;
    private int multicastPort;

    private DatagramSocket datagramSocket;
    private MulticastSocket multicastSocket;

    private BlockingQueue<DatagramPacket> sendDatagrams = new LinkedBlockingQueue<>();
    private BlockingQueue<DatagramPacket> receiveDatagrams = new LinkedBlockingQueue<>();

    private Thread sendDatagramThread;
    private Thread receiveDatagramThread;
    private Thread receiveMulticastThread;

    private boolean continueSendingReceiving = true;

    public DatagramSenderReceiver(DatagramSocket datagramSocket, MulticastSocket multicastSocket, SnakeModel snakeModel) {
        this.snakeModel = snakeModel;
        this.datagramSocket = datagramSocket;
        this.multicastSocket = multicastSocket;
        this.multicastAddress = snakeModel.getMulticastAddress();
        this.multicastPort = snakeModel.getMulticastPort();

        this.sendDatagramThread = new Thread(() -> sendingDatagram());
        this.receiveDatagramThread = new Thread(() -> receivingDatagrams());
        this.receiveMulticastThread = new Thread(() -> receivingMulticast());
    }

    public void initial() {
        sendDatagramThread.start();
        receiveDatagramThread.start();
        receiveMulticastThread.start();
    }

    public void stop() {
        continueSendingReceiving = false;
        sendDatagramThread.interrupt();
        receiveDatagramThread.interrupt();
        receiveMulticastThread.interrupt();
    }

    public void sendUnicastDatagram(MessageWrapper messageWrapper) {
        log.debug("send to {}", messageWrapper.getInetSocketAddress().getAddress());
        DatagramPacket datagramPacket = new DatagramPacket(messageWrapper.getMessage(), messageWrapper.getMessageLength(),
                messageWrapper.getInetAddress(), messageWrapper.getPort());
        
        sendDatagrams.add(datagramPacket);
    }

    public void sendMulticastDatagram(byte[] message) {
        DatagramPacket datagramPacket = new DatagramPacket(message, message.length, multicastAddress, multicastPort);
        sendDatagrams.add(datagramPacket);
    }

    public DatagramPacket getReceiverDatagramPacket() throws InterruptedException {
        return receiveDatagrams.take();
    }

    private void sendingDatagram()  {
        while (continueSendingReceiving) {
            try {
                DatagramPacket datagramPacket = sendDatagrams.take();
                datagramSocket.send(datagramPacket);
            } catch (InterruptedException ex) {
                /* не самое элегантное зато рабочее решение */
                break;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void receivingDatagrams() {
        while (continueSendingReceiving) {
            try {
                byte[] receiveBuffer = new byte[SnakeConstants.DATAGRAM_BUFFER_SIZE];
                DatagramPacket datagramPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                datagramSocket.receive(datagramPacket);
                receiveDatagrams.add(datagramPacket);
            } catch (IOException ex) {
                new RuntimeException(ex);
            }
        }
    }

    private void receivingMulticast() {
        while (continueSendingReceiving) {
            try {
                byte[] receiverBuffer = new byte[DATAGRAM_BUFFER_SIZE];
                DatagramPacket datagramPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                multicastSocket.receive(datagramPacket);
                receiveDatagrams.add(datagramPacket);
            } catch (IOException ex) {
                break;
            }
        }
    }
}
