package backend.service;

import org.springframework.stereotype.Service;
import backend.dto.*;
import backend.core.GameState;
import backend.world.World;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.List;

/**
 * MatchmakingService - Gestiona la creación y listado de partidas
 * 
 * Spring @Service que reemplaza la funcionalidad del MatchmakingServer (socket-based)
 * con una arquitectura REST-friendly. Mantiene el mismo comportamiento de sincronización
 * que el original: ConcurrentHashMap para partidas activas y AtomicInteger para asignación
 * de puertos.
 * 
 * Sincronización:
 *   - ConcurrentHashMap: Almacenamiento thread-safe de GameService por puerto
 *   - AtomicInteger: Generación atómica de puertos únicos
 *   - findAvailablePort(): Mismo algoritmo que MatchmakingServer original
 */
@Service
public class MatchmakingService {
    // Port range for game servers (matches original)
    private static final int MIN_GAME_PORT = 50001;
    private static final int MAX_GAME_PORT = 50100;
    
    // Thread-safe storage: {port -> GameService}
    // Reemplaza ConcurrentHashMap<Integer, GameInfo> del original
    private final ConcurrentHashMap<Integer, GameService> activeGames = new ConcurrentHashMap<>();
    
    // Thread-safe port allocation
    // Mismo comportamiento que el AtomicInteger del MatchmakingServer
    private final AtomicInteger nextGamePort = new AtomicInteger(MIN_GAME_PORT);
    
    /**
     * Creates a new game and assigns it a port
     * Thread-safe: Uses AtomicInteger for port allocation
     * 
     * Equivalente a: MatchmakingServer.processRequest("CREATE:...")
     * Diferencia: No inicia servidor socket separado, usa GameService in-memory
     * 
     * @param request DTO con gameName, username, maxPlayers
     * @return GameResponseDTO con puerto asignado e información de la partida
     * @throws RuntimeException si no hay puertos disponibles
     */
    public GameResponseDTO createGame(CreateGameRequestDTO request) {
        int port = findAvailablePort();
        if (port == -1) {
            throw new RuntimeException("No available ports");
        }
        
        // Create GameService for this game
        World world = new World(20, 10);
        GameState gameState = new GameState(world);
        gameState.setGameName(request.getGameName());
        gameState.setHostName(request.getUsername());
        gameState.setMaxPlayers(request.getMaxPlayers());
        
        GameService gameService = new GameService(gameState);
        
        // Register the game
        activeGames.put(port, gameService);
        
        return new GameResponseDTO(
            port,
            request.getGameName(),
            request.getUsername(),
            request.getMaxPlayers(),
            0  // currentPlayers = 0 hasta que alguien se una via WebSocket
        );
    }
    
    /**
     * Lists all active games
     * Thread-safe: ConcurrentHashMap.entrySet() is a safe snapshot
     * 
     * Equivalente a: MatchmakingServer.processRequest("LIST")
     * 
     * @return GameListDTO con lista de todas las partidas activas
     */
    public GameListDTO listGames() {
        List<GameResponseDTO> games = activeGames.entrySet().stream()
            .map(entry -> {
                int port = entry.getKey();
                GameService service = entry.getValue();
                GameState state = service.getGameState();
                
                return new GameResponseDTO(
                    port,
                    state.getGameName(),
                    state.getHostName(),
                    state.getMaxPlayers(),
                    service.getCurrentPlayerCount()
                );
            })
            .collect(Collectors.toList());
        
        return new GameListDTO(games);
    }
    
    /**
     * Join a game by port
     * Thread-safe: Individual GameService handles synchronization
     * 
     * Equivalente a: MatchmakingServer.processRequest("JOIN:...") + Server.accept()
     * Diferencia: No crea socket, solo agrega jugador al GameService
     * 
     * @param port Puerto de la partida
     * @param request DTO con playerName
     * @return GameResponseDTO con información actualizada de la partida
     * @throws RuntimeException si la partida no existe o está llena
     */
    public GameResponseDTO joinGame(int port, JoinGameRequestDTO request) {
        GameService gameService = activeGames.get(port);
        
        if (gameService == null) {
            throw new RuntimeException("Game not found");
        }
        
        GameState state = gameService.getGameState();
        
        if (gameService.getCurrentPlayerCount() >= state.getMaxPlayers()) {
            throw new RuntimeException("Game is full");
        }
        
        gameService.addPlayer(request.getPlayerName());
        
        return new GameResponseDTO(
            port,
            state.getGameName(),
            state.getHostName(),
            state.getMaxPlayers(),
            gameService.getCurrentPlayerCount()
        );
    }
    
    /**
     * Delete a game
     * Thread-safe: ConcurrentHashMap.remove() is atomic
     * 
     * Equivalente a: MatchmakingServer.processRequest("DELETE:...")
     * 
     * @param port Puerto de la partida a eliminar
     */
    public void deleteGame(int port) {
        activeGames.remove(port);
    }
    
    /**
     * Get GameService for a specific port (used by WebSocket handler)
     * Thread-safe: ConcurrentHashMap.get() is atomic
     * 
     * Método auxiliar para que el WebSocket handler (Task 7) pueda acceder
     * al GameService correspondiente a una partida.
     * 
     * @param port Puerto de la partida
     * @return GameService o null si no existe
     */
    public GameService getGameService(int port) {
        return activeGames.get(port);
    }
    
    /**
     * Find available port (same logic as original MatchmakingServer)
     * Thread-safe: AtomicInteger guarantees unique port assignment
     * 
     * Algoritmo idéntico al del MatchmakingServer.findAvailablePort()
     * - Usa AtomicInteger para incrementación atómica
     * - Reinicia al límite mediante compareAndSet
     * - Verifica disponibilidad en ConcurrentHashMap
     * 
     * @return Puerto disponible o -1 si no hay puertos disponibles
     */
    private int findAvailablePort() {
        // Intentar hasta 100 puertos (rango completo)
        for (int i = 0; i < (MAX_GAME_PORT - MIN_GAME_PORT); i++) {
            int candidatePort = nextGamePort.getAndIncrement();
            
            // Si llegamos al límite, reiniciar
            if (candidatePort > MAX_GAME_PORT) {
                // Sincronización: reset atómico mediante compareAndSet
                nextGamePort.compareAndSet(candidatePort + 1, MIN_GAME_PORT);
                candidatePort = MIN_GAME_PORT;
            }
            
            // Verificar si el puerto ya está en uso
            if (!activeGames.containsKey(candidatePort)) {
                return candidatePort;
            }
        }
        return -1; 
    }
}
