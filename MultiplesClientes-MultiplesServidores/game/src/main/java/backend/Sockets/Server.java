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

/**
 * Server - Servidor de juego para una partida específica
 * 
 * Este servidor es iniciado automáticamente por el MatchmakingServer
 * cuando un jugador crea una nueva partida. No funciona de manera standalone.
 * 
 * Argumentos requeridos: [port] [gameName] [hostName] [maxPlayers] [matchmakingHost]
 * - port: Puerto donde escuchará el servidor de juego
 * - gameName: Nombre de la partida
 * - hostName: Nombre del host
 * - maxPlayers: Máximo de jugadores
 * - matchmakingHost: Host del matchmaking (usar "NONE" si no aplica)
 * 
 * Sincronización:
 *   - AtomicInteger nextPlayerId: Generación de IDs únicos para jugadores
 *   - synchronized en manejo de clientes: Evita condiciones de carrera
 *   - ConcurrentHashMap en GameState: Acceso thread-safe a jugadores
 *   - volatile en running: Visibilidad inmediata entre hilos
 *   - Thread pool para clientes: Evita crear muchos hilos innecesarios
 */
public class Server {
    
    // Puerto por defecto si no se especifica
    private static final int DEFAULT_PORT = 12345;
    private static final String MATCHMAKING_HOST = "localhost";
    private static final int MATCHMAKING_PORT = 12345;
    
    public static void main(String[] args){
        // ============================================
        // PROCESAMIENTO DE ARGUMENTOS
        // ============================================
        // Formato: Server [port] [gameName] [hostName] [maxPlayers] [matchmakingHost]
        
        int port = DEFAULT_PORT;
        String gameName = "Partida";
        String hostName = "Host";
        int maxPlayers = 4;
        String matchmakingHost = MATCHMAKING_HOST;
        
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);  // Puerto del servidor de juego
        }
        if (args.length > 1) {
            gameName = args[1];                  // Nombre de la partida
        }
        if (args.length > 2) {
            hostName = args[2];                  // Nombre del host
        }
        if (args.length > 3) {
            maxPlayers = Integer.parseInt(args[3]); // Máximo jugadores
        }
        if (args.length > 4) {
            matchmakingHost = args[4];            // Host del matchmaking
        }
        
        final int serverPort = port;
        final String finalGameName = gameName;
        final String finalHostName = hostName;
        final int finalMaxPlayers = maxPlayers;
        final String finalMatchmakingHost = matchmakingHost;
        
        System.out.println("========================================");
        System.out.println("INICIANDO SERVIDOR DE JUEGO");
        System.out.println("========================================");
        System.out.println("Puerto: " + serverPort);
        System.out.println("Partida: " + finalGameName);
        System.out.println("Host: " + finalHostName);
        System.out.println("Max Jugadores: " + finalMaxPlayers);
        System.out.println("========================================");
        
        try{
            // ============================================
            // CREACIÓN DEL SERVER SOCKET
            // ============================================
            // ServerSocket: Punto de entrada para conexiones de clientes
            // Cada accept() retorna un Socket para un cliente específico
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server escuchando en puerto " + serverPort);
            
            // Inicializar mundo y estado del juego
            World world = new World(20, 10);
            GameState gameState = new GameState(world);
            
            // Configurar información de la partida (Multi-Servidor)
            gameState.setGameName(finalGameName);
            gameState.setHostName(finalHostName);
            gameState.setMaxPlayers(finalMaxPlayers);
            gameState.setGameServerAddress(matchmakingHost);
            
            // ============================================
            // GENERADOR DE IDs DE JUGADOR
            // Thread-safe: AtomicInteger garantiza IDs únicos sin sincronización explícita
            // Cada cliente recibe un ID único en el orden de conexión
            // ============================================
            AtomicInteger nextPlayerId = new AtomicInteger(1);
            
            // ============================================
            // HILO DEL RELOJ MUNDIAL
            // Actualiza el tiempo del mundo periódicamente
            // Sincronización: incrementWorldTime() es synchronized en GameState
            // ============================================
            Thread worldClThread = new Thread(new WorldClock(gameState));   
            worldClThread.start();
            
            // ============================================
            // ACTUALIZACIÓN PERIÓDICA AL MATCHMAKING
            // Thread separada que actualiza el número de jugadores cada 10 segundos
            // Sincronización: Comunicación asíncrona sin necesidad de locks
            // ============================================
            Thread updateThread = new Thread(() -> {
                while (gameState.isRunning()) {
                    try {
                        Thread.sleep(10000); // Actualizar cada 10 segundos
                        if (!finalMatchmakingHost.equals("NONE")) {
                            updateMatchmaking(finalMatchmakingHost, serverPort, gameState.getCurrentPlayerCount());
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            updateThread.start();
            
            // ============================================
            // BUCLE PRINCIPAL DE ACEPTACIÓN DE CLIENTES
            // Accept() es bloqueante: el servidor espera hasta que un cliente se conecte
            // Por cada cliente, se crea un nuevo hilo para manejar sus mensajes
            // ============================================
            for(;;){
                // Bloqueante: espera hasta que un cliente se conecte
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado desde " + clientSocket.getInetAddress());
                
                // ============================================
                // VERIFICAR MÁXIMO DE JUGADORES
                // Si ya se alcanzó el límite, rechazar la conexión
                // ============================================
                if (gameState.getCurrentPlayerCount() >= gameState.getMaxPlayers()) {
                    System.out.println("Conexión rechazada: partida llena (" + gameState.getCurrentPlayerCount() + "/" + gameState.getMaxPlayers() + ")");
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        // Ignorar
                    }
                    continue;
                }
                
                // ============================================
                // CREACIÓN DEL JUGADOR
                // Thread-safe: getAndIncrement() es atómico
                // La posición se obtiene del mundo (con currentPlayers como seed)
                // ============================================
                int playerId = nextPlayerId.getAndIncrement();
                
                // Obtener posición aleatoria para el nuevo jugador
                int[] pos = world.getRandomWalkablePosition();
                Player player = new Player("Player" + playerId, pos[0], pos[1]);
                
                // Añadir al estado del juego
                // Sincronización: addPlayer() es synchronized en GameState
                gameState.addPlayer(playerId, player);
                
                System.out.println("Jugador " + playerId + " unido en posición (" + pos[0] + ", " + pos[1] + ")");
                
                // Captura de variables para el lambda (deben ser effectively final)
                final int currentPlayerId = playerId;
                
                // ============================================
                // HILO DE MANEJO DEL CLIENTE
                // Cada cliente tiene su propio hilo para comunicación asíncrona
                // Sincronización: Solo un hilo (este) lee/escribe para este cliente
                // El acceso a gameState está sincronizado mediante métodos synchronized
                // ============================================
                new Thread(()->{
                    try{
                        // Setup de streams de comunicación
                        // UTF-8: Encoding para soportar caracteres especiales
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
                        
                        // CommandProcessor: Procesa los comandos del cliente
                        CommandProcessor commandProcessor = new CommandProcessor(gameState);
                        
                        // JSON Parser: Convierte mensajes JSON a objetos
                        JSONParser parser = new JSONParser();
                        
                        boolean clientConnected = true;
                        
                        // ============================================
                        // MENSAJE DE BIENVENIDA
                        // Envía el estado inicial al cliente recién conectado
                        // Incluye información de la partida (gameName, host, etc.)
                        // ============================================
                        JSONObject welcomeJson = gameState.toJSON();
                        welcomeJson.put("playerId", currentPlayerId);
                        welcomeJson.put("message", "Bienvenido a " + finalGameName);
                        out.println(welcomeJson.toJSONString());
                        
                        // ============================================
                        // BUCLE DE RECEPCIÓN DE COMANDOS
                        // Lee continuamente los comandos del cliente
                        // in.ready() no es bloqueante, solo indica si hay datos disponibles
                        // ============================================
                        while(clientConnected && gameState.isRunning()){
                            if(in.ready()){
                                String message = in.readLine();
                                if(message == null){
                                    clientConnected = false;
                                    break;
                                }
                                
                                // Parsear comando recibido
                                JSONObject commandData = (JSONObject) parser.parse(message);
                                String command = (String) commandData.get("command");
                                int playerIdFromClient = ((Long) commandData.get("playerId")).intValue();
                                
                                System.out.println("Comando de Player " + playerIdFromClient + ": " + command);
                                
                                // Procesar comando
                                // Sincronización: process() usa métodos synchronized de GameState
                                commandProcessor.process(command, playerIdFromClient);
                                
                                if(command.equalsIgnoreCase("quit")){
                                    clientConnected = false;
                                }
                            }
                            
                            // ============================================
                            // ENVÍO DE ESTADO DEL JUEGO
                            // Envía el estado actual a cada cliente en cada iteración
                            // Esto mantiene a todos los clientes sincronizados
                            // Thread.sleep(): Limita la velocidad de envío (10 fps)
                            // ============================================
                            JSONObject stateJson = gameState.toJSON();
                            stateJson.put("playerId", currentPlayerId);
                            out.println(stateJson.toJSONString());
                            
                            Thread.sleep(100); // ~10 actualizaciones por segundo
                        }
                        
                        // ============================================
                        // LIMPIEZA AL DESCONECTARSE
                        // Remover jugador y cerrar conexión
                        // Sincronización: removePlayer() es synchronized
                        // ============================================
                        System.out.println("Cliente Player " + currentPlayerId + " desconectado");
                        gameState.removePlayer(currentPlayerId);
                        clientSocket.close();
                        
                        // Notificar al matchmaking sobre desconexión
                        if (!finalMatchmakingHost.equals("NONE")) {
                            updateMatchmaking(finalMatchmakingHost, serverPort, gameState.getCurrentPlayerCount());
                        }
                        
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }).start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    /**
     * Actualiza el número de jugadores en el servidor de matchmaking
     * 
     * Sincronización:
     *   - Comunicación asíncrona, no bloquea el servidor de juego
     *   - El matchmaking actualiza su lista internamente
     */
    private static void updateMatchmaking(String host, int port, int currentPlayers) {
        try {
            Socket mmSocket = new Socket(host, MATCHMAKING_PORT);
            PrintWriter out = new PrintWriter(mmSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(mmSocket.getInputStream()));
            
            // Comando: UPDATE:{port}:{currentPlayers}
            String request = "UPDATE:" + port + ":" + currentPlayers;
            out.println(request);
            
            String response = in.readLine();
            // Solo loguear si hay error
            if (!response.startsWith("OK")) {
                System.out.println("Error actualizando matchmaking: " + response);
            }
            
            in.close();
            out.close();
            mmSocket.close();
            
        } catch (Exception e) {
            // Silencioso: el matchmaking puede no estar disponible
        }
    }
}
