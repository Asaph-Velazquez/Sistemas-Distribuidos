package com.matchmaking.service;

import com.matchmaking.entity.Game;
import com.matchmaking.repository.GameRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MatchmakingService {
    private final GameRepository gameRepository;

    public MatchmakingService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game createGame(String gameName, String hostName, Long hostUserId, int maxPlayers) {
        Game game = new Game();
        game.setGameName(gameName);
        game.setHostName(hostName);
        game.setHostUserId(hostUserId);
        game.setMaxPlayers(maxPlayers);
        return gameRepository.save(game);
    }

    public List<Game> listGames() {
        return gameRepository.findByStatus("WAITING");
    }

    public Game getGame(Long gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game not found"));
    }

    public Game joinGame(Long gameId, String playerName, Long userId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Game not found"));
        if (game.getCurrentPlayers() >= game.getMaxPlayers()) {
            throw new RuntimeException("Game is full");
        }
        game.setCurrentPlayers(game.getCurrentPlayers() + 1);
        if (game.getCurrentPlayers() >= game.getMaxPlayers()) {
            game.setStatus("READY");
        }
        return gameRepository.save(game);
    }

    public void leaveGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Game not found"));
        game.setCurrentPlayers(Math.max(0, game.getCurrentPlayers() - 1));
        if (game.getCurrentPlayers() < game.getMaxPlayers()) {
            game.setStatus("WAITING");
        }
        gameRepository.save(game);
    }

    public void deleteGame(Long gameId) {
        gameRepository.deleteById(gameId);
    }
}