package backend.combat;

import java.util.concurrent.BlockingQueue;
import backend.core.GameState;
import backend.game.GameMode;
import backend.entities.Player;

public class CombatSystem {

    private GameState gameState;
    private Enemy currentEnemy;
    private boolean playerTurn = true;
    private String lastActionMessage = "";

    public CombatSystem(GameState gameState) {
        this.gameState = gameState;
    }

    public Enemy getCurrentEnemy() {
        return currentEnemy;
    }

    public boolean isPlayerTurn() {
        return playerTurn;
    }
    
    public String getLastActionMessage() {
        return lastActionMessage;
    }

    public void startCombat(Enemy enemy) {
        this.currentEnemy = enemy;
        this.playerTurn = true;
        this.lastActionMessage = "¡Combate iniciado contra " + enemy.getName() + "!";
        gameState.setMode(GameMode.Mode.Combat);
    }

    public void update(BlockingQueue<String> inputQueue) {

        if (currentEnemy == null) return;

        if (playerTurn) {
            processPlayerTurn(inputQueue);
        }
        
        checkCombatEnd();
    }

    // Método para procesar un comando individual
    public void processCommand(String command) {
        if (currentEnemy == null || !playerTurn) return;
        
        String cmd = command.toLowerCase().trim();
        Player player = gameState.getPlayer();

        switch (cmd) {

            case "attack", "a" -> {
                int damage = player.dealDamage();
                currentEnemy.takeDamage(damage);
                lastActionMessage = "Atacas a " + currentEnemy.getName() + " por " + damage + " de daño!";
                
                if (currentEnemy.isAlive()) {
                    processEnemyTurn();
                }
            }

            case "run", "r" -> {
                lastActionMessage = "¡Escapaste del combate!";
                endCombat();
            }

            default -> {
                lastActionMessage = "Comando inválido en combate. Usa 'attack' ('a') o 'run' ('r').";
            }
        }
        
        checkCombatEnd();
    }

    private void processPlayerTurn(BlockingQueue<String> inputQueue) {

        if (inputQueue.isEmpty()) return;

        String cmd = inputQueue.poll().toLowerCase().trim();

        Player player = gameState.getPlayer();

        switch (cmd) {

            case "attack", "a" -> {
                int damage = player.dealDamage();
                currentEnemy.takeDamage(damage);
                lastActionMessage = "Atacas a " + currentEnemy.getName() + " por " + damage + " de daño!";
                
                if (currentEnemy.isAlive()) {
                    processEnemyTurn();
                }
            }

            case "run", "r" -> {
                lastActionMessage = "¡Escapaste del combate!";
                endCombat();
            }

            default -> {
                lastActionMessage = "Comando inválido. Usa 'attack' o 'run'.";
            }
        }
    }

    private void processEnemyTurn() {

        if (!currentEnemy.isAlive()) return;

        Player player = gameState.getPlayer();

        int damage = currentEnemy.dealDamage();
        player.takeDamage(damage);
        
        lastActionMessage += "\n" + currentEnemy.getName() + " te ataca por " + damage + " de daño!";
        
        playerTurn = true;
    }

    private void checkCombatEnd() {

        Player player = gameState.getPlayer();

        if (!player.isAlive()) {
            lastActionMessage = "¡HAS MUERTO! Game Over.";
            gameState.stop();
            return;
        }

        if (currentEnemy != null && !currentEnemy.isAlive()) {
            lastActionMessage = "¡Victoria! Derrotaste a " + currentEnemy.getName() + "!";
            endCombat();
        }
    }

    private void endCombat() {
        if (currentEnemy != null && !currentEnemy.isAlive()) {
            int enemyX = currentEnemy.getX();
            int enemyY = currentEnemy.getY();
            gameState.getWorld().removeEnemy(currentEnemy);
            
            Player player = gameState.getPlayer();
            int dx = enemyX - player.getX();
            int dy = enemyY - player.getY();
            player.move(dx, dy);
        }
        currentEnemy = null;
        gameState.setMode(GameMode.Mode.Exploration);
    }
}
