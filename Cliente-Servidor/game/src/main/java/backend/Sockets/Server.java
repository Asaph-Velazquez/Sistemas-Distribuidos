package backend.Sockets;
import java.net.*;
import java.io.*;
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

            Player player = new Player("Hero", 1, 1);
            World world = new World(20, 10);
            GameState gameState = new GameState(player, world);
            
           Thread worldClThread = new Thread(new WorldClock(gameState));   
            worldClThread.start();
            
            for(;;){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress());

                new Thread(()->{
                    try{
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
                        
                        CommandProcessor commandProcessor = new CommandProcessor(gameState);
                        JSONParser parser = new JSONParser();
                        
                        boolean clientConnected = true;
                        
                        out.println(gameState.toJSON().toJSONString());
                        
                        while(clientConnected && gameState.isRunning()){
                            if(in.ready()){
                                String message = in.readLine();
                                if(message == null){
                                    clientConnected = false;
                                    break;
                                }
                                
                                JSONObject commandData = (JSONObject) parser.parse(message);
                                String command = (String) commandData.get("command");
                                
                                System.out.println("Comando recibido: " + command);
                                
                                commandProcessor.process(command);
                                
                                if(command.equalsIgnoreCase("quit")){
                                    clientConnected = false;
                                }
                            }
                            
                            out.println(gameState.toJSON().toJSONString());
                            
                            Thread.sleep(100);
                        }
                        
                        System.out.println("Cliente desconectado");
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
