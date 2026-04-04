package backend.service;

import backend.core.GameState;
import backend.entities.Player;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GameService - Gestiona el estado de una sola instancia de juego
 * 
 * NO es un @Service de Spring - se crea una instancia por cada juego
 * Reemplaza el Server.java basado en sockets con gestión de estado en memoria
 * 
 * Cada instancia de GameService gestiona:
 *   - Un GameState (mundo, jugadores, tiempo)
 *   - Generación de IDs de jugador (AtomicInteger para seguridad en hilos)
 *   - Ciclo de vida de los jugadores (agregar/eliminar)
 * 
 * Seguridad en hilos (Thread-safety):
 *   - Métodos synchronized: Protegen la adición/eliminación de jugadores
 *   - AtomicInteger: Generación de IDs segura para múltiples hilos
 *   - GameState: Usa ConcurrentHashMap internamente para los jugadores
 */
public class GameService {
    private final GameState gameState;
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    
    /**
     * Constructor
     * @param gameState
     */
    public GameService(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Agrega un jugador al juego
     * Seguro para hilos: Usa el método addPlayer sincronizado de GameState
     * 
     * Equivalente a: Server aceptando una nueva conexión de cliente
     * - Genera un ID único para el jugador (AtomicInteger)
     * - Obtiene una posición aleatoria transitable del mundo
     * - Crea la entidad Player
     * - La agrega al GameState
     * 
     * @param playerName Nombre del jugador
     * @return La instancia de Player creada
    */
    public synchronized Player addPlayer(String playerName) {
        int playerId = nextPlayerId.getAndIncrement();
        
        int[] pos = gameState.getWorld().getRandomWalkablePosition();
        Player player = new Player(playerName, pos[0], pos[1]);
        
        gameState.addPlayer(playerId, player);
        
        return player;
    }
    
    /**
     * Elimina un jugador del juego
     * Seguro para hilos: Usa el método removePlayer sincronizado de GameState
     * 
     * Equivalente a: Server manejando la desconexión de un cliente
     * 
     * @param playerId ID del jugador a eliminar
     */
    public synchronized void removePlayer(int playerId) {
        gameState.removePlayer(playerId);
    }
    
    /**
     * Obtiene el estado del juego
     * @return La instancia de GameState
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Obtiene el número actual de jugadores
     * Seguro para hilos: Usa el método sincronizado de GameState
     * @return Número de jugadores actualmente en el juego
     */
    public int getCurrentPlayerCount() {
        return gameState.getCurrentPlayerCount();
    }
}
