package com.matchmaking.dto;

public class JoinGameRequest {
    private String playerName;
    private Long userId;

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}