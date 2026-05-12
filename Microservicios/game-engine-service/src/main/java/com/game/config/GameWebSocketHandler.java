package com.game.config;

import com.game.core.GameState;
import com.game.core.World;
import com.game.entities.Player;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, GameState> games = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToGame = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String[] parts = payload.split(":");
        String command = parts[0];

        switch (command) {
            case "join":
                handleJoin(session, parts);
                break;
            case "move":
                handleMove(session, parts);
                break;
            case "attack":
                handleAttack(session, parts);
                break;
            case "getState":
                handleGetState(session);
                break;
        }
    }

    private void handleJoin(WebSocketSession session, String[] parts) throws IOException {
        Long gameId = Long.parseLong(parts[1]);
        String playerName = parts[2];
        Long userId = Long.parseLong(parts[3]);

        GameState game = games.computeIfAbsent(gameId, id -> {
            World world = new World(20, 10);
            return new GameState(id, world);
        });

        int[] pos = game.getWorld().getRandomWalkablePosition();
        Player player = new Player(playerName, pos[0], pos[1], userId);
        game.addPlayer(session.getId(), player);
        sessionToGame.put(session.getId(), gameId);

        session.sendMessage(new TextMessage("joined:" + player.getX() + "," + player.getY()));
    }

    private void handleMove(WebSocketSession session, String[] parts) throws IOException {
        Long gameId = sessionToGame.get(session.getId());
        if (gameId == null) return;

        GameState game = games.get(gameId);
        if (game == null) return;

        Player player = game.getPlayer(session.getId());
        if (player == null) return;

        int dx = Integer.parseInt(parts[1]);
        int dy = Integer.parseInt(parts[2]);
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (game.getWorld().isWalkable(newX, newY)) {
            player.setX(newX);
            player.setY(newY);
            session.sendMessage(new TextMessage("moved:" + newX + "," + newY));
        }
    }

    private void handleAttack(WebSocketSession session, String[] parts) throws IOException {
        Long gameId = sessionToGame.get(session.getId());
        if (gameId == null) return;

        GameState game = games.get(gameId);
        if (game == null) return;

        Player player = game.getPlayer(session.getId());
        if (player == null) return;

        var enemy = game.getWorld().getEnemyAt(player.getX(), player.getY());
        if (enemy != null) {
            int damage = player.dealDamage();
            enemy.takeDamage(damage);
            if (!enemy.isAlive()) {
                game.getWorld().removeEnemy(enemy);
                session.sendMessage(new TextMessage("enemy_killed:" + enemy.getName()));
            } else {
                player.takeDamage(enemy.dealDamage());
                session.sendMessage(new TextMessage("attacked:" + damage + "," + player.getHealth()));
            }
        }
    }

    private void handleGetState(WebSocketSession session) throws IOException {
        Long gameId = sessionToGame.get(session.getId());
        if (gameId == null) return;

        GameState game = games.get(gameId);
        if (game == null) return;

        StringBuilder sb = new StringBuilder("state:");
        sb.append("players:").append(game.getCurrentPlayerCount()).append(";");
        sb.append("enemies:").append(game.getWorld().getEnemies().size());
        session.sendMessage(new TextMessage(sb.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long gameId = sessionToGame.remove(session.getId());
        if (gameId != null) {
            GameState game = games.get(gameId);
            if (game != null) {
                game.removePlayer(session.getId());
            }
        }
    }
}