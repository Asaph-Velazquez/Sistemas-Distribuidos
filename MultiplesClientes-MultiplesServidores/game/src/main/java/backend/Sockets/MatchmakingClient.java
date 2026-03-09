package backend.Sockets;

import java.net.*;
import java.io.*;

/**
 * MatchmakingClient - Cliente para el servidor de matchmaking
 * 
 * Permite a los jugadores:
 *   - Conectarse al servidor de matchmaking
 *   - Crear nuevas partidas
 *   - Listar partidas disponibles
 *   - Unirse a una partida existente
 * 
 * Sincronización:
 *   - Cada operación es independiente, no requiere sincronización interna
 *   - La comunicación TCP es manejada por el sistema operativo
 *   - Este cliente es single-threaded para el menú, pero puede manejar
 *     la conexión mientras el juego corre en otra thread
 */
public class MatchmakingClient {
    
    // Dirección del servidor de matchmaking
    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    public MatchmakingClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Conecta al servidor de matchmaking
     * 
     * Sincronización:
     *   - Socket connection es thread-safe a nivel del SO
     *   - Streams son creados una vez y compartidos
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Conectado al servidor de matchmaking en " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.out.println("Error al conectar: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Crea una nueva partida
     * 
     * Formato de solicitud: CREATE:{username}:{gameName}:{maxPlayers}
     * Formato de respuesta: OK:{port}:{gameName} o ERROR:{message}
     * 
     * Sincronización:
     *   - out.println() es thread-safe (synchronized internamente)
     *   - La respuesta es procesada en orden (FIFO)
     */
    public CreateGameResult createGame(String username, String gameName, int maxPlayers) {
        try {
            String request = "CREATE:" + username + ":" + gameName + ":" + maxPlayers;
            out.println(request);
            
            String response = in.readLine();
            System.out.println("Respuesta: " + response);
            
            if (response.startsWith("OK:")) {
                String[] parts = response.split(":");
                int port = Integer.parseInt(parts[1]);
                String name = parts[2];
                return new CreateGameResult(true, port, name, "");
            } else {
                return new CreateGameResult(false, -1, "", response.substring(6));
            }
        } catch (IOException e) {
            return new CreateGameResult(false, -1, "", e.getMessage());
        }
    }
    
    /**
     * Lista todas las partidas disponibles
     * 
     * Formato de solicitud: LIST
     * Formato de respuesta: OK:{game1};{game2};... o OK: (vacío)
     * Formato de juego: name:host:port:current:max
     * 
     * Sincronización:
     *   - Iteración sobre la lista de juegos es thread-safe en el servidor
     *   - El cliente recibe una copia snapshots de las partidas
     */
    public GameInfo[] listGames() {
        try {
            out.println("LIST");
            
            String response = in.readLine();
            System.out.println("Respuesta: " + response);
            
            if (response.startsWith("OK:")) {
                String data = response.substring(3);
                if (data.isEmpty()) {
                    return new GameInfo[0];
                }
                
                String[] games = data.split(";");
                GameInfo[] result = new GameInfo[games.length];
                
                for (int i = 0; i < games.length; i++) {
                    String[] parts = games[i].split(":");
                    result[i] = new GameInfo(
                        parts[0],  // gameName
                        parts[1],  // hostName
                        Integer.parseInt(parts[2]),  // port
                        Integer.parseInt(parts[3]),  // currentPlayers
                        Integer.parseInt(parts[4])   // maxPlayers
                    );
                }
                return result;
            }
            return new GameInfo[0];
        } catch (Exception e) {
            System.out.println("Error al listar juegos: " + e.getMessage());
            return new GameInfo[0];
        }
    }
    
    /**
     * Se une a una partida existente
     * 
     * Formato de solicitud: JOIN:{port}
     * Formato de respuesta: OK:{port}:{gameName} o ERROR:{message}
     * 
     * Sincronización:
     *   - Verificación de espacio es atómica en el servidor
     *   - No hay condiciones de carrera entre JOIN y UPDATE
     */
    public JoinGameResult joinGame(int port) {
        try {
            String request = "JOIN:" + port;
            out.println(request);
            
            String response = in.readLine();
            System.out.println("Respuesta: " + response);
            
            if (response.startsWith("OK:")) {
                String[] parts = response.split(":");
                int actualPort = Integer.parseInt(parts[1]);
                String gameName = parts[2];
                return new JoinGameResult(true, actualPort, gameName, "");
            } else {
                return new JoinGameResult(false, -1, "", response.substring(6));
            }
        } catch (IOException e) {
            return new JoinGameResult(false, -1, "", e.getMessage());
        }
    }
    
    /**
     * Desconecta del servidor de matchmaking
     * 
     * Sincronización:
     *   - Cierre de streams y socket es thread-safe
     *   - Debe llamarse antes de conectar al servidor de juego
     */
    public void disconnect() {
        try {
            if (out != null) out.println("QUIT");
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Desconectado del matchmaking");
        } catch (IOException e) {
            System.out.println("Error al desconectar: " + e.getMessage());
        }
    }
    
    // ============================================
    // CLASES AUXILIARES PARA RESULTADOS
    // ============================================
    
    public static class CreateGameResult {
        public boolean success;
        public int port;
        public String gameName;
        public String error;
        
        public CreateGameResult(boolean success, int port, String gameName, String error) {
            this.success = success;
            this.port = port;
            this.gameName = gameName;
            this.error = error;
        }
    }
    
    public static class JoinGameResult {
        public boolean success;
        public int port;
        public String gameName;
        public String error;
        
        public JoinGameResult(boolean success, int port, String gameName, String error) {
            this.success = success;
            this.port = port;
            this.gameName = gameName;
            this.error = error;
        }
    }
    
    /**
     * GameInfo - Información de una partida disponible
     */
    public static class GameInfo {
        public String gameName;
        public String hostName;
        public int port;
        public int currentPlayers;
        public int maxPlayers;
        
        public GameInfo(String gameName, String hostName, int port, int currentPlayers, int maxPlayers) {
            this.gameName = gameName;
            this.hostName = hostName;
            this.port = port;
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }
        
        public void print() {
            System.out.printf("  [%d] %s (Host: %s) - %d/%d jugadores%n", 
                port, gameName, hostName, currentPlayers, maxPlayers);
        }
    }
}
