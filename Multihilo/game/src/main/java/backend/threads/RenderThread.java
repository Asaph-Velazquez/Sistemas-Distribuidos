package backend.threads;

import backend.combat.CombatSystem;
import backend.combat.Enemy;
import backend.core.GameState;
import backend.game.GameMode;
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
                if (gameState.getCurrentGameMode() == GameMode.Mode.Combat) {
                    Thread.sleep(2000); 
                } else {
                    Thread.sleep(500); 
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void render() {

        synchronized (gameState) {

            clearScreen();

            World world = gameState.getWorld();
            Player player = gameState.getPlayer();

            world.render(player);

            drawUI(player);
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void drawUI(Player player) {

        System.out.println();
        System.out.println("===== STATS =====");
        System.out.println("Nombre: " + player.getName());
        System.out.println("Vida: " + player.getHealth() + "/" + player.getMaxHealth());
        System.out.println("Hora: " + gameState.getWorldTime());
        System.out.println("Modo: " + gameState.getCurrentGameMode());
        
        if (gameState.getCurrentGameMode() == GameMode.Mode.Combat) {
            CombatSystem combat = gameState.getCombatSystem();
            Enemy enemy = combat.getCurrentEnemy();
            if (enemy != null) {
                System.out.println("\n--- COMBATE ---");
                System.out.println("Enemigo: " + enemy.getName());
                System.out.println("Vida Enemigo: " + enemy.getHealth() + "/" + enemy.getMaxHealth());
                System.out.println("Turno: " + (combat.isPlayerTurn() ? "TUYO" : "ENEMIGO"));
                
                String lastMessage = combat.getLastActionMessage();
                if (!lastMessage.isEmpty()) {
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
