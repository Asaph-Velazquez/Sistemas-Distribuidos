package backend.rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * GameObjectBroker - Broker de Objetos Distribuidos
 * 
 * Este broker acts as un registro central para objetos remotos RMI.
 * Permite a los clientes descubrir y acceder a servicios remotos del juego.
 * 
 * Uso:
 *   java backend.rmi.GameObjectBroker [port]
 * 
 * Puerto por defecto: 1099
 */
public class GameObjectBroker {
    
    public static final int DEFAULT_PORT = 1099;
    public static final String SERVICE_NAME = "MatchmakingService";
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        final int finalPort;
        
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        finalPort = port;
        
        System.out.println("========================================");
        System.out.println("   BROKER DE OBJETOS DISTRIBUIDOS");
        System.out.println("========================================");
        System.out.println("Iniciando broker en puerto: " + finalPort);
        System.out.println("");
        
        try {
            // Crear o obtener el registro RMI
            Registry registry = LocateRegistry.createRegistry(finalPort);
            System.out.println("[OK] Registry RMI creado en puerto " + finalPort);
            
            // Crear e registrar el servicio de matchmaking
            RemoteMatchmakingImpl matchmakingService = new RemoteMatchmakingImpl();
            Naming.rebind("rmi://localhost:" + finalPort + "/" + SERVICE_NAME, matchmakingService);
            
            System.out.println("[OK] MatchmakingService registrado como: rmi://localhost:" + finalPort + "/" + SERVICE_NAME);
            System.out.println("");
            System.out.println("========================================");
            System.out.println("   SERVICIOS DISPONIBLES");
            System.out.println("========================================");
            System.out.println("  - MatchmakingService (rmi://localhost:" + finalPort + "/MatchmakingService)");
            System.out.println("");
            System.out.println("Métodos disponibles:");
            System.out.println("  - createGame(username, gameName, maxPlayers)");
            System.out.println("  - listGames()");
            System.out.println("  - joinGame(port)");
            System.out.println("  - updateGame(port, players)");
            System.out.println("  - deleteGame(port)");
            System.out.println("  - ping()");
            System.out.println("========================================");
            System.out.println("");
            System.out.println("Broker listo. Presiona Ctrl+C para detener.");
            System.out.println("");
            
            // Mantener el broker activo
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nDeteniendo broker...");
                try {
                    Naming.unbind("rmi://localhost:" + finalPort + "/" + SERVICE_NAME);
                    System.out.println("Broker detenido.");
                } catch (Exception e) {
                    System.out.println("Error al detener: " + e.getMessage());
                }
            }));
            
            // Esperar indefinidamente
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Error al iniciar broker: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
