package backend.threads;

import java.io.BufferedReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ReceiverThread implements Runnable {
    private BufferedReader in;
    private volatile boolean running = true;
    private JSONObject lastGameState;
    private final JSONParser parser = new JSONParser();

    public ReceiverThread(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            while (running) {
                String message = in.readLine();
                if (message == null) {
                    System.out.println("Servidor desconectado");
                    break;
                }
                
                // Parsear el estado del juego recibido
                lastGameState = (JSONObject) parser.parse(message);
                
                // Renderizar el estado
                renderGameState(lastGameState);
                
            }
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    private void renderGameState(JSONObject gameState) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        // Obtener datos del juego
        JSONObject world = (JSONObject) gameState.get("world");
        JSONObject player = (JSONObject) gameState.get("player");
        
        if(world != null && player != null){
            // Obtener posición del jugador
            Long playerX = (Long) player.get("x");
            Long playerY = (Long) player.get("y");
            
            // Obtener mapa
            org.json.simple.JSONArray mapArray = (org.json.simple.JSONArray) world.get("map");
            
            // Obtener enemigos
            org.json.simple.JSONArray enemiesArray = (org.json.simple.JSONArray) world.get("enemies");
            
            // Renderizar el mapa
            for(int y = 0; y < mapArray.size(); y++){
                String row = (String) mapArray.get(y);
                for(int x = 0; x < row.length(); x++){
                    // Verificar si es la posición del jugador
                    if(x == playerX && y == playerY){
                        System.out.print("@");
                    }
                    // Verificar si hay un enemigo en esta posición
                    else if(hasEnemyAt(enemiesArray, x, y)){
                        System.out.print("E");
                    }
                    else{
                        System.out.print(row.charAt(x));
                    }
                }
                System.out.println();
            }
            
            // Mostrar UI
            System.out.println();
            System.out.println("===== STATS =====");
            System.out.println("Nombre: " + player.get("name"));
            System.out.println("Vida: " + player.get("hp") + "/" + player.get("maxHp"));
            System.out.println("Hora: " + gameState.get("worldTime"));
            System.out.println("Modo: " + gameState.get("gameMode"));
            
            // Verificar si está en combate
            String gameMode = (String) gameState.get("gameMode");
            if("Combat".equals(gameMode)){
                JSONObject combat = (JSONObject) gameState.get("combat");
                if(combat != null){
                    System.out.println("\n--- COMBATE ---");
                    
                    JSONObject enemy = (JSONObject) combat.get("enemy");
                    if(enemy != null){
                        System.out.println("Enemigo: " + enemy.get("name"));
                        System.out.println("Vida Enemigo: " + enemy.get("hp") + "/" + enemy.get("maxHp"));
                    }
                    
                    Boolean playerTurn = (Boolean) combat.get("playerTurn");
                    System.out.println("Turno: " + (playerTurn ? "TUYO" : "ENEMIGO"));
                    
                    String lastMessage = (String) combat.get("lastMessage");
                    if(lastMessage != null && !lastMessage.isEmpty()){
                        System.out.println("\n" + lastMessage);
                    }
                    
                    System.out.println("\nComandos: attack ('a') | run ('r')");
                }
            } else {
                System.out.println("\nComandos: W A S D | quit");
                System.out.println("Leyenda: @ = Tú | E = Enemigo | # = Muro");
            }
            
            System.out.println("=================");
            System.out.print("> ");
        }
    }
    
    private boolean hasEnemyAt(org.json.simple.JSONArray enemies, int x, int y){
        for(Object obj : enemies){
            JSONObject enemy = (JSONObject) obj;
            Long enemyX = (Long) enemy.get("x");
            Long enemyY = (Long) enemy.get("y");
            if(enemyX == x && enemyY == y){
                return true;
            }
        }
        return false;
    }

    public JSONObject getLastGameState() {
        return lastGameState;
    }

    public void stop() {
        running = false;
    }
}
