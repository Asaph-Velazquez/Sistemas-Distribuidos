package backend.core;

import backend.combat.CombatSystem;
import backend.game.GameMode;
import backend.entities.Player;
import backend.world.World;
import org.json.simple.JSONObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private volatile boolean running = true;
    private volatile GameMode.Mode currentGameMode = GameMode.Mode.Exploration;
    private Map<Integer, Player> players = new ConcurrentHashMap<>();
    private int worldTime = 0;
    private World world;
    private CombatSystem combatSystem;

    public GameState(World world) {
        this.world = world;
        this.combatSystem = new CombatSystem(this);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }

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

    public synchronized void incrementWorldTime() {
        worldTime++;
    }

    public int getWorldTime() {
        return worldTime;
    }

    public synchronized void addPlayer(int id, Player player) {
        players.put(id, player);
    }

    public synchronized void removePlayer(int id) {
        combatSystem.removePlayer(id);
        players.remove(id);
    }

    public synchronized Player getPlayer(int id) {
        return players.get(id);
    }

    public synchronized Player getAnyPlayer() {
        return players.isEmpty() ? null : players.values().iterator().next();
    }

    public synchronized Map<Integer, Player> getAllPlayers() {
        return new ConcurrentHashMap<>(players);
    }

    public synchronized JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("worldTime", worldTime);
        json.put("gameMode", currentGameMode.toString());
        json.put("running", running);
        
        org.json.simple.JSONArray playersArray = new org.json.simple.JSONArray();
        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
            JSONObject playerJson = new JSONObject();
            playerJson.put("id", entry.getKey());
            playerJson.put("name", entry.getValue().getName());
            playerJson.put("x", entry.getValue().getX());
            playerJson.put("y", entry.getValue().getY());
            playerJson.put("hp", entry.getValue().getHealth());
            playerJson.put("maxHp", entry.getValue().getMaxHealth());
            playersArray.add(playerJson);
        }
        json.put("players", playersArray);
        
        JSONObject worldJson = new JSONObject();
        worldJson.put("width", world.width);
        worldJson.put("height", world.height);
        
        org.json.simple.JSONArray mapArray = new org.json.simple.JSONArray();
        for(int y = 0; y < world.height; y++){
            StringBuilder row = new StringBuilder();
            for(int x = 0; x < world.width; x++){
                row.append(world.map[y][x]);
            }
            mapArray.add(row.toString());
        }
        worldJson.put("map", mapArray);
        
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
        
        org.json.simple.JSONArray combatsArray = new org.json.simple.JSONArray();
        for (Integer playerId : players.keySet()) {
            if (combatSystem.isInCombat(playerId)) {
                JSONObject combatJson = new JSONObject();
                combatJson.put("playerId", playerId);
                combatJson.put("playerTurn", combatSystem.isPlayerTurn(playerId));
                
                backend.combat.Enemy enemy = combatSystem.getCurrentEnemy(playerId);
                if (enemy != null) {
                    JSONObject enemyJson = new JSONObject();
                    enemyJson.put("name", enemy.getName());
                    enemyJson.put("hp", enemy.getHealth());
                    enemyJson.put("maxHp", enemy.getMaxHealth());
                    combatJson.put("enemy", enemyJson);
                }
                
                combatJson.put("lastMessage", combatSystem.getLastActionMessage(playerId));
                combatsArray.add(combatJson);
            }
        }
        if (!combatsArray.isEmpty()) {
            json.put("combats", combatsArray);
        }
        
        return json;
    }
}
