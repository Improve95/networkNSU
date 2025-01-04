package ru.improve.model;

import lombok.Data;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

@Data
public class ServerModel {

    private Map<SocketChannel, ClientData> clients = new HashMap<>();
}
