package backend.Sockets;
import java.net.*;
import java.nio.Buffer;
import java.util.concurrent.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import backend.threads.InputThread;
import backend.threads.RenderThread;
import backend.threads.ReceiverThread;
import backend.commands.CommandProcessor;
import backend.entities.Player;


/**
 * Client - Cliente del juego
 * 
 * Flujo de conexión:
 * 1. Se conecta al servidor de matchmaking (puerto 12345)
 * 2. Permite crear, listar o unirse a partidas
 * 3. Se desconecta del matchmaking y conecta al servidor de juego
 * 4. Juega normalmente
 * 
 * Sincronización:
 *   - BlockingDeque para cola de entrada: Thread-safe por diseño
 *   - Hilos separados para input y receiver: Procesamiento paralelo
 *   - No hay variables compartidas que requieran sincronización adicional
 *   - JSON parsing y envío son operaciones atómicas independientes
 */
public class Client {
    
    // ============================================
    // CONFIGURACIÓN
    // ============================================
    private static final String MATCHMAKING_HOST = "localhost";
    private static final int MATCHMAKING_PORT = 12345;
    private static final String DEFAULT_GAME_HOST = "localhost";
    
    // Nombre de usuario del jugador
    private static String username = "Guest";
    private static String matchmakingHost = MATCHMAKING_HOST;
    
    /**
     * Muestra un panel de login gráfico para registrar el usuario
     * 
     * Usa Swing JOptionPane para crear un diálogo simple
     * Sincronización: Solo se ejecuta en el hilo principal antes de cualquier operación
     */
    private static void showLoginPanel() {
        // Crear el panel de login
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Título
        JLabel titleLabel  = new JLabel("+=======================================+");
        JLabel titleLabel2 = new JLabel("            CLIENTE DE JUEGO             ");
        JLabel titleLabel3 = new JLabel("+=======================================+");
        titleLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        titleLabel2.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        titleLabel3.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        // Campo de texto para el nombre
        JLabel nameLabel = new JLabel("Ingresa tu nombre de jugador:");
        nameLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        JTextField nameField = new JTextField(20);
        nameField.setMaximumSize(nameField.getPreferredSize());
        nameField.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        // Opcional: Elegir servidor
        JLabel serverLabel = new JLabel("Servidor de matchmaking:");
        serverLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        JTextField serverField = new JTextField("localhost", 20);
        serverField.setMaximumSize(serverField.getPreferredSize());
        serverField.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        // Agregar componentes al panel
        panel.add(Box.createVerticalStrut(10));
        panel.add(titleLabel);
        panel.add(titleLabel2);
        panel.add(titleLabel3);
        panel.add(Box.createVerticalStrut(20));
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(serverLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(serverField);
        panel.add(Box.createVerticalStrut(20));
        
        // Mostrar el diálogo
        int result = JOptionPane.showConfirmDialog(
            null, 
            panel, 
            "Login - Juego RPG",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String server = serverField.getText().trim();
            
            if (!name.isEmpty()) {
                username = name;
            }
            
            // Guardar el servidor elegido
            if (!server.isEmpty()) {
                matchmakingHost = server;
            }
        } else {
            // El usuario canceló
            JOptionPane.showMessageDialog(null, "Has cancelado. Saliendo...", "Adiós", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
    
    public static void main(String[] args){
        
        // ============================================
        // PANEL DE LOGIN GRÁFICO
        // Muestra un diálogo para que el usuario ingrese su nombre
        // JOptionPane.showConfirmDialog es modal y bloquea hasta que el usuario responda
        // Sincronización: Solo se ejecuta en el hilo principal
        // ============================================
        showLoginPanel();
        
        // Si viene por argumento, usarlo (sobrescribe el valor del panel)
        if (args.length > 0) {
            username = args[0];
        }
        
        System.out.println("========================================");
        System.out.println("       CLIENTE DE JUEGO - RPG TEXT");
        System.out.println("========================================");
        System.out.println("Bienvenido, " + username + "!");
        System.out.println("Conectando al servidor: " + matchmakingHost);
        System.out.println("");
        
        // ============================================
        // FASE 1: MATCHMAKING
        // Conectar al servidor de matchmaking para buscar/crear partida
        // ============================================
        
        MatchmakingClient matchmakingClient = new MatchmakingClient(matchmakingHost, MATCHMAKING_PORT);
        
        if (!matchmakingClient.connect()) {
            System.out.println("No se pudo conectar al servidor de matchmaking.");
            System.out.println("Verifica que el servidor esté corriendo en " + matchmakingHost);
            System.out.println("Saliendo...");
            return;
        }
        
        int gamePort = -1;
        String gameName = "";
        
        // ============================================
        // MENÚ PRINCIPAL DE MATCHMAKING
        // Permite crear, listar, unirse o salir
        // Sincronización: Solo el hilo principal accede a matchmakingClient
        // ============================================
        while (gamePort == -1) {
            System.out.println("\n========================================");
            System.out.println("       MENÚ DE PARTIDAS");
            System.out.println("========================================");
            System.out.println("Comandos disponibles:");
            System.out.println("  create <nombre> <max_jugadores>  - Crear nueva partida");
            System.out.println("  list                            - Ver partidas disponibles");
            System.out.println("  join <puerto>                   - Unirse a una partida");
            System.out.println("  quit                            - Salir del juego");
            System.out.println("========================================");
            System.out.print("> ");
            
            try {
                BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));
                String input = consoleIn.readLine();
                
                if (input == null || input.trim().isEmpty()) {
                    continue;
                }
                
                String[] parts = input.trim().split("\\s+");
                String command = parts[0].toLowerCase();
                
                switch (command) {
                    case "create": {
                        // create <nombre> <max_jugadores>
                        String name = (parts.length > 1) ? parts[1] : "MiPartida";
                        int maxPlayers = (parts.length > 2) ? Integer.parseInt(parts[2]) : 4;
                        
                        System.out.println("Creando partida '" + name + "' con max " + maxPlayers + " jugadores...");
                        
                        // Llamar al matchmaking para crear la partida
                        MatchmakingClient.CreateGameResult result = 
                            matchmakingClient.createGame(username, name, maxPlayers);
                        
                        if (result.success) {
                            System.out.println("========================================");
                            System.out.println("PARTIDA CREADA Y SERVIDOR INICIADO!");
                            System.out.println("========================================");
                            System.out.println("Nombre: " + result.gameName);
                            System.out.println("Puerto: " + result.port);
                            System.out.println("");
                            System.out.println("Esperando a que otros jugadores se unan...");
                            System.out.println("Usa 'list' para ver otras partidas disponibles");
                            System.out.println("");
                            
                            // Conectar directamente al puerto asignado
                            gamePort = result.port;
                            gameName = result.gameName;
                        } else {
                            System.out.println("Error al crear partida: " + result.error);
                        }
                        break;
                    }
                    
                    case "list": {
                        // list - Ver todas las partidas disponibles
                        System.out.println("Obteniendo lista de partidas...");
                        
                        MatchmakingClient.GameInfo[] games = matchmakingClient.listGames();
                        
                        System.out.println("\n========================================");
                        System.out.println("       PARTIDAS DISPONIBLES");
                        System.out.println("========================================");
                        
                        if (games.length == 0) {
                            System.out.println("No hay partidas disponibles.");
                            System.out.println("Crea una nueva partida con 'create'!");
                        } else {
                            System.out.println("Partidas encontradas: " + games.length);
                            System.out.println("");
                            
                            // Log de puertos disponibles
                            System.out.println(">>> PUERTOS DISPONIBLES:");
                            StringBuilder portsInfo = new StringBuilder();
                            for (MatchmakingClient.GameInfo game : games) {
                                game.print();
                                if (portsInfo.length() > 0) portsInfo.append(", ");
                                portsInfo.append(game.port);
                            }
                            System.out.println("");
                            System.out.println("Para unirte, usa: join <puerto>");
                            System.out.println("Puertos: " + portsInfo.toString());
                        }
                        break;
                    }
                    
                    case "join": {
                        // join <puerto> - Unirse a una partida existente
                        if (parts.length < 2) {
                            System.out.println("Uso: join <puerto>");
                            System.out.println("Usa 'list' para ver los puertos disponibles.");
                            break;
                        }
                        
                        int port = Integer.parseInt(parts[1]);
                        System.out.println("Uniéndose a la partida en puerto " + port + "...");
                        
                        MatchmakingClient.JoinGameResult result = 
                            matchmakingClient.joinGame(port);
                        
                        if (result.success) {
                            System.out.println("========================================");
                            System.out.println("¡CONECTADO A LA PARTIDA!");
                            System.out.println("========================================");
                            System.out.println("Partida: " + result.gameName);
                            System.out.println("Puerto: " + result.port);
                            
                            gamePort = result.port;
                            gameName = result.gameName;
                        } else {
                            System.out.println("Error al unirse: " + result.error);
                        }
                        break;
                    }
                    
                    case "quit": {
                        // Salir del juego
                        System.out.println("¡Hasta luego!");
                        matchmakingClient.disconnect();
                        return;
                    }
                    
                    default:
                        System.out.println("Comando desconocido. Usa: create, list, join, o quit");
                }
                
            } catch (IOException e) {
                System.out.println("Error de lectura: " + e.getMessage());
            }
        }
        
        // ============================================
        // FASE 2: CONEXIÓN AL SERVIDOR DE JUEGO
        // Desconectar del matchmaking y conectar al juego
        // Sincronización: Solo un hilo haciendo esta transición
        // ============================================
        
        System.out.println("\nConectando al servidor de juego en puerto " + gamePort + "...");
        matchmakingClient.disconnect();
        
        // Conectar al servidor de juego
        connectToGame(DEFAULT_GAME_HOST, gamePort);
    }
    
    /**
     * Conecta al servidor de juego y maneja la sesión
     * 
     * Sincronización:
     *   - BlockingDeque: Implementado para thread-safe push/pop
     *   - Cada hilo (input, receiver) trabaja independientemente
     *   - No hay objetos compartidos entre hilos que requieran sincronización
     */
    private static void connectToGame(String host, int port) {
        try {
            // ============================================
            // CONEXIÓN AL SERVIDOR DE JUEGO
            // Socket: Canal de comunicación bidireccional
            // Sincronización: TCP garantiza orden de mensajes
            // ============================================
            Socket clientSocket = new Socket(host, port);
            System.out.println("Conectado al servidor de juego en " + host + ":" + port);
            
            // Streams de comunicación
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            // ============================================
            // MENSAJE DE BIENVENIDA
            // Lee el estado inicial del juego
            // Sincronización: Primer mensaje, debe llegar completo antes de continuar
            // ============================================
            String firstMessage = in.readLine();
            JSONObject firstJson = (JSONObject) new JSONParser().parse(firstMessage);
            int myPlayerId = ((Long) firstJson.get("playerId")).intValue();
            
            String welcomeMsg = (String) firstJson.getOrDefault("message", "");
            String gameName = (String) firstJson.getOrDefault("gameName", "Unknown");
            String hostName = (String) firstJson.getOrDefault("hostName", "Unknown");
            int maxPlayers = ((Long) firstJson.getOrDefault("maxPlayers", 4)).intValue();
            
            System.out.println("\n========================================");
            System.out.println("       BIENVENIDO AL JUEGO");
            System.out.println("========================================");
            System.out.println("Tu ID de jugador: " + myPlayerId);
            System.out.println("Partida: " + gameName);
            System.out.println("Host: " + hostName);
            System.out.println("Max jugadores: " + maxPlayers);
            if (!welcomeMsg.isEmpty()) {
                System.out.println("Mensaje: " + welcomeMsg);
            }
            System.out.println("========================================\n");
            
            // ============================================
            // THREADS AUXILIARES
            // Thread-safe: BlockingDeque para comunicación entre hilos
            // ============================================
            
            // InputThread: Lee del teclado y pone en cola
            // BlockingDeque.put() es thread-safe
            BlockingDeque<String> inputQueue = new LinkedBlockingDeque<>();
            InputThread inputThreadRunnable = new InputThread(inputQueue);
            Thread inputThread = new Thread(inputThreadRunnable);
            inputThread.start();
            
            // ReceiverThread: Lee del servidor y muestra estado
            // No comparte estado con otros hilos, solo lee
            ReceiverThread receiverThreadRunnable = new ReceiverThread(in);
            Thread receiverThread = new Thread(receiverThreadRunnable);
            receiverThread.start();
            
            // ============================================
            // BUCLE PRINCIPAL DE ENVÍO DE COMANDOS
            // Toma comandos de la cola y los envía al servidor
            // Sincronización: BlockingDeque.take() es thread-safe
            // ============================================
            JSONObject playerData = new JSONObject();
            playerData.put("playerId", myPlayerId);

            boolean running = true;
            System.out.println("Cliente listo. Escribe comandos (usa 'help' para ver comandos):");
            
            while(running){
                try{
                    // Bloqueante: espera hasta que haya un comando disponible
                    // Thread-safe: tak() es synchronized internamente
                    String command = inputQueue.take();
                    
                    System.out.println("Enviando comando: " + command);
                    playerData.put("command", command);
                    
                    // Enviar al servidor
                    // PrintWriter.println() es thread-safe
                    out.println(playerData.toJSONString());
                    System.out.println("Enviado: " + playerData.toJSONString());
                    
                    // Verificar si debe salir
                    if(command.equalsIgnoreCase("quit")){
                        running = false;
                    }
    
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }

            // ============================================
            // LIMPIEZA AL SALIR
            // Detener threads y cerrar conexiones
            // Sincronización: cada thread tiene su propio estado
            // ============================================
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
