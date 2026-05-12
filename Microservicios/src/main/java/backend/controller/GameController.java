package backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import backend.dto.*;
import backend.service.MatchmakingService;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final MatchmakingService matchmakingService;

    public GameController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK - WebSocket endpoint: ws://localhost:8080/ws/game");
    }

    @PostMapping("/create")
    public ResponseEntity<GameResponseDTO> createGame(@RequestBody CreateGameRequestDTO request) {
        GameResponseDTO response = matchmakingService.createGame(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GameListDTO> listGames() {
        GameListDTO games = matchmakingService.listGames();
        return ResponseEntity.ok(games);
    }

    @PostMapping("/{port}/join")
    public ResponseEntity<GameResponseDTO> joinGame(
            @PathVariable int port,
            @RequestBody JoinGameRequestDTO request) {
        try {
            GameResponseDTO response = matchmakingService.joinGame(port, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{port}")
    public ResponseEntity<Void> deleteGame(@PathVariable int port) {
        matchmakingService.deleteGame(port);
        return ResponseEntity.noContent().build();
    }
}