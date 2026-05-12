package com.profile.dto;

public class ProfileDTO {
    private Long userId;
    private String displayName;
    private String avatar;
    private int totalGames;
    private int wins;
    private int losses;

    public ProfileDTO() {}

    public ProfileDTO(Long userId, String displayName, String avatar, int totalGames, int wins, int losses) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatar = avatar;
        this.totalGames = totalGames;
        this.wins = wins;
        this.losses = losses;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
}