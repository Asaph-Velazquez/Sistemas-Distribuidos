package com.game.core;

import com.game.entities.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private volatile boolean running = true;
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private World world;
    private Long gameId;
    private String status = "ACTIVE";

    public GameState(Long gameId, World world) {
        this.gameId = gameId;
        this.world = world;
    }

    public Long getGameId() { return gameId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getCurrentPlayerCount() { return players.size(); }
    public boolean isRunning() { return running; }
    public void stop() { running = false; }
    public World getWorld() { return world; }

    public synchronized void addPlayer(String sessionId, Player player) {
        players.put(sessionId, player);
    }

    public synchronized void removePlayer(String sessionId) {
        players.remove(sessionId);
    }

    public synchronized Player getPlayer(String sessionId) {
        return players.get(sessionId);
    }

    public synchronized Map<String, Player> getAllPlayers() {
        return new ConcurrentHashMap<>(players);
    }
}