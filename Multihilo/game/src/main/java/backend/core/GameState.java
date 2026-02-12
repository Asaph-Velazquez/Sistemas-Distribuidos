package backend.core;

import backend.combat.CombatSystem;
import backend.game.GameMode;
import backend.entities.Player;
import backend.world.World;

public class GameState {
    private volatile boolean running = true;
    private volatile GameMode.Mode currentGameMode = GameMode.Mode.Exploration;
    private Player player;
    private int worldTime = 0;
    private World world;
    private CombatSystem combatSystem;

    public GameState(Player player, World world) {
        this.player = player;
        this.world = world;
        this.combatSystem = new CombatSystem(this);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }

    // Modo de juego
    public GameMode.Mode getCurrentGameMode() {
        return currentGameMode;
    }
    
    public void setMode(GameMode.Mode mode){
        this.currentGameMode = mode;
    }
    
    public CombatSystem getCombatSystem(){
        return combatSystem;
    }

    public World getWorld() {
        return world;
    }

    // Tiempo global
    public synchronized void incrementWorldTime() {
        worldTime++;
    }

    public int getWorldTime() {
        return worldTime;
    }

    // Jugador
    public Player getPlayer() {
        return player;
    }
}
