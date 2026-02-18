package backend.commands;

import backend.combat.Enemy;
import backend.core.GameState;
import backend.entities.Player;
import backend.world.World;
import backend.game.GameMode;

public class CommandProcessor {

    private final GameState gameState;

    public CommandProcessor(GameState gameState) {
        this.gameState = gameState;
    }

    private void movePlayer(int dx, int dy) {

        synchronized (gameState) {

            Player player = gameState.getPlayer();
            World world = gameState.getWorld();

            int newX = player.getX() + dx;
            int newY = player.getY() + dy;

            if (world.isWalkable(newX, newY)) {
                Enemy enemy = world.getEnemyAt(newX, newY);
                if (enemy != null) {
                    gameState.getCombatSystem().startCombat(enemy);
                } else {
                    player.move(dx, dy);
                }
            }
        }
    }

    public void process(String command) {

        String cmd = command.toLowerCase().trim();
        
        // Verificar el modo de juego actual
        GameMode.Mode currentMode = gameState.getCurrentGameMode();
        
        // Si está en combate, delegar al CombatSystem
        if (currentMode == GameMode.Mode.Combat) {
            gameState.getCombatSystem().processCommand(cmd);
            return;
        }
        
        // Si está en exploración, procesar comandos de movimiento
        switch (cmd) {

            case "w" -> movePlayer(0, -1);
            case "s" -> movePlayer(0, 1);
            case "a" -> movePlayer(-1, 0);
            case "d" -> movePlayer(1, 0);

            case "quit" -> {
                System.out.println("Saliendo del juego...");
                gameState.stop();
            }

            case "combat" -> {
                Enemy enemy = new Enemy("Goblin", 0, 0, 30, 8);
                gameState.getCombatSystem().startCombat(enemy);
            }

            case "explore" -> {
                gameState.setMode(GameMode.Mode.Exploration);
            }

            default -> {}
        }
    }
}
