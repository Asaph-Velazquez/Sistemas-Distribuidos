package backend.Sockets;
import java.net.*;
import java.nio.Buffer;
import java.util.concurrent.*;
import java.io.*;
import org.json.simple.*;
import backend.threads.InputThread;
import backend.threads.RenderThread;
import backend.threads.ReceiverThread;
import backend.commands.CommandProcessor;
import backend.entities.Player;


public class Client{
    public static void main(String[] args){
        try{
            Socket clientSocket = new Socket("localhost", 12345);
            System.out.println("Connected to server at " + clientSocket.getInetAddress());
            
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            BlockingDeque<String> inputQueue = new LinkedBlockingDeque<>();
            InputThread inputThreadRunnable = new InputThread(inputQueue);
            Thread inputThread = new Thread(inputThreadRunnable);
            inputThread.start();
            
            ReceiverThread receiverThreadRunnable = new ReceiverThread(in);
            Thread receiverThread = new Thread(receiverThreadRunnable);
            receiverThread.start();
            
            JSONObject playerData = new JSONObject();
            playerData.put("name", "Player1");

            boolean running = true;
            System.out.println("Cliente listo. Escribe comandos:");
            
            while(running){
                try{
                    String command = inputQueue.take();
                    
                    System.out.println("Comando recibido: " + command);
                    playerData.put("command", command);
                    
                    out.println(playerData.toJSONString());
                    System.out.println("Enviado al servidor: " + playerData.toJSONString());
                    
                    if(command.equalsIgnoreCase("quit")){
                        running = false;
                    }
    
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }

            inputThreadRunnable.stop();
            receiverThreadRunnable.stop();
            
            out.close();
            in.close();
            clientSocket.close();
            System.out.println("Cliente desconectado");

        }catch(Exception e){
            e.printStackTrace();   
        }
    }

}