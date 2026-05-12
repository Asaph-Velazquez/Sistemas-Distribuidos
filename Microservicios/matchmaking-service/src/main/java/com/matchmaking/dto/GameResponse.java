package com.matchmaking.dto;

public class GameResponse {
    private Long id;
    private String gameName;
    private String hostName;
    private int maxPlayers;
    private int currentPlayers;
    private String status;

    public GameResponse() {}

    public GameResponse(Long id, String gameName, String hostName, int maxPlayers, int currentPlayers, String status) {
        this.id = id;
        this.gameName = gameName;
        this.hostName = hostName;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}