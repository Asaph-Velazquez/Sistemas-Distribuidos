package backend.config;

import backend.service.GameService;
import backend.service.MatchmakingService;
import backend.core.GameState;
import backend.entities.Player;
import backend.combat.Enemy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final MatchmakingService matchmakingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionToPort = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayerName = new ConcurrentHashMap<>();

    public GameWebSocketHandler(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Integer port = sessionToPort.remove(sessionId);
        String playerName = sessionToPlayerName.remove(sessionId);
        sessions.remove(sessionId);

        if (port != null && playerName != null) {
            GameService gameService = matchmakingService.getGameService(port);
            if (gameService != null) {
                broadcastGameState(port);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ObjectNode json = objectMapper.readValue(payload, ObjectNode.class);
        String type = json.has("type") ? json.get("type").asText() : "";

        String sessionId = session.getId();

        switch (type) {
            case "join":
                handleJoin(sessionId, session, json);
                break;
            case "move":
                handleMove(sessionId, json);
                break;
            case "attack":
                handleAttack(sessionId, json);
                break;
            case "getState":
                handleGetState(sessionId);
                break;
            default:
                sendError(sessionId, "Unknown command type: " + type);
        }
    }

    private void handleJoin(String sessionId, WebSocketSession session, ObjectNode json) {
        int port = json.get("port").asInt();
        String playerName = json.get("playerName").asText();

        GameService gameService = matchmakingService.getGameService(port);
        if (gameService == null) {
            sendError(sessionId, "Game not found");
            return;
        }

        GameState gameState = gameService.getGameState();
        Map<Integer, Player> allPlayers = gameState.getAllPlayers();

        Map.Entry<Integer, Player> existingPlayerEntry = allPlayers.entrySet().stream()
            .filter(entry -> entry.getValue().getName().equals(playerName))
            .findFirst()
            .orElse(null);

        Integer playerId;
        if (existingPlayerEntry != null) {
            playerId = existingPlayerEntry.getKey();
        } else {
            Player player = gameService.addPlayer(playerName);
            playerId = gameService.getGameState().getAllPlayers().entrySet().stream()
                .filter(entry -> entry.getValue() == player)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
        }

        sessionToPort.put(sessionId, port);
        sessionToPlayerName.put(sessionId, playerName);

        sendMessage(sessionId, "joined", objectMapper.createObjectNode()
            .put("port", port)
            .put("gameName", gameState.getGameName())
            .put("playerName", playerName)
            .put("playerId", playerId));

        broadcastGameState(port);
    }

    private void handleMove(String sessionId, ObjectNode json) {
        Integer port = sessionToPort.get(sessionId);
        if (port == null) {
            sendError(sessionId, "Not joined to any game");
            return;
        }

        String direction = json.get("direction").asText();
        GameService gameService = matchmakingService.getGameService(port);
        if (gameService == null) return;

        String playerName = sessionToPlayerName.get(sessionId);
        GameState gameState = gameService.getGameState();
        Map<Integer, Player> players = gameState.getAllPlayers();

        Player player = null;
        for (Player p : players.values()) {
            if (p.getName().equals(playerName)) {
                player = p;
                break;
            }
        }

        if (player == null) return;

        int newX = player.getX();
        int newY = player.getY();

        switch (direction) {
            case "w": newY--; break;
            case "s": newY++; break;
            case "a": newX--; break;
            case "d": newX++; break;
        }

        if (gameState.getWorld().isWalkable(newX, newY) && 
            !gameState.getWorld().hasEnemyAt(newX, newY)) {
            player.setX(newX);
            player.setY(newY);
        }

        broadcastGameState(port);
    }

    private void handleAttack(String sessionId, ObjectNode json) {
        Integer port = sessionToPort.get(sessionId);
        if (port == null) {
            sendError(sessionId, "Not joined to any game");
            return;
        }

        GameService gameService = matchmakingService.getGameService(port);
        if (gameService == null) return;

        String playerName = sessionToPlayerName.get(sessionId);
        GameState gameState = gameService.getGameState();
        Map<Integer, Player> players = gameState.getAllPlayers();

        Player player = null;
        for (Player p : players.values()) {
            if (p.getName().equals(playerName)) {
                player = p;
                break;
            }
        }

        if (player == null) return;

        int px = player.getX();
        int py = player.getY();

        Integer targetX = json.has("targetX") ? json.get("targetX").asInt() : null;
        Integer targetY = json.has("targetY") ? json.get("targetY").asInt() : null;
        Enemy enemy = findAdjacentEnemy(gameState, px, py, targetX, targetY);

        if (enemy != null) {
            int playerDamage = player.dealDamage();
            enemy.takeDamage(playerDamage);

            ObjectNode combatResult = objectMapper.createObjectNode();
            combatResult.put("attacker", player.getName());
            combatResult.put("enemy", enemy.getName());
            combatResult.put("damageToEnemy", playerDamage);
            combatResult.put("enemyHp", enemy.getHealth());
            combatResult.put("enemyMaxHp", enemy.getMaxHealth());

            if (!enemy.isAlive()) {
                gameState.getWorld().removeEnemy(enemy);
                combatResult.put("result", "enemyDefeated");
                sendMessage(sessionId, "combatResult", combatResult);
            } else {
                int enemyDamage = Math.max(1, enemy.dealDamage() - player.getDefense());
                player.takeDamage(enemyDamage);

                combatResult.put("damageToPlayer", enemyDamage);
                combatResult.put("playerHp", player.getHealth());
                combatResult.put("playerMaxHp", player.getMaxHealth());
                combatResult.put("result", player.isAlive() ? "exchange" : "playerDefeated");
                sendMessage(sessionId, "combatResult", combatResult);
            }
        } else {
            sendError(sessionId, "No hay enemigo adyacente para atacar");
        }

        broadcastGameState(port);
    }

    private Enemy findAdjacentEnemy(GameState gameState, int px, int py, Integer targetX, Integer targetY) {
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        if (targetX != null && targetY != null) {
            int distance = Math.abs(targetX - px) + Math.abs(targetY - py);
            if (distance == 1) {
                return gameState.getWorld().getEnemyAt(targetX, targetY);
            }
            return null;
        }

        for (int[] dir : directions) {
            Enemy candidate = gameState.getWorld().getEnemyAt(px + dir[0], py + dir[1]);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private void handleGetState(String sessionId) {
        Integer port = sessionToPort.get(sessionId);
        if (port == null) {
            sendError(sessionId, "Not joined to any game");
            return;
        }

        broadcastGameState(port);
    }

    private void broadcastGameState(int port) {
        GameService gameService = matchmakingService.getGameService(port);
        if (gameService == null) return;

        GameState gameState = gameService.getGameState();
        String stateJson = buildGameStateJson(gameState, port);

        for (Map.Entry<String, Integer> entry : sessionToPort.entrySet()) {
            if (entry.getValue() == port) {
                String sessionId = entry.getKey();
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(stateJson));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String buildGameStateJson(GameState gameState, int port) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "gameState");
        root.put("port", port);
        root.put("gameName", gameState.getGameName());
        root.put("hostName", gameState.getHostName());
        root.put("maxPlayers", gameState.getMaxPlayers());
        root.put("currentPlayers", gameState.getCurrentPlayerCount());

        ObjectNode worldNode = root.putObject("world");
        worldNode.put("width", gameState.getWorld().width);
        worldNode.put("height", gameState.getWorld().height);

        ObjectNode mapNode = worldNode.putObject("map");
        char[][] map = gameState.getWorld().map;
        for (int y = 0; y < map.length; y++) {
            String row = new String(map[y]);
            mapNode.put("row" + y, row);
        }

        ObjectNode playersNode = root.putObject("players");
        Map<Integer, Player> players = gameState.getAllPlayers();
        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
            Player p = entry.getValue();
            ObjectNode playerNode = playersNode.putObject(String.valueOf(entry.getKey()));
            playerNode.put("name", p.getName());
            playerNode.put("x", p.getX());
            playerNode.put("y", p.getY());
            playerNode.put("hp", p.getHealth());
            playerNode.put("maxHp", p.getMaxHealth());
        }

        ObjectNode enemiesNode = root.putObject("enemies");
        int enemyIndex = 0;
        for (Enemy e : gameState.getWorld().getEnemies()) {
            ObjectNode enemyNode = enemiesNode.putObject(e.getName() + "_" + enemyIndex++);
            enemyNode.put("x", e.getX());
            enemyNode.put("y", e.getY());
            enemyNode.put("hp", e.getHealth());
            enemyNode.put("maxHp", e.getMaxHealth());
        }

        return root.toString();
    }

    private void sendMessage(String sessionId, String type, ObjectNode data) {
        data.put("type", type);
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(data.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendError(String sessionId, String error) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("type", "error");
        errorNode.put("message", error);
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(errorNode.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}