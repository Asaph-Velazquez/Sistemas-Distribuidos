package backend;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args){
        Player player = new Player("Hero", 1, 1);
        World world = new World(20, 10);

        GameState gameState = new GameState(player, world);
        
        BlockingDeque<String> inputQueue = new LinkedBlockingDeque<>();
        
        Thread inputThread = new Thread(new InputThread(inputQueue));
        Thread worldClockThread = new Thread(new WorldClock(gameState));
        Thread gameLoopThread = new Thread(new GameLoop(gameState, inputQueue));
        Thread renderThread = new Thread(new RenderThread(gameState));

        inputThread.start();
        worldClockThread.start();
        gameLoopThread.start();
        renderThread.start();
    }    
}
