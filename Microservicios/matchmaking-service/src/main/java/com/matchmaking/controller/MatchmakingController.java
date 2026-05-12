package com.matchmaking.controller;

import com.matchmaking.dto.*;
import com.matchmaking.service.MatchmakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/games")
public class MatchmakingController {
    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/create")
    public ResponseEntity<GameResponse> createGame(@RequestBody CreateGameRequest request) {
        try {
            var game = matchmakingService.createGame(
                request.getGameName(),
                request.getHostName(),
                request.getHostUserId(),
                request.getMaxPlayers()
            );
            return ResponseEntity.ok(new GameResponse(
                game.getId(), game.getGameName(), game.getHostName(),
                game.getMaxPlayers(), game.getCurrentPlayers(), game.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<GameResponse>> listGames() {
        var games = matchmakingService.listGames();
        var responses = games.stream()
            .map(g -> new GameResponse(g.getId(), g.getGameName(), g.getHostName(),
                g.getMaxPlayers(), g.getCurrentPlayers(), g.getStatus()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable Long gameId) {
        try {
            var game = matchmakingService.getGame(gameId);
            return ResponseEntity.ok(new GameResponse(
                game.getId(), game.getGameName(), game.getHostName(),
                game.getMaxPlayers(), game.getCurrentPlayers(), game.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameResponse> joinGame(
            @PathVariable Long gameId,
            @RequestBody JoinGameRequest request) {
        try {
            var game = matchmakingService.joinGame(gameId, request.getPlayerName(), request.getUserId());
            return ResponseEntity.ok(new GameResponse(
                game.getId(), game.getGameName(), game.getHostName(),
                game.getMaxPlayers(), game.getCurrentPlayers(), game.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long gameId) {
        matchmakingService.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }
}