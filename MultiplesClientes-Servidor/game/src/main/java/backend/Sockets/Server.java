package backend.Sockets;
import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.simple.*;
import org.json.simple.parser.*;
import backend.core.*;
import backend.entities.*;
import backend.threads.WorldClock;
import backend.commands.CommandProcessor;
import backend.world.*;

public class Server {
    public static void main(String[] args){
        try{
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Server is listening on port 12345");

            World world = new World(20, 10);
            GameState gameState = new GameState(world);
            
            AtomicInteger nextPlayerId = new AtomicInteger(1);
            
           Thread worldClThread = new Thread(new WorldClock(gameState));   
            worldClThread.start();
            
            for(;;){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress());

                int playerId = nextPlayerId.getAndIncrement();
                
                int[] pos = world.getRandomWalkablePosition();
                Player player = new Player("Player" + playerId, pos[0], pos[1]);
                gameState.addPlayer(playerId, player);
                
                System.out.println("Player " + playerId + " joined at (" + pos[0] + ", " + pos[1] + ")");

                final int currentPlayerId = playerId;
                
                new Thread(()->{
                    try{
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
                        
                        CommandProcessor commandProcessor = new CommandProcessor(gameState);
                        JSONParser parser = new JSONParser();
                        
                        boolean clientConnected = true;
                        
                        JSONObject welcomeJson = gameState.toJSON();
                        welcomeJson.put("playerId", currentPlayerId);
                        out.println(welcomeJson.toJSONString());
                        
                        while(clientConnected && gameState.isRunning()){
                            if(in.ready()){
                                String message = in.readLine();
                                if(message == null){
                                    clientConnected = false;
                                    break;
                                }
                                
                                JSONObject commandData = (JSONObject) parser.parse(message);
                                String command = (String) commandData.get("command");
                                int playerIdFromClient = ((Long) commandData.get("playerId")).intValue();
                                
                                System.out.println("Comando recibido de Player " + playerIdFromClient + ": " + command);
                                
                                commandProcessor.process(command, playerIdFromClient);
                                
                                if(command.equalsIgnoreCase("quit")){
                                    clientConnected = false;
                                }
                            }
                            
                            JSONObject stateJson = gameState.toJSON();
                            stateJson.put("playerId", currentPlayerId);
                            out.println(stateJson.toJSONString());
                            
                            Thread.sleep(100);
                        }
                        
                        System.out.println("Cliente Player " + currentPlayerId + " desconectado");
                        gameState.removePlayer(currentPlayerId);
                        clientSocket.close();
                        
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }).start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
