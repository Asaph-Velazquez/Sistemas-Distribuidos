package backend.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteMatchmakingImpl extends UnicastRemoteObject implements RemoteMatchmaking {
    
    // Puerto fijo para matchmaking RMI
    public static final int MATCHMAKING_PORT = 1099;
    public static final String SERVICE_NAME = "MatchmakingService";
    
    // Rango de puertos dinámicos para las partidas
    public static final int MIN_GAME_PORT = 50001;
    public static final int MAX_GAME_PORT = 50100;
    
    // Almacén de partidas activas
    private final ConcurrentHashMap<Integer, GameInfo> activeGames = new ConcurrentHashMap<>();
    private final AtomicInteger nextGamePort = new AtomicInteger(MIN_GAME_PORT);
    
    // Almacén de procesos de juego
    private final ConcurrentHashMap<Integer, Process> gameProcesses = new ConcurrentHashMap<>();
    
    static class GameInfo {
        String gameName;
        String hostName;
        int port;
        int maxPlayers;
        int currentPlayers;
        long createdAt;
        
        GameInfo(String gameName, String hostName, int port, int maxPlayers) {
            this.gameName = gameName;
            this.hostName = hostName;
            this.port = port;
            this.maxPlayers = maxPlayers;
            this.currentPlayers = 1;
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    public RemoteMatchmakingImpl() throws RemoteException {
        super();
        System.out.println("=== REMOTE MATCHMAKING SERVICE ===");
        System.out.println("Puerto RMI: " + MATCHMAKING_PORT);
        System.out.println("Rango de puertos de juego: " + MIN_GAME_PORT + "-" + MAX_GAME_PORT);
    }
    
    @Override
    public synchronized String createGame(String username, String gameName, int maxPlayers) throws RemoteException {
        try {
            int port = findAvailablePort();
            
            if (port == -1) {
                return "ERROR:No hay puertos disponibles";
            }
            
            GameInfo gameInfo = new GameInfo(gameName, username, port, maxPlayers);
            activeGames.put(port, gameInfo);
            
            System.out.println("Partida creada: " + gameName + " por " + username + " en puerto " + port);
            
            // Iniciar servidor de juego
            startGameServer(port, gameName, username, maxPlayers);
            
            // Esperar a que el servidor esté listo
            Thread.sleep(2000);
            
            return "OK:" + port + ":" + gameName;
            
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
    
    @Override
    public List<String> listGames() throws RemoteException {
        List<String> games = new ArrayList<>();
        
        for (GameInfo game : activeGames.values()) {
            String info = game.gameName + ":" + game.hostName + ":" + 
                          game.port + ":" + game.currentPlayers + ":" + game.maxPlayers;
            games.add(info);
        }
        
        return games;
    }
    
    @Override
    public synchronized String joinGame(int port) throws RemoteException {
        GameInfo game = activeGames.get(port);
        
        if (game == null) {
            return "ERROR:La partida ya no existe";
        }
        if (game.currentPlayers >= game.maxPlayers) {
            return "ERROR:La partida está llena";
        }
        
        return "OK:" + game.port + ":" + game.gameName;
    }
    
    @Override
    public synchronized String updateGame(int port, int currentPlayers) throws RemoteException {
        GameInfo game = activeGames.get(port);
        
        if (game != null) {
            game.currentPlayers = currentPlayers;
            return "OK";
        }
        
        return "ERROR:Partida no encontrada";
    }
    
    @Override
    public synchronized String deleteGame(int port) throws RemoteException {
        GameInfo removed = activeGames.remove(port);
        
        if (removed != null) {
            // Terminar proceso del servidor si existe
            Process p = gameProcesses.remove(port);
            if (p != null) {
                p.destroy();
            }
            System.out.println("Partida eliminada: " + removed.gameName + " (puerto " + port + ")");
            return "OK";
        }
        
        return "ERROR:Partida no encontrada";
    }
    
    @Override
    public String ping() throws RemoteException {
        return "OK:MatchmakingService activo";
    }
    
    private int findAvailablePort() {
        for (int i = 0; i < (MAX_GAME_PORT - MIN_GAME_PORT); i++) {
            int candidatePort = nextGamePort.getAndIncrement();
            
            if (candidatePort > MAX_GAME_PORT) {
                nextGamePort.compareAndSet(candidatePort + 1, MIN_GAME_PORT);
                candidatePort = MIN_GAME_PORT;
            }
            
            if (!activeGames.containsKey(candidatePort)) {
                return candidatePort;
            }
        }
        return -1;
    }
    
    private void startGameServer(int port, String gameName, String hostName, int maxPlayers) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String javaExec = System.getProperty("java.home") + "/bin/java" + (os.contains("win") ? ".exe" : "");
            
            String classpath = System.getProperty("user.dir") + "/target/classes";
            String separator = os.contains("win") ? ";" : ":";
            
            // Agregar dependencias al classpath
            String userHome = System.getProperty("user.home");
            classpath += separator + userHome + "/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar";
            classpath += separator + userHome + "/.m2/repository/com/googlecode/lanterna/lanterna/3.1.1/lanterna-3.1.1.jar";
            
            String[] command = {
                javaExec,
                "-cp", classpath,
                "backend.Sockets.Server",
                String.valueOf(port),
                gameName,
                hostName,
                String.valueOf(maxPlayers),
                "NONE"
            };
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new java.io.File(System.getProperty("user.dir")));
            pb.inheritIO();
            
            Process process = pb.start();
            System.out.println(">>> Servidor de juego iniciado en puerto " + port);
            
            gameProcesses.put(port, process);
            
            new Thread(() -> {
                try {
                    process.waitFor();
                    System.out.println(">>> Servidor en puerto " + port + " ha terminado");
                    activeGames.remove(port);
                    gameProcesses.remove(port);
                } catch (InterruptedException e) {
                    // Ignorar
                }
            }).start();
            
        } catch (Exception e) {
            System.out.println("Error al iniciar servidor: " + e.getMessage());
        }
    }
}
