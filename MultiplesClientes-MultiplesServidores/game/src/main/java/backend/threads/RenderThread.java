package backend.threads;

import backend.core.GameState;
import backend.world.World;
import backend.entities.Player;

public class RenderThread implements Runnable {

    private final GameState gameState;

    public RenderThread(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void run() {

        while (gameState.isRunning()) {

            render();

            try {
                Thread.sleep(500); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void render() {

        synchronized (gameState) {

            clearScreen();

            World world = gameState.getWorld();
            var players = gameState.getAllPlayers();

            world.render(players);

            drawUI(players);
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void drawUI(java.util.Map<Integer, Player> players) {

        System.out.println();
        System.out.println("===== STATS =====");
        
        for (var entry : players.entrySet()) {
            Player player = entry.getValue();
            boolean inCombat = gameState.getCombatSystem().isInCombat(entry.getKey());
            String combatStatus = inCombat ? " [COMBATE]" : "";
            System.out.println("Jugador " + entry.getKey() + ": " + player.getName() + 
                             " - Vida: " + player.getHealth() + "/" + player.getMaxHealth() + combatStatus);
        }
        
        System.out.println("Hora: " + gameState.getWorldTime());
        
        System.out.println("\nComandos: W A S D | quit");
        System.out.println("Leyenda: 1,2,3 = Jugadores | E = Enemigo | # = Muro");
        
        System.out.println("=================");
        System.out.print("> ");
    }
}
