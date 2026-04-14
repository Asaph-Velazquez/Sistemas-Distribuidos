package backend.core;

import backend.entities.Player;
import backend.world.World;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameState - Estado del juego
 * 
 * Gestiona el estado global de una partida/juego.
 * 
 * Sincronización:
 *   - ConcurrentHashMap para players: permite acceso concurrente sin bloqueos
 *   - Variables volatile: running, currentGameMode para visibilidad entre hilos
 *   - Métodos synchronized: addPlayer, removePlayer, getPlayer para consistencia
 *   - ConcurrentHashMap para el combatSystem: acceso thread-safe a combatientes
 */
public class GameState {
    // Volatile: Garantiza que los cambios sean visibles entre hilos inmediatamente
    // Sin volatile, los hilos podrían ver valores stale (antiguos)
    private volatile boolean running = true;
    
    // Thread-safe: ConcurrentHashMap permite múltiples lectores simultáneos
    // y escritores seguros sin bloqueos externos
    private Map<Integer, Player> players = new ConcurrentHashMap<>();
    
    private int worldTime = 0;
    private World world;
    // TODO: CombatSystem será implementado en otra tarea
    // private CombatSystem combatSystem;
    
    // ============================================
    // INFORMACIÓN DE LA PARTIDA (Multi-Servidor)
    // ============================================
    // Información de la partida para el sistema de matchmaking
    private String gameName = "";        // Nombre de la partida
    private String hostName = "";        // Nombre del host (creador)
    private int maxPlayers = 4;          // Máximo de jugadores
    private String gameServerAddress;    // Dirección del servidor de matchmaking

    /**
     * Constructor de GameState
     * @param world Instancia del mundo del juego
     */
    public GameState(World world) {
        this.world = world;
        // TODO: Inicializar CombatSystem cuando se implemente
        // this.combatSystem = new CombatSystem(this);
    }
    
    // ============================================
    // MÉTODOS DE INFORMACIÓN DE PARTIDA (Multi-Servidor)
    // ============================================
    
    public String getGameName() {
        return gameName;
    }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public void setGameServerAddress(String address) {
        this.gameServerAddress = address;
    }
    
    public String getGameServerAddress() {
        return gameServerAddress;
    }
    
    /**
     * Devuelve el número actual de jugadores conectados
     * Synchronized: Necesario para consistencia al leer el tamaño
     * @return Número de jugadores activos
     */
    public synchronized int getCurrentPlayerCount() {
        return players.size();
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
    
    // TODO: Método getCombatSystem() será implementado cuando exista CombatSystem
    // public CombatSystem getCombatSystem(){
    //     return combatSystem;
    // }

    public World getWorld() {
        return world;
    }

    /**
     * Agrega un jugador al estado del juego
     * Synchronized: Garantiza consistencia al modificar el mapa de jugadores
     * @param id ID único del jugador
     * @param player Instancia del jugador
     */
    public synchronized void addPlayer(int id, Player player) {
        players.put(id, player);
    }

    /**
     * Elimina un jugador del estado del juego
     * Synchronized: Garantiza consistencia al modificar el mapa de jugadores
     * @param id ID del jugador a eliminar
     */
    public synchronized void removePlayer(int id) {
        // TODO: Llamar a combatSystem.removePlayer(id) cuando se implemente
        // combatSystem.removePlayer(id);
        players.remove(id);
    }

    /**
     * Obtiene un jugador por su ID
     * Synchronized: Garantiza lectura consistente
     * @param id ID del jugador
     * @return Jugador o null si no existe
     */
    public synchronized Player getPlayer(int id) {
        return players.get(id);
    }

    /**
     * Obtiene todos los jugadores
     * Synchronized: Garantiza lectura consistente
     * @return Copia del mapa de jugadores
     */
    public synchronized Map<Integer, Player> getAllPlayers() {
        return new ConcurrentHashMap<>(players);
    }

    // NOTA: El método toJSON() será implementado en otra tarea posterior
    // cuando se integre con la serialización JSON de Spring Boot
}
