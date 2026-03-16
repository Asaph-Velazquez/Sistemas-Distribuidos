package backend.core;

import java.util.concurrent.BlockingQueue;
import backend.commands.CommandProcessor;
import backend.entities.Player;

public class GameLoop implements Runnable {

    private GameState gameState;
    private BlockingQueue<String> inputQueue;
    private CommandProcessor commandProcessor;

    public GameLoop(GameState gameState, BlockingQueue<String> inputQueue) {
        this.gameState = gameState;
        this.inputQueue = inputQueue;
        this.commandProcessor = new CommandProcessor(gameState);
    }

    @Override
    public void run() {

        while (gameState.isRunning()) {

            processInput();

            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processInput() {
        while (!inputQueue.isEmpty()) {
            String cmd = inputQueue.poll();
            Player player = gameState.getAnyPlayer();
            if (player != null) {
                int playerId = gameState.getAllPlayers().entrySet().stream()
                    .filter(e -> e.getValue() == player)
                    .findFirst().map(e -> e.getKey()).orElse(1);
                commandProcessor.process(cmd, playerId);
            }
        }
    }
}
