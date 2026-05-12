package com.game.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.core.GameState;
import com.game.core.World;
import com.game.entities.Enemy;
import com.game.entities.Player;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, GameState> games = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToGame = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText("");

            switch (type) {
                case "join" -> handleJoin(session, payload);
                case "move" -> handleMove(session, payload);
                case "attack" -> handleAttack(session, payload);
                case "getState" -> sendGameState(session);
                default -> sendError(session, "Tipo de mensaje no soportado: " + type);
            }
        } catch (Exception e) {
            sendError(session, "Mensaje WebSocket invalido: " + e.getMessage());
        }
    }

    private void handleJoin(WebSocketSession session, JsonNode payload) throws IOException {
        long gameId = firstLong(payload, "gameId", "id", "port");
        String playerName = payload.path("playerName").asText("Jugador");
        long userId = payload.path("userId").asLong(0L);

        GameState game = games.computeIfAbsent(gameId, id -> new GameState(id, new World(20, 10)));

        int[] pos = game.getWorld().getRandomWalkablePosition();
        Player player = new Player(playerName, pos[0], pos[1], userId);
        game.addPlayer(session.getId(), player);
        sessionToGame.put(session.getId(), gameId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "joined");
        response.put("gameId", gameId);
        response.put("port", gameId);
        response.put("gameName", "Partida " + gameId);
        response.put("x", player.getX());
        response.put("y", player.getY());
        sendJson(session, response);
        sendGameState(session);
    }

    private void handleMove(WebSocketSession session, JsonNode payload) throws IOException {
        GameState game = getSessionGame(session);
        if (game == null) return;

        Player player = game.getPlayer(session.getId());
        if (player == null) return;

        int[] delta = getMoveDelta(payload);
        int newX = player.getX() + delta[0];
        int newY = player.getY() + delta[1];

        if (!game.getWorld().isWalkable(newX, newY) || game.getWorld().getEnemyAt(newX, newY) != null) {
            sendError(session, "No se puede mover a esa posicion");
            return;
        }

        player.setX(newX);
        player.setY(newY);
        broadcastGameState(game.getGameId());
    }

    private void handleAttack(WebSocketSession session, JsonNode payload) throws IOException {
        GameState game = getSessionGame(session);
        if (game == null) return;

        Player player = game.getPlayer(session.getId());
        if (player == null) return;

        Enemy enemy = null;
        if (payload.has("targetX") && payload.has("targetY")) {
            enemy = game.getWorld().getEnemyAt(payload.get("targetX").asInt(), payload.get("targetY").asInt());
        }
        if (enemy == null) {
            enemy = findAdjacentEnemy(game, player);
        }
        if (enemy == null) {
            sendError(session, "No hay enemigos cercanos");
            return;
        }

        int damageToEnemy = player.dealDamage();
        enemy.takeDamage(damageToEnemy);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "combatResult");
        response.put("damageToEnemy", damageToEnemy);
        response.put("enemy", enemy.getName());

        if (!enemy.isAlive()) {
            game.getWorld().removeEnemy(enemy);
            response.put("result", "enemyDefeated");
        } else {
            int damageToPlayer = enemy.dealDamage();
            player.takeDamage(damageToPlayer);
            response.put("damageToPlayer", damageToPlayer);
            response.put("playerHealth", player.getHealth());
            response.put("result", player.isAlive() ? "exchange" : "playerDefeated");
        }

        sendJson(session, response);
        broadcastGameState(game.getGameId());
    }

    private void sendGameState(WebSocketSession session) throws IOException {
        GameState game = getSessionGame(session);
        if (game != null) {
            sendJson(session, toGameStatePayload(game));
        }
    }

    private void broadcastGameState(long gameId) throws IOException {
        Map<String, Object> payload = toGameStatePayload(games.get(gameId));
        for (Map.Entry<String, Long> entry : sessionToGame.entrySet()) {
            if (!entry.getValue().equals(gameId)) continue;
            WebSocketSession session = findOpenSession(entry.getKey());
            if (session != null) {
                sendJson(session, payload);
            }
        }
    }

    private WebSocketSession findOpenSession(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);
        return session != null && session.isOpen() ? session : null;
    }

    private Map<String, Object> toGameStatePayload(GameState game) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "gameState");
        payload.put("gameId", game.getGameId());
        payload.put("port", game.getGameId());
        payload.put("status", game.getStatus());

        Map<String, Object> worldPayload = new LinkedHashMap<>();
        worldPayload.put("width", game.getWorld().width);
        worldPayload.put("height", game.getWorld().height);
        Map<String, String> rows = new LinkedHashMap<>();
        for (int y = 0; y < game.getWorld().height; y++) {
            rows.put("row" + y, new String(game.getWorld().map[y]));
        }
        worldPayload.put("map", rows);
        payload.put("world", worldPayload);

        Map<String, Object> players = new LinkedHashMap<>();
        for (Map.Entry<String, Player> entry : game.getAllPlayers().entrySet()) {
            Player player = entry.getValue();
            Map<String, Object> playerPayload = new LinkedHashMap<>();
            playerPayload.put("name", player.getName());
            playerPayload.put("x", player.getX());
            playerPayload.put("y", player.getY());
            playerPayload.put("hp", player.getHealth());
            playerPayload.put("maxHp", player.getMaxHealth());
            playerPayload.put("userId", player.getUserId());
            players.put(entry.getKey(), playerPayload);
        }
        payload.put("players", players);

        Map<String, Object> enemies = new LinkedHashMap<>();
        int index = 0;
        for (Enemy enemy : game.getWorld().getEnemies()) {
            Map<String, Object> enemyPayload = new LinkedHashMap<>();
            enemyPayload.put("name", enemy.getName());
            enemyPayload.put("x", enemy.getX());
            enemyPayload.put("y", enemy.getY());
            enemyPayload.put("hp", enemy.getHealth());
            enemyPayload.put("maxHp", enemy.getMaxHealth());
            enemies.put(enemy.getName() + "_" + index++, enemyPayload);
        }
        payload.put("enemies", enemies);
        return payload;
    }

    private void sendJson(WebSocketSession session, Map<String, Object> payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "error");
        payload.put("message", message);
        sendJson(session, payload);
    }

    private GameState getSessionGame(WebSocketSession session) {
        Long gameId = sessionToGame.get(session.getId());
        return gameId == null ? null : games.get(gameId);
    }

    private Enemy findAdjacentEnemy(GameState game, Player player) {
        for (Enemy enemy : game.getWorld().getEnemies()) {
            int distance = Math.abs(enemy.getX() - player.getX()) + Math.abs(enemy.getY() - player.getY());
            if (distance == 1) return enemy;
        }
        return null;
    }

    private int[] getMoveDelta(JsonNode payload) {
        if (payload.has("direction")) {
            return switch (payload.get("direction").asText()) {
                case "w" -> new int[]{0, -1};
                case "s" -> new int[]{0, 1};
                case "a" -> new int[]{-1, 0};
                case "d" -> new int[]{1, 0};
                default -> new int[]{0, 0};
            };
        }
        return new int[]{payload.path("dx").asInt(0), payload.path("dy").asInt(0)};
    }

    private long firstLong(JsonNode payload, String... fields) {
        for (String field : fields) {
            if (payload.has(field)) return payload.get(field).asLong();
        }
        return 1L;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        Long gameId = sessionToGame.remove(session.getId());
        if (gameId != null) {
            GameState game = games.get(gameId);
            if (game != null) {
                game.removePlayer(session.getId());
            }
        }
    }
}
