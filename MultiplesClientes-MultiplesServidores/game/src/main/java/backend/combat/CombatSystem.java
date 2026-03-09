package backend.combat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import backend.core.GameState;
import backend.entities.Player;

public class CombatSystem {

    private GameState gameState;
    private Map<Integer, CombatContext> playerCombats = new ConcurrentHashMap<>();

    public CombatSystem(GameState gameState) {
        this.gameState = gameState;
    }

    public boolean isInCombat(int playerId) {
        return playerCombats.containsKey(playerId);
    }

    public Enemy getCurrentEnemy(int playerId) {
        CombatContext ctx = playerCombats.get(playerId);
        return ctx != null ? ctx.enemy : null;
    }

    public boolean isPlayerTurn(int playerId) {
        CombatContext ctx = playerCombats.get(playerId);
        return ctx != null && ctx.playerTurn;
    }

    public String getLastActionMessage(int playerId) {
        CombatContext ctx = playerCombats.get(playerId);
        return ctx != null ? ctx.lastActionMessage : "";
    }

    public void startCombat(int playerId, Enemy enemy) {
        CombatContext ctx = new CombatContext(enemy);
        ctx.lastActionMessage = "¡Combate iniciado contra " + enemy.getName() + "!";
        playerCombats.put(playerId, ctx);
    }

    public void processCommand(int playerId, String command) {
        CombatContext ctx = playerCombats.get(playerId);
        if (ctx == null || ctx.enemy == null || !ctx.playerTurn) return;

        String cmd = command.toLowerCase().trim();
        Player player = gameState.getPlayer(playerId);
        if (player == null) return;

        switch (cmd) {
            case "attack", "a" -> {
                int damage = player.dealDamage();
                ctx.enemy.takeDamage(damage);
                ctx.lastActionMessage = "Atacas a " + ctx.enemy.getName() + " por " + damage + " de daño!";

                if (ctx.enemy.isAlive()) {
                    processEnemyTurn(ctx, player);
                }
            }

            case "run", "r" -> {
                ctx.lastActionMessage = "¡Escapaste del combate!";
                playerCombats.remove(playerId);
            }

            default -> {
                ctx.lastActionMessage = "Comando inválido. Usa 'attack' ('a') o 'run' ('r').";
            }
        }

        checkCombatEnd(ctx, playerId, player);
    }

    private void processEnemyTurn(CombatContext ctx, Player player) {
        if (!ctx.enemy.isAlive()) return;

        int damage = ctx.enemy.dealDamage();
        player.takeDamage(damage);

        ctx.lastActionMessage += "\n" + ctx.enemy.getName() + " te ataca por " + damage + " de daño!";

        ctx.playerTurn = true;
    }

    private void checkCombatEnd(CombatContext ctx, int playerId, Player player) {
        if (player == null) return;

        if (!player.isAlive()) {
            ctx.lastActionMessage = "¡HAS MUERTO! Game Over.";
            playerCombats.remove(playerId);
            return;
        }

        if (ctx.enemy != null && !ctx.enemy.isAlive()) {
            ctx.lastActionMessage = "¡Victoria! Derrotaste a " + ctx.enemy.getName() + "!";

            int enemyX = ctx.enemy.getX();
            int enemyY = ctx.enemy.getY();
            gameState.getWorld().removeEnemy(ctx.enemy);

            int dx = enemyX - player.getX();
            int dy = enemyY - player.getY();
            player.move(dx, dy);

            playerCombats.remove(playerId);
        }
    }

    public void removePlayer(int playerId) {
        playerCombats.remove(playerId);
    }

    private static class CombatContext {
        Enemy enemy;
        boolean playerTurn = true;
        String lastActionMessage = "";

        CombatContext(Enemy enemy) {
            this.enemy = enemy;
        }
    }
}
