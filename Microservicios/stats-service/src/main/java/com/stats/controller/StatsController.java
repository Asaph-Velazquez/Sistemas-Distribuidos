package com.stats.controller;

import com.stats.dto.LeaderboardEntry;
import com.stats.entity.UserStats;
import com.stats.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stats")
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserStats> getUserStats(@PathVariable Long userId) {
        return ResponseEntity.ok(statsService.getUserStats(userId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        List<UserStats> stats = statsService.getLeaderboard();
        List<LeaderboardEntry> entries = new java.util.ArrayList<>();
        int rank = 1;
        for (UserStats s : stats) {
            entries.add(new LeaderboardEntry(
                s.getUserId(), s.getWins(), s.getLosses(),
                s.getKills(), s.getGamesPlayed(), rank++));
        }
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/user/{userId}/win")
    public ResponseEntity<Void> recordWin(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int kills) {
        statsService.recordWin(userId, kills);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/{userId}/loss")
    public ResponseEntity<Void> recordLoss(@PathVariable Long userId) {
        statsService.recordLoss(userId);
        return ResponseEntity.ok().build();
    }
}