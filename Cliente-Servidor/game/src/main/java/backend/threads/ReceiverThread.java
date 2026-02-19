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
            
            JSONObject world = (JSONObject) gameState.get("world");
            JSONObject player = (JSONObject) gameState.get("player");
            
            if(world != null && player != null){
                Long playerX = (Long) player.get("x");
                Long playerY = (Long) player.get("y");
                
                org.json.simple.JSONArray mapArray = (org.json.simple.JSONArray) world.get("map");
                org.json.simple.JSONArray enemiesArray = (org.json.simple.JSONArray) world.get("enemies");
                
                // Renderizar el mapa con colores
                for(int y = 0; y < mapArray.size(); y++){
                    String row = (String) mapArray.get(y);
                    for(int x = 0; x < row.length(); x++){
                        char ch = row.charAt(x);
                        TextColor color = TextColor.ANSI.WHITE;
                        
                        if(x == playerX && y == playerY){
                            ch = '@';
                            color = TextColor.ANSI.GREEN_BRIGHT;
                        }
                        else if(hasEnemyAt(enemiesArray, x, y)){
                            ch = 'E';
                            color = TextColor.ANSI.RED_BRIGHT;
                        }
                        else if(ch == '#'){
                            color = TextColor.ANSI.CYAN;
                        }
                        else if(ch == '.'){
                            color = TextColor.ANSI.BLACK_BRIGHT;
                        }
                        
                        drawText(x, y, String.valueOf(ch), color);
                    }
                }
                
                // UI en el lado derecho o abajo
                int uiY = 0;
                int uiX = 25;
                
                drawText(uiX, uiY++, "╔════════ STATS ════════╗", TextColor.ANSI.YELLOW);
                drawText(uiX, uiY++, "║ Nombre: " + player.get("name") + "        ║", TextColor.ANSI.WHITE);
                
                Long hp = (Long) player.get("hp");
                Long maxHp = (Long) player.get("maxHp");
                String hpBar = createHealthBar(hp.intValue(), maxHp.intValue(), 10);
                drawText(uiX, uiY++, "║ Vida: " + hpBar + "    ║", TextColor.ANSI.WHITE);
                drawText(uiX, uiY++, "║       " + hp + "/" + maxHp + "           ║", TextColor.ANSI.WHITE);
                drawText(uiX, uiY++, "║ Tiempo: " + gameState.get("worldTime") + "           ║", TextColor.ANSI.WHITE);
                drawText(uiX, uiY++, "║ Modo: " + gameState.get("gameMode") + "    ║", TextColor.ANSI.WHITE);
                
                // Verificar si está en combate
                String gameMode = (String) gameState.get("gameMode");
                if("Combat".equals(gameMode)){
                    JSONObject combat = (JSONObject) gameState.get("combat");
                    if(combat != null){
                        drawText(uiX, uiY++, "╠═══════ COMBATE ═══════╣", TextColor.ANSI.RED_BRIGHT);
                        
                        JSONObject enemy = (JSONObject) combat.get("enemy");
                        if(enemy != null){
                            drawText(uiX, uiY++, "║ Enemigo: " + enemy.get("name") + "      ║", TextColor.ANSI.RED);
                            Long enemyHp = (Long) enemy.get("hp");
                            Long enemyMaxHp = (Long) enemy.get("maxHp");
                            String enemyHpBar = createHealthBar(enemyHp.intValue(), enemyMaxHp.intValue(), 10);
                            drawText(uiX, uiY++, "║ HP: " + enemyHpBar + "        ║", TextColor.ANSI.RED);
                        }
                        
                        Boolean playerTurn = (Boolean) combat.get("playerTurn");
                        TextColor turnColor = playerTurn ? TextColor.ANSI.GREEN : TextColor.ANSI.YELLOW;
                        drawText(uiX, uiY++, "║ Turno: " + (playerTurn ? "TUYO" : "ENEMIGO") + "     ║", turnColor);
                        
                        String lastMessage = (String) combat.get("lastMessage");
                        if(lastMessage != null && !lastMessage.isEmpty()){
                            uiY++;
                            // Dividir mensaje si es muy largo
                            String[] lines = lastMessage.split("\n");
                            for(String line : lines){
                                if(line.length() > 20) {
                                    drawText(uiX, uiY++, "║ " + line.substring(0, 20) + "║", TextColor.ANSI.YELLOW);
                                } else {
                                    drawText(uiX, uiY++, "║ " + line + " ".repeat(20 - line.length()) + "║", TextColor.ANSI.YELLOW);
                                }
                            }
                        }
                        
                        drawText(uiX, uiY++, "║                       ║", TextColor.ANSI.WHITE);
                        drawText(uiX, uiY++, "║ (A)ttack | (R)un      ║", TextColor.ANSI.CYAN);
                    }
                } else {
                    drawText(uiX, uiY++, "║                       ║", TextColor.ANSI.WHITE);
                    drawText(uiX, uiY++, "║ W A S D | quit        ║", TextColor.ANSI.CYAN);
                    drawText(uiX, uiY++, "║ @ = Tú  E = Enemigo   ║", TextColor.ANSI.WHITE);
                    drawText(uiX, uiY++, "║ # = Muro              ║", TextColor.ANSI.WHITE);
                }
                
                drawText(uiX, uiY++, "╚═══════════════════════╝", TextColor.ANSI.YELLOW);
                
                // Prompt
                drawText(0, (int)mapArray.size() + 1, "> ", TextColor.ANSI.GREEN_BRIGHT);
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
