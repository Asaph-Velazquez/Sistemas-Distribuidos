package backend.core;

import java.util.concurrent.BlockingQueue;
import backend.combat.CombatSystem;
import backend.commands.CommandProcessor;
import backend.game.GameMode;

public class GameLoop implements Runnable {

    private GameState gameState;
    private BlockingQueue<String> inputQueue;
    private CommandProcessor commandProcessor;
    private CombatSystem combatSystem;

    public GameLoop(GameState gameState, BlockingQueue<String> inputQueue) {
        this.gameState = gameState;
        this.inputQueue = inputQueue;
        this.commandProcessor = new CommandProcessor(gameState);
        this.combatSystem = gameState.getCombatSystem();
    }

    @Override
    public void run() {

        while (gameState.isRunning()) {

            if (gameState.getCurrentGameMode() == GameMode.Mode.Exploration) {
                processExplorationInput();
                updateExploration();
            } else if (gameState.getCurrentGameMode() == GameMode.Mode.Combat) {
                updateCombat();
            }

            try {
                Thread.sleep(100); // evita 100% CPU
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processExplorationInput() {
        while (!inputQueue.isEmpty()) {
            String cmd = inputQueue.poll();
            commandProcessor.process(cmd);
        }
    }

    private void updateExploration() {
        // Aquí después irán eventos del mundo
    }

    private void updateCombat() {
        combatSystem.update(inputQueue);
    }
}
