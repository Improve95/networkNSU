package ru.improve.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.improve.communication.message.SnakesProto.GameConfig;
import ru.improve.model.enums.GameStatus;
import ru.improve.model.enums.ViewState;
import ru.improve.model.gameState.Coord;
import ru.improve.model.gameState.PlayerData;
import ru.improve.model.gameState.SnakeData;
import ru.improve.util.Property;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class SnakeModel {

    /* данные из конфига */
    private static final Properties properties = Property.getInstance();

    private int blockSize;
    private GameConfig gameConfig;

    private InetAddress multicastAddress;
    private int multicastPort;

    private String gameName;

    /* данные игры */
    private final int FPS = 30;
    private int tickDelay;

    private FieldData fieldData = new FieldData();

    @Setter
    private ViewState viewState = ViewState.INITIALIZE;

    @Setter
    private GameStatus gameStatus = GameStatus.WAIT;

    @Setter
    private InetSocketAddress masterAddress = null;

    @Setter
    private InetSocketAddress deputyAddress = null;

    private String localPlayerName;
    @Setter
    private InetSocketAddress localAddress;
    @Setter
    private PlayerData localPlayer;

//    @Getter(AccessLevel.PRIVATE)
    private AtomicInteger nextFreePlayerId = new AtomicInteger();

    private Map<InetSocketAddress, PlayerSnakePair> playerSnakeDataMap = new ConcurrentHashMap<>();

    @Setter
    private List<Coord> foods = new ArrayList<>();

    private AtomicInteger gameStateOrder = new AtomicInteger();

    /* === msgSeq для отправителя, каждое сообщение уникальное хотя бы из-за msgSeq === */
    @Getter(AccessLevel.PRIVATE)
    private AtomicLong msgSeq = new AtomicLong();

    /* InetSocketAddress master сервера и игра к которой хотим присоединиться */
    private Map<InetSocketAddress, AnnounceGameData> announceGameDataMap = new ConcurrentHashMap<>();

    public SnakeModel() {
        this.blockSize = Integer.parseInt(properties.getProperty("snake.block.size"));
        this.gameName = properties.getProperty("snake.game.name");
        this.localPlayerName = properties.getProperty("snake.player.name");
        this.localAddress = SnakeConstants.LOCAL_PLAYER_ADDRESS;
        this.tickDelay = Integer.parseInt(properties.getProperty("snake.frame.delay"));
        this.gameConfig = GameConfig.newBuilder()
                .setWidth(Integer.parseInt(properties.getProperty("snake.field.width")))
                .setHeight(Integer.parseInt(properties.getProperty("snake.field.height")))
                .setFoodStatic(Integer.parseInt(properties.getProperty("snake.food.static")))
                .setStateDelayMs(Integer.parseInt(properties.getProperty("snake.state_delay.ms")))
                .build();

        this.multicastPort = Integer.parseInt(properties.getProperty("multicast.port"));
        try {
            this.multicastAddress = InetAddress.getByName(properties.getProperty("multicast.group"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<SnakeData> getAllSnakesCopyOnWriteList() {
        return new CopyOnWriteArrayList<>(playerSnakeDataMap.entrySet().stream()
                /* по идее эта проверка не нужна, но пусть будет */
                .filter(entry -> entry.getValue().getPlayer() != null && entry.getValue().getSnakeData() != null)
                .map(entry -> entry.getValue().getSnakeData())
                .collect(Collectors.toList()));
    }

    public List<Coord> getAllFoodsCopyOnWriteList() {
        return new CopyOnWriteArrayList<>(foods);
    }

    public List<PlayerSnakePair> getAllPlayerSnakePairs() {
        return playerSnakeDataMap.entrySet().stream()
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
    }

    public long getAndIncrementMsgSeq() {
        return msgSeq.getAndIncrement();
    }

    public int getAndIncrementPlayerId() {
        return nextFreePlayerId.getAndIncrement();
    }

    public int getGameStateOrder() {
        return gameStateOrder.get();
    }

    public int getAndIncrementGameStateOrder() {
        return gameStateOrder.getAndIncrement();
    }

    public void setGameStateOrder(int stateOrder) {
        gameStateOrder.set(stateOrder);
    }

    public int getTotalFoodNumber() {
        return gameConfig.getFoodStatic() + playerSnakeDataMap.size();
    }

    /* надо помнить что в начальный момент игры, существует только локальный игрок в модели
    * и у него нет змейки в playerSnakeDataMap, так делать, конечно, не стоит, но за то я это подписал */
    public PlayerData getLocalPlayer() {
        return localPlayer;
    }

    public SnakeData getLocalSnake() {
        PlayerSnakePair pair = playerSnakeDataMap.get(localAddress);
        return pair != null ? pair.getSnakeData() : null;
    }

    public PlayerSnakePair getLocalPlayerSnakePair() {
        return playerSnakeDataMap.get(localAddress);
    }

    public PlayerData getMasterPlayer() {
        PlayerSnakePair pair = playerSnakeDataMap.get(masterAddress);
        return pair != null ? pair.getPlayer() : null;
    }

    public PlayerData getPlayer(InetSocketAddress inetSocketAddress) {
        PlayerSnakePair pair = playerSnakeDataMap.get(inetSocketAddress);
        return pair != null ? pair.getPlayer() : null;
    }

    public SnakeData getSnake(InetSocketAddress inetSocketAddress) {
        return playerSnakeDataMap.get(inetSocketAddress).getSnakeData();
    }

    public PlayerSnakePair getPlayerSnakePair(InetSocketAddress inetSocketAddress) {
        return playerSnakeDataMap.get(inetSocketAddress);
    }

    public void putPlayerSnakePair(InetSocketAddress inetSocketAddress, PlayerSnakePair pair) {
        playerSnakeDataMap.put(inetSocketAddress, pair);
    }

    public boolean containsPlayerSnakeMap(InetSocketAddress inetSocketAddress) {
        return playerSnakeDataMap.containsKey(inetSocketAddress);
    }

    public void clearPlayerSnakeDataMap() {
        playerSnakeDataMap.clear();
    }
}
