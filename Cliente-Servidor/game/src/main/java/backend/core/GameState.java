package backend.core;

import backend.combat.CombatSystem;
import backend.game.GameMode;
import backend.entities.Player;
import backend.world.World;
import org.json.simple.JSONObject;

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

    // Serializar estado del juego a JSON para enviar al cliente
    public synchronized JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("worldTime", worldTime);
        json.put("gameMode", currentGameMode.toString());
        json.put("running", running);
        
        // Datos del jugador
        JSONObject playerJson = new JSONObject();
        playerJson.put("name", player.getName());
        playerJson.put("x", player.getX());
        playerJson.put("y", player.getY());
        playerJson.put("hp", player.getHealth());
        playerJson.put("maxHp", player.getMaxHealth());
        json.put("player", playerJson);
        
        // Datos del mundo
        JSONObject worldJson = new JSONObject();
        worldJson.put("width", world.width);
        worldJson.put("height", world.height);
        
        // Serializar el mapa
        org.json.simple.JSONArray mapArray = new org.json.simple.JSONArray();
        for(int y = 0; y < world.height; y++){
            StringBuilder row = new StringBuilder();
            for(int x = 0; x < world.width; x++){
                row.append(world.map[y][x]);
            }
            mapArray.add(row.toString());
        }
        worldJson.put("map", mapArray);
        
        // Serializar enemigos
        org.json.simple.JSONArray enemiesArray = new org.json.simple.JSONArray();
        for(backend.combat.Enemy enemy : world.getEnemies()){
            JSONObject enemyJson = new JSONObject();
            enemyJson.put("name", enemy.getName());
            enemyJson.put("x", enemy.getX());
            enemyJson.put("y", enemy.getY());
            enemyJson.put("hp", enemy.getHealth());
            enemiesArray.add(enemyJson);
        }
        worldJson.put("enemies", enemiesArray);
        
        json.put("world", worldJson);
        

        // Datos de combate (si en combate)
        if(currentGameMode == GameMode.Mode.Combat){
            CombatSystem combat = getCombatSystem();
            backend.combat.Enemy enemy = combat.getCurrentEnemy();
            
            if(enemy != null){
                JSONObject combatJson = new JSONObject();
                
                // Información del turno
                combatJson.put("playerTurn", combat.isPlayerTurn());
                
                // Información del enemigo
                JSONObject enemyJson = new JSONObject();
                enemyJson.put("name", enemy.getName());
                enemyJson.put("hp", enemy.getHealth());
                enemyJson.put("maxHp", enemy.getMaxHealth());
                combatJson.put("enemy", enemyJson);
                
                // Último mensaje de acción
                combatJson.put("lastMessage", combat.getLastActionMessage());
                
                json.put("combat", combatJson);
            }
        }
        return json;
    }
}
