package backend;

public class WorldClock implements Runnable {
    public GameState gameState;
    public WorldClock(GameState gameState){
        this.gameState = gameState;
    }

    @Override
    public void run(){
        while(gameState.isRunning()){
            if(gameState.getCurrentGameMode() == GameMode.Mode.Exploration){
                gameState.incrementWorldTime();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

        }
    }
}
