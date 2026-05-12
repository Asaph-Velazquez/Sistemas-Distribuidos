package com.stats.service;

import com.stats.entity.UserStats;
import com.stats.repository.UserStatsRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatsService {
    private final UserStatsRepository userStatsRepository;

    public StatsService(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    public UserStats getUserStats(Long userId) {
        return userStatsRepository.findByUserId(userId)
            .orElseGet(() -> createUserStats(userId));
    }

    public UserStats createUserStats(Long userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        return userStatsRepository.save(stats);
    }

    public void recordWin(Long userId, int kills) {
        UserStats stats = getUserStats(userId);
        stats.setWins(stats.getWins() + 1);
        stats.setGamesPlayed(stats.getGamesPlayed() + 1);
        stats.setKills(stats.getKills() + kills);
        userStatsRepository.save(stats);
    }

    public void recordLoss(Long userId) {
        UserStats stats = getUserStats(userId);
        stats.setLosses(stats.getLosses() + 1);
        stats.setGamesPlayed(stats.getGamesPlayed() + 1);
        userStatsRepository.save(stats);
    }

    public List<UserStats> getLeaderboard() {
        return userStatsRepository.findTop10ByOrderByWinsDesc();
    }
}