package com.stats.dto;

public class LeaderboardEntry {
    private Long userId;
    private int wins;
    private int losses;
    private int kills;
    private int gamesPlayed;
    private int rank;

    public LeaderboardEntry() {}

    public LeaderboardEntry(Long userId, int wins, int losses, int kills, int gamesPlayed, int rank) {
        this.userId = userId;
        this.wins = wins;
        this.losses = losses;
        this.kills = kills;
        this.gamesPlayed = gamesPlayed;
        this.rank = rank;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}