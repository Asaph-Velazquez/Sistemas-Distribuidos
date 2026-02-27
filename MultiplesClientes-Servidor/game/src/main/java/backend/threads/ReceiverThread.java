package backend.threads;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;

public class ReceiverThread implements Runnable {
    private BufferedReader in;
    private volatile boolean running = true;
    private JSONObject lastGameState;
    private final JSONParser parser = new JSONParser();
    private Screen screen;
    private int myPlayerId = -1;

    public ReceiverThread(BufferedReader in) {
        this.in = in;
        try {
            // Configurar fuente mejorada con anti-aliasing
            SwingTerminalFontConfiguration fontConfig = new SwingTerminalFontConfiguration(
                true,  // useAntiAliasing
                SwingTerminalFontConfiguration.BoldMode.EVERYTHING,
                new Font("Consolas", Font.PLAIN, 18),
                new Font("Courier New", Font.PLAIN, 18)
            );
            
            SwingTerminalFrame terminal = new SwingTerminalFrame(
                "Game Client",
                new TerminalSize(80, 30),
                null,  // deviceConfig
                fontConfig,
                null,  // colorConfig
                TerminalEmulatorAutoCloseTrigger.CloseOnEscape
            );
            
            terminal.setPreferredSize(new java.awt.Dimension(1000, 700));
            terminal.pack();
            terminal.setVisible(true);
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            screen.setCursorPosition(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                String message = in.readLine();
                if (message == null) {
                    drawText(0, 0, "Servidor desconectado", TextColor.ANSI.RED);
                    screen.refresh();
                    break;
                }
                lastGameState = (JSONObject) parser.parse(message);
                renderGameState(lastGameState);
            }
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            cleanup();
        }
    }

    private void renderGameState(JSONObject gameState) {
        try {
            screen.clear();
            
            if (gameState.get("playerId") != null) {
                myPlayerId = ((Long) gameState.get("playerId")).intValue();
            }
            
            JSONObject world = (JSONObject) gameState.get("world");
            org.json.simple.JSONArray playersArray = (org.json.simple.JSONArray) gameState.get("players");
            
            JSONObject myPlayer = null;
            if (playersArray != null) {
                for (Object p : playersArray) {
                    JSONObject pj = (JSONObject) p;
                    if (((Long) pj.get("id")).intValue() == myPlayerId) {
                        myPlayer = pj;
                        break;
                    }
                }
            }
            
            if(world != null && playersArray != null){
                org.json.simple.JSONArray mapArray = (org.json.simple.JSONArray) world.get("map");
                org.json.simple.JSONArray enemiesArray = (org.json.simple.JSONArray) world.get("enemies");
                
                for(int y = 0; y < mapArray.size(); y++){
                    String row = (String) mapArray.get(y);
                    for(int x = 0; x < row.length(); x++){
                        char ch = row.charAt(x);
                        TextColor color = TextColor.ANSI.WHITE;
                        
                        boolean isPlayer = false;
                        for (Object p : playersArray) {
                            JSONObject pj = (JSONObject) p;
                            Long px = (Long) pj.get("x");
                            Long py = (Long) pj.get("y");
                            if (px == x && py == y) {
                                int pid = ((Long) pj.get("id")).intValue();
                                ch = (char) ('0' + pid);
                                color = (pid == myPlayerId) ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.YELLOW;
                                isPlayer = true;
                                break;
                            }
                        }
                        
                        if (!isPlayer) {
                            if(hasEnemyAt(enemiesArray, x, y)){
                                ch = 'E';
                                color = TextColor.ANSI.RED_BRIGHT;
                            }
                            else if(ch == '#'){
                                color = TextColor.ANSI.CYAN;
                            }
                            else if(ch == '.'){
                                color = TextColor.ANSI.BLACK_BRIGHT;
                            }
                        }
                        
                        drawText(x, y, String.valueOf(ch), color);
                    }
                }
                
                int uiY = 0;
                int uiX = 25;
                
                drawText(uiX, uiY++, "╔════════ STATS ════════╗", TextColor.ANSI.YELLOW);
                
                if (myPlayer != null) {
                    drawText(uiX, uiY++, "║ Jugador: " + myPlayer.get("name") + "     ║", TextColor.ANSI.WHITE);
                    
                    Long hp = (Long) myPlayer.get("hp");
                    Long maxHp = (Long) myPlayer.get("maxHp");
                    String hpBar = createHealthBar(hp.intValue(), maxHp.intValue(), 10);
                    drawText(uiX, uiY++, "║ Vida: " + hpBar + "    ║", TextColor.ANSI.WHITE);
                    drawText(uiX, uiY++, "║       " + hp + "/" + maxHp + "           ║", TextColor.ANSI.WHITE);
                }
                
                drawText(uiX, uiY++, "║ Jugadores: " + playersArray.size() + "            ║", TextColor.ANSI.WHITE);
                drawText(uiX, uiY++, "║ Tu ID: " + myPlayerId + "                ║", TextColor.ANSI.WHITE);
                drawText(uiX, uiY++, "║ Tiempo: " + gameState.get("worldTime") + "           ║", TextColor.ANSI.WHITE);
                drawText(uiX, uiY++, "║ Modo: " + gameState.get("gameMode") + "    ║", TextColor.ANSI.WHITE);
                
                org.json.simple.JSONArray combats = (org.json.simple.JSONArray) gameState.get("combats");
                if (combats != null && !combats.isEmpty()) {
                    for (Object c : combats) {
                        JSONObject combat = (JSONObject) c;
                        int combatPlayerId = ((Long) combat.get("playerId")).intValue();
                        boolean isMyCombat = (combatPlayerId == myPlayerId);
                        
                        drawText(uiX, uiY++, "╠══ COMBATE J" + combatPlayerId + " ═╣", isMyCombat ? TextColor.ANSI.RED_BRIGHT : TextColor.ANSI.RED);
                        
                        JSONObject enemy = (JSONObject) combat.get("enemy");
                        if(enemy != null){
                            drawText(uiX, uiY++, "║ Enemigo: " + enemy.get("name") + "      ║", TextColor.ANSI.RED);
                            Long enemyHp = (Long) enemy.get("hp");
                            Long enemyMaxHp = (Long) enemy.get("maxHp");
                            String enemyHpBar = createHealthBar(enemyHp.intValue(), enemyMaxHp.intValue(), 10);
                            drawText(uiX, uiY++, "║ HP: " + enemyHpBar + "        ║", TextColor.ANSI.RED);
                        }
                        
                        Boolean playerTurn = (Boolean) combat.get("playerTurn");
                        TextColor turnColor = (playerTurn && isMyCombat) ? TextColor.ANSI.GREEN : TextColor.ANSI.YELLOW;
                        drawText(uiX, uiY++, "║ Turno: " + (playerTurn ? "J" + combatPlayerId : "ENEMIGO") + "     ║", turnColor);
                        
                        String lastMessage = (String) combat.get("lastMessage");
                        if(lastMessage != null && !lastMessage.isEmpty()){
                            uiY++;
                            String[] lines = lastMessage.split("\n");
                            for(String line : lines){
                                if(line.length() > 20) {
                                    drawText(uiX, uiY++, "║ " + line.substring(0, 20) + "║", TextColor.ANSI.YELLOW);
                                } else {
                                    drawText(uiX, uiY++, "║ " + line + " ".repeat(20 - line.length()) + "║", TextColor.ANSI.YELLOW);
                                }
                            }
                        }
                        
                        if (isMyCombat) {
                            drawText(uiX, uiY++, "║ (A)ttack | (R)un      ║", TextColor.ANSI.CYAN);
                        }
                    }
                } else {
                    drawText(uiX, uiY++, "║                       ║", TextColor.ANSI.WHITE);
                    drawText(uiX, uiY++, "║ W A S D | quit        ║", TextColor.ANSI.CYAN);
                    drawText(uiX, uiY++, "║ 1,2,3 = Jugadores    ║", TextColor.ANSI.WHITE);
                    drawText(uiX, uiY++, "║ E = Enemigo # = Muro ║", TextColor.ANSI.WHITE);
                }
                
                drawText(uiX, uiY++, "╚═══════════════════════╝", TextColor.ANSI.YELLOW);
                
                drawText(0, mapArray.size() + 1, "> ", TextColor.ANSI.GREEN_BRIGHT);
            }
            
            screen.refresh();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String createHealthBar(int current, int max, int length) {
        int filled = (int) ((double) current / max * length);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        return bar.toString();
    }
    
    private void drawText(int x, int y, String text, TextColor color) {
        try {
            for (int i = 0; i < text.length(); i++) {
                screen.setCharacter(x + i, y, 
                    new com.googlecode.lanterna.TextCharacter(
                        text.charAt(i), 
                        color, 
                        TextColor.ANSI.BLACK
                    )
                );
            }
        } catch (Exception e) {
            // Ignorar si está fuera de límites
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
        cleanup();
    }
    
    private void cleanup() {
        try {
            if (screen != null) {
                screen.stopScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
