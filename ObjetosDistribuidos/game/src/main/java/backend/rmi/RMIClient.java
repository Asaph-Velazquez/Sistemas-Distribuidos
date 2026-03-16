package backend.rmi;

import java.rmi.Naming;
import java.util.List;
import java.util.Scanner;

/**
 * RMIClient - Cliente RMI de prueba para el Matchmaking
 * 
 * Permite probar el broker RMI sin necesidad del cliente completo del juego.
 * 
 * Uso:
 *   java backend.rmi.RMIClient [host] [port]
 * 
 * Ejemplo:
 *   java backend.rmi.RMIClient localhost 1099
 */
public class RMIClient {
    
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 1099;
    public static final String SERVICE_NAME = "MatchmakingService";
    
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        
        System.out.println("========================================");
        System.out.println("   CLIENTE RMI - MATCHMAKING");
        System.out.println("========================================");
        System.out.println("Conectando a: rmi://" + host + ":" + port + "/" + SERVICE_NAME);
        System.out.println("");
        
        try {
            // Obtener referencia al objeto remoto
            String serviceUrl = "rmi://" + host + ":" + port + "/" + SERVICE_NAME;
            RemoteMatchmaking matchmaking = (RemoteMatchmaking) Naming.lookup(serviceUrl);
            
            System.out.println("[OK] Conectado al servicio de matchmaking");
            System.out.println("");
            
            // Probar ping
            String pingResult = matchmaking.ping();
            System.out.println("Ping: " + pingResult);
            System.out.println("");
            
            // Menú interactivo
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                System.out.println("========================================");
                System.out.println("   MENÚ DE COMANDOS");
                System.out.println("========================================");
                System.out.println("  1. create <nombre> <max>  - Crear partida");
                System.out.println("  2. list                   - Listar partidas");
                System.out.println("  3. join <puerto>         - Unirse a partida");
                System.out.println("  4. update <puerto> <n>   - Actualizar jugadores");
                System.out.println("  5. delete <puerto>        - Eliminar partida");
                System.out.println("  6. ping                   - Verificar conexión");
                System.out.println("  7. quit                   - Salir");
                System.out.println("========================================");
                System.out.print("> ");
                
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) continue;
                
                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();
                
                try {
                    switch (command) {
                        case "1":
                        case "create": {
                            String username = "Host";
                            String gameName = parts.length > 1 ? parts[1] : "MiPartida";
                            int maxPlayers = parts.length > 2 ? Integer.parseInt(parts[2]) : 4;
                            
                            System.out.println("Creando partida '" + gameName + "'...");
                            String result = matchmaking.createGame(username, gameName, maxPlayers);
                            System.out.println("Resultado: " + result);
                            break;
                        }
                        
                        case "2":
                        case "list": {
                            System.out.println("Obteniendo partidas...");
                            List<String> games = matchmaking.listGames();
                            
                            if (games.isEmpty()) {
                                System.out.println("No hay partidas disponibles.");
                            } else {
                                System.out.println("Partidas encontradas: " + games.size());
                                System.out.println("");
                                for (String game : games) {
                                    String[] info = game.split(":");
                                    System.out.println("  Nombre: " + info[0]);
                                    System.out.println("  Host: " + info[1]);
                                    System.out.println("  Puerto: " + info[2]);
                                    System.out.println("  Jugadores: " + info[3] + "/" + info[4]);
                                    System.out.println("---");
                                }
                            }
                            break;
                        }
                        
                        case "3":
                        case "join": {
                            if (parts.length < 2) {
                                System.out.println("Uso: join <puerto>");
                                break;
                            }
                            int portJoin = Integer.parseInt(parts[1]);
                            
                            System.out.println("Uniéndose a puerto " + portJoin + "...");
                            String result = matchmaking.joinGame(portJoin);
                            System.out.println("Resultado: " + result);
                            break;
                        }
                        
                        case "4":
                        case "update": {
                            if (parts.length < 3) {
                                System.out.println("Uso: update <puerto> <jugadores>");
                                break;
                            }
                            int portUpdate = Integer.parseInt(parts[1]);
                            int players = Integer.parseInt(parts[2]);
                            
                            String result = matchmaking.updateGame(portUpdate, players);
                            System.out.println("Resultado: " + result);
                            break;
                        }
                        
                        case "5":
                        case "delete": {
                            if (parts.length < 2) {
                                System.out.println("Uso: delete <puerto>");
                                break;
                            }
                            int portDelete = Integer.parseInt(parts[1]);
                            
                            System.out.println("Eliminando partida en puerto " + portDelete + "...");
                            String result = matchmaking.deleteGame(portDelete);
                            System.out.println("Resultado: " + result);
                            break;
                        }
                        
                        case "6":
                        case "ping": {
                            String result = matchmaking.ping();
                            System.out.println("Ping: " + result);
                            break;
                        }
                        
                        case "7":
                        case "quit":
                        case "exit": {
                            running = false;
                            System.out.println("¡Hasta luego!");
                            break;
                        }
                        
                        default:
                            System.out.println("Comando desconocido.");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
            
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("Error al conectar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
