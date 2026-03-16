package backend.commands;

import backend.combat.Enemy;
import backend.core.GameState;
import backend.entities.Player;
import backend.world.World;

public class CommandProcessor {

    private final GameState gameState;

    public CommandProcessor(GameState gameState) {
        this.gameState = gameState;
    }

    private void movePlayer(int dx, int dy, int playerId) {

        synchronized (gameState) {

            if (gameState.getCombatSystem().isInCombat(playerId)) {
                return;
            }

            Player player = gameState.getPlayer(playerId);
            if (player == null) {
                return;
            }
            
            World world = gameState.getWorld();

            int newX = player.getX() + dx;
            int newY = player.getY() + dy;

            if (world.isWalkable(newX, newY)) {
                Enemy enemy = world.getEnemyAt(newX, newY);
                if (enemy != null) {
                    gameState.getCombatSystem().startCombat(playerId, enemy);
                } else {
                    player.move(dx, dy);
                }
            }
        }
    }

    public void process(String command, int playerId) {

        String cmd = command.toLowerCase().trim();
        
        if (gameState.getCombatSystem().isInCombat(playerId)) {
            gameState.getCombatSystem().processCommand(playerId, cmd);
            return;
        }
        
        switch (cmd) {

            case "w" -> movePlayer(0, -1, playerId);
            case "s" -> movePlayer(0, 1, playerId);
            case "a" -> movePlayer(-1, 0, playerId);
            case "d" -> movePlayer(1, 0, playerId);

            case "quit" -> {
                System.out.println("Jugador " + playerId + " sale del juego...");
                gameState.removePlayer(playerId);
            }

            case "combat" -> {
                Enemy enemy = new Enemy("Goblin", 0, 0, 30, 8);
                gameState.getCombatSystem().startCombat(playerId, enemy);
            }

            default -> {}
        }
    }
}
