package backend.threads;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;

public class InputThread implements Runnable {
    private BlockingDeque<String> inputQueue;
    private volatile boolean running = true;

    public InputThread(BlockingDeque<String> inputQueue) {
        this.inputQueue = inputQueue;
    }
    public void stop(){
        running = false;
    }
    @Override
    public void run(){
        Scanner sc = new Scanner(System.in);
        while(running){
            String input = sc.nextLine().trim();
            if(!input.isEmpty()){
                inputQueue.offer(input);
            }
        }
    }
}
