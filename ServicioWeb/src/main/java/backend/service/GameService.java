package backend.service;

import backend.core.GameState;
import backend.entities.Player;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GameService - Manages the state of a single game instance
 * 
 * NOT a Spring @Service - one instance created per game
 * Replaces the socket-based Server.java with in-memory state management
 * 
 * Each GameService instance manages:
 *   - One GameState (world, players, time)
 *   - Player ID generation (AtomicInteger for thread-safety)
 *   - Player lifecycle (add/remove)
 * 
 * Thread-safety:
 *   - synchronized methods: Protect player addition/removal
 *   - AtomicInteger: Thread-safe ID generation
 *   - GameState: Uses ConcurrentHashMap internally for players
 */
public class GameService {
    private final GameState gameState;
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    
    /**
     * Constructor
     * @param gameState The GameState instance for this game
     */
    public GameService(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Add a player to the game
     * Thread-safe: Uses GameState's synchronized addPlayer
     * 
     * Equivalent to: Server accepting a new client connection
     * - Generates unique player ID (AtomicInteger)
     * - Gets random walkable position from world
     * - Creates Player entity
     * - Adds to GameState
     * 
     * @param playerName Name of the player
     * @return The created Player instance
     */
    public synchronized Player addPlayer(String playerName) {
        int playerId = nextPlayerId.getAndIncrement();
        
        int[] pos = gameState.getWorld().getRandomWalkablePosition();
        Player player = new Player(playerName, pos[0], pos[1]);
        
        gameState.addPlayer(playerId, player);
        
        return player;
    }
    
    /**
     * Remove a player from the game
     * Thread-safe: Uses GameState's synchronized removePlayer
     * 
     * Equivalent to: Server handling client disconnection
     * 
     * @param playerId ID of the player to remove
     */
    public synchronized void removePlayer(int playerId) {
        gameState.removePlayer(playerId);
    }
    
    /**
     * Get the game state
     * @return The GameState instance
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Get current player count
     * Thread-safe: Uses GameState's synchronized method
     * @return Number of players currently in the game
     */
    public int getCurrentPlayerCount() {
        return gameState.getCurrentPlayerCount();
    }
}
