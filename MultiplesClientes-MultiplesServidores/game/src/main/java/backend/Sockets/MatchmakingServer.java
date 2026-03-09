package backend.Sockets;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * MatchmakingServer - Servidor central de matchmaking
 * 
 * Este servidor gestiona el registro y descubrimiento de partidas.
 * Permite a los clientes:
 *   - Crear nuevas partidas (se les asigna un puerto dinámico)
 *   - Listar partidas disponibles
 *   - Unirse a una partida existente
 * 
 * Sincronización:
 *   - ConcurrentHashMap para almacenar las partidas de forma thread-safe
 *   - AtomicInteger para generar IDs de partida únicos
 *   - Synchronized en métodos que modifican la lista de partidas
 */
public class MatchmakingServer {
    
    // Puerto fijo donde escuchará el servidor de matchmaking
    public static final int MATCHMAKING_PORT = 12345;
    
    // Range de puertos dinámicos para las partidas (50001-50100)
    public static final int MIN_GAME_PORT = 50001;
    public static final int MAX_GAME_PORT = 50100;
    
    // Thread-safe: Almacena las partidas activas {port -> GameInfo}
    // ConcurrentHashMap permite acceso concurrente sin bloqueos explícitos
    private static final ConcurrentHashMap<Integer, GameInfo> activeGames = new ConcurrentHashMap<>();
    
    // Thread-safe: Generador de IDs únicos para partidas
    // AtomicInteger garantiza atomicidad en la incrementación
    private static final AtomicInteger nextGamePort = new AtomicInteger(MIN_GAME_PORT);
    
    /**
     * GameInfo - Información de una partida registrada
     */
    static class GameInfo {
        String gameName;      // Nombre de la partida
        String hostName;      // Nombre del host (creador)
        int port;             // Puerto del servidor de juego
        int maxPlayers;       // Máximo de jugadores permitidos
        int currentPlayers;   // Jugadores actuales (se actualiza periódicamente)
        long createdAt;       // Timestamp de creación
        
        GameInfo(String gameName, String hostName, int port, int maxPlayers) {
            this.gameName = gameName;
            this.hostName = hostName;
            this.port = port;
            this.maxPlayers = maxPlayers;
            this.currentPlayers = 1; // El host cuenta como 1
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(MATCHMAKING_PORT);
            System.out.println("=== MATCHMAKING SERVER ===");
            System.out.println("Escuchando en puerto " + MATCHMAKING_PORT);
            System.out.println("Rango de puertos de juego: " + MIN_GAME_PORT + "-" + MAX_GAME_PORT);
            System.out.println("Comandos disponibles:");
            System.out.println("  CREATE:{username}:{gameName}:{maxPlayers}");
            System.out.println("  LIST");
            System.out.println("  UPDATE:{port}:{currentPlayers}");
            System.out.println("  DELETE:{port}");
            System.out.println("============================\n");
            
            // Hilo limpio de partidas inactivas (más de 2 horas)
            // Sincronización: Este hilo limpia partidas que ya no están activas
            ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
            cleaner.scheduleAtFixedRate(() -> {
                long twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000);
                // ConcurrentHashMap iteration es thread-safe
                activeGames.entrySet().removeIf(entry -> entry.getValue().createdAt < twoHoursAgo);
            }, 30, 30, TimeUnit.MINUTES);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
                
                // Cada cliente se maneja en un hilo separado
                // Sincronización: El manejo de cada cliente es independiente
                new Thread(() -> handleClient(clientSocket)).start();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Maneja las solicitudes de un cliente de matchmaking
     * 
     * Sincronización:
     *   - El acceso a activeGames es thread-safe gracias a ConcurrentHashMap
     *   - No necesitamos bloques synchronized adicionales para operaciones simples
     */
    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
            
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Solicitud recibida: " + message);
                String response = processRequest(message.trim());
                out.println(response);
                System.out.println("Respuesta enviada: " + response);
                
                // Si es un comando de desconexión, salir
                if (message.startsWith("QUIT")) {
                    break;
                }
            }
            
            clientSocket.close();
            System.out.println("Cliente desconectado del matchmaking");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Procesa las solicitudes del cliente
     * 
     * Sincronización:
     *   - ConcurrentHashMap.putIfAbsent() es atómico
     *   - ConcurrentHashMap.remove() es atómico
     *   - iteration sobre ConcurrentHashMap es thread-safe
     */
    private static String processRequest(String request) {
        try {
            String[] parts = request.split(":");
            String command = parts[0].toUpperCase();
            
            switch (command) {
                case "CREATE": {
                    // CREATE:{username}:{gameName}:{maxPlayers}
                    String username = parts[1];
                    String gameName = parts[2];
                    int maxPlayers = Integer.parseInt(parts[3]);
                    
                    // Buscar un puerto disponible
                    // Sincronización: AtomicInteger garantiza puerto único
                    int port = findAvailablePort();
                    
                    if (port == -1) {
                        return "ERROR:No hay puertos disponibles";
                    }
                    
                    // Registrar la partida
                    // Sincronización: putIfAbsent es atómico, previene duplicados
                    GameInfo gameInfo = new GameInfo(gameName, username, port, maxPlayers);
                    activeGames.put(port, gameInfo);
                    
                    System.out.println("Partida creada: " + gameName + " por " + username + " en puerto " + port);
                    
                    // ============================================================
                    // INICIAR SERVIDOR DE JUEGO AUTOMÁTICAMENTE
                    // Creamos un nuevo proceso Java que ejecuta el Server
                    // Esto permite que otros jugadores se unan inmediatamente
                    // Sincronización: El proceso se inicia de forma asíncrona
                    // ============================================================
                    startGameServer(port, gameName, username, maxPlayers);
                    
                    // Esperar a que el servidor de juego esté listo
                    // Sin esto, el cliente intentaría conectar antes de que el servidor escuche
                    Thread.sleep(3000);
                    
                    return "OK:" + port + ":" + gameName;
                }
                
                case "LIST": {
                    // LIST - Devuelve todas las partidas disponibles
                    // Sincronización: ConcurrentHashMap.values() es una vista thread-safe
                    StringBuilder sb = new StringBuilder("OK:");
                    boolean first = true;
                    
                    for (GameInfo game : activeGames.values()) {
                        if (!first) sb.append(";");
                        // Formato: name:host:port:current:max
                        sb.append(game.gameName).append(":")
                          .append(game.hostName).append(":")
                          .append(game.port).append(":")
                          .append(game.currentPlayers).append(":")
                          .append(game.maxPlayers);
                        first = false;
                    }
                    
                    return sb.toString();
                }
                
                case "UPDATE": {
                    // UPDATE:{port}:{currentPlayers}
                    // Actualiza el número de jugadores en una partida
                    int port = Integer.parseInt(parts[1]);
                    int currentPlayers = Integer.parseInt(parts[2]);
                    
                    // Sincronización: get y put son thread-safe en ConcurrentHashMap
                    GameInfo game = activeGames.get(port);
                    if (game != null) {
                        game.currentPlayers = currentPlayers;
                        return "OK";
                    }
                    return "ERROR:Partida no encontrada";
                }
                
                case "DELETE": {
                    // DELETE:{port}
                    // Elimina una partida del registro
                    int port = Integer.parseInt(parts[1]);
                    
                    // Sincronización: remove es atómico
                    GameInfo removed = activeGames.remove(port);
                    if (removed != null) {
                        System.out.println("Partida eliminada: " + removed.gameName + " (puerto " + port + ")");
                        return "OK";
                    }
                    return "ERROR:Partida no encontrada";
                }
                
                case "JOIN": {
                    // JOIN:{port}
                    // Verifica si una partida existe y tiene espacio
                    int port = Integer.parseInt(parts[1]);
                    
                    GameInfo game = activeGames.get(port);
                    if (game == null) {
                        return "ERROR:La partida ya no existe";
                    }
                    if (game.currentPlayers >= game.maxPlayers) {
                        return "ERROR:La partida está llena";
                    }
                    return "OK:" + game.port + ":" + game.gameName;
                }
                
                default:
                    return "ERROR:Comando desconocido";
            }
            
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
    
    /**
     * Busca un puerto disponible en el rango configurado
     * 
     * Sincronización:
     *   - Usa AtomicInteger para garantizar que no se asignen puertos duplicados
     *   - El bucle + putIfAbsent es una operación atómica para encontrar puerto libre
     */
    private static int findAvailablePort() {
        // Intentar hasta 100 puertos (rango completo)
        for (int i = 0; i < (MAX_GAME_PORT - MIN_GAME_PORT); i++) {
            int candidatePort = nextGamePort.getAndIncrement();
            
            // Si llegamos al límite, reiniciar
            if (candidatePort > MAX_GAME_PORT) {
                // Sincronización: reset atómico mediante compareAndSet
                nextGamePort.compareAndSet(candidatePort + 1, MIN_GAME_PORT);
                candidatePort = MIN_GAME_PORT;
            }
            
            // Verificar si el puerto ya está en uso
            // Sincronización: putIfAbsent retorna null si no existe la key
            if (!activeGames.containsKey(candidatePort)) {
                return candidatePort;
            }
        }
        return -1; // No hay puertos disponibles
    }
    
    /**
     * Inicia el servidor de juego en un proceso separado
     * 
     * Cuando un jugador crea una partida, automáticamente se levanta
     * un servidor de juego en el puerto asignado.
     * 
     * Sincronización:
     *   - ProcessBuilder.start() crea un proceso independiente del sistema
     *   - El proceso hijo corre en su propio hilo/jvm
     *   - No comparte memoria con el proceso padre
     */
    private static void startGameServer(int port, String gameName, String hostName, int maxPlayers) {
        try {
            // Construir el comando para ejecutar el servidor
            // Detectar el ejecutable de Java (funciona en Windows y Linux)
            String os = System.getProperty("os.name").toLowerCase();
            String javaExec = System.getProperty("java.home") + "/bin/java" + (os.contains("win") ? ".exe" : "");
            
            // Obtener el classpath con las dependencias de Maven
            // Primero intentamos obtener el classpath desde Maven, si no funciona usamos el local
            String classpath = getMavenClasspath();
            
            // En Windows, el classpath usa ; como separador, en Linux/Mac usa :
            String separator = os.contains("win") ? ";" : ":";
            
            String[] command;
            
            if (os.contains("win")) {
                command = new String[] {
                    javaExec,
                    "-cp", classpath,
                    "backend.Sockets.Server",
                    String.valueOf(port),
                    gameName,
                    hostName,
                    String.valueOf(maxPlayers),
                    "NONE"
                };
            } else {
                command = new String[] {
                    javaExec,
                    "-cp", classpath,
                    "backend.Sockets.Server",
                    String.valueOf(port),
                    gameName,
                    hostName,
                    String.valueOf(maxPlayers),
                    "NONE"
                };
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.inheritIO(); // Mostrar output del servidor en consola del matchmaking
            
            Process process = pb.start();
            
            System.out.println(">>> Servidor de juego iniciado en puerto " + port + " (PID: " + process.pid() + ")");
            
            // Guardar referencia del proceso para poder cerrarlo después
            // Thread-safe: ConcurrentHashMap para procesos
            gameProcesses.put(port, process);
            
            // Hilo para monitorear cuando termina el proceso
            new Thread(() -> {
                try {
                    process.waitFor();
                    System.out.println(">>> Servidor de juego en puerto " + port + " ha terminado");
                    activeGames.remove(port);
                    gameProcesses.remove(port);
                } catch (InterruptedException e) {
                    // Ignorar
                }
            }).start();
            
        } catch (Exception e) {
            System.out.println("Error al iniciar servidor de juego: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene el classpath completo de Maven incluyendo las dependencias
     * 
     * Intenta primero usar 'mvn dependency:build-classpath' para obtener
     * el classpath completo. Si falla, usa un fallback con las dependencias locales.
     */
    private static String getMavenClasspath() {
        String os = System.getProperty("os.name").toLowerCase();
        String separator = os.contains("win") ? ";" : ":";
        
        // Primero, probar con mvn dependency:build-classpath
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "dependency:build-classpath", "-Dmdep.outputFile=/tmp/cp.txt");
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            
            if (exitCode == 0) {
                // Leer el archivo de classpath
                File cpFile = new File("/tmp/cp.txt");
                if (cpFile.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(cpFile));
                    String mavenCp = reader.readLine();
                    reader.close();
                    cpFile.delete();
                    
                    if (mavenCp != null && !mavenCp.isEmpty()) {
                        // Agregar target/classes al inicio
                        String projectClasses = System.getProperty("user.dir") + "/target/classes";
                        return projectClasses + separator + mavenCp;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("No se pudo obtener classpath de Maven: " + e.getMessage());
        }
        
        // Fallback: construir classpath manualmente con dependencias comunes
        // Esto funciona si las dependencias ya están descargadas en .m2/repository
        String userHome = System.getProperty("user.home");
        String m2Repo = userHome + "/.m2/repository";
        
        StringBuilder fallbackCp = new StringBuilder();
        fallbackCp.append(System.getProperty("user.dir")).append("/target/classes").append(separator);
        
        // Agregar json-simple
        fallbackCp.append(m2Repo).append("/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar").append(separator);
        
        // Agregar lanterna
        fallbackCp.append(m2Repo).append("/com/googlecode/lanterna/lanterna/3.1.1/lanterna-3.1.1.jar");
        
        return fallbackCp.toString();
    }
    
    // Almacén de procesos de juego activos
    // Thread-safe: ConcurrentHashMap para acceso concurrente
    private static final ConcurrentHashMap<Integer, Process> gameProcesses = new ConcurrentHashMap<>();
}
