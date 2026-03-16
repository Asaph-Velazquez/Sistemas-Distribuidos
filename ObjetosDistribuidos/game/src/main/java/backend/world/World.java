package backend.world;

import backend.combat.Enemy;
import backend.entities.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class World {
    public char[][] map;    
    public int width;
    public int height;
    private List<Enemy> enemies;
    private Random random;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.map = new char[height][width];
        this.enemies = new ArrayList<>();
        this.random = new Random();
        generateMap();
        generateEnemies();
    }

    private void generateMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                    map[y][x] = '#';
                } else {
                    map[y][x] = '.';
                }
            }
        }

        map[3][3] = '#';
        map[4][5] = '#';
        map[6][2] = '#';
    }
    
    private void generateEnemies() {
        int numEnemies = 3 + random.nextInt(3);
        
        for (int i = 0; i < numEnemies; i++) {
            int x, y;
            do {
                x = 1 + random.nextInt(width - 2);
                y = 1 + random.nextInt(height - 2);
            } while (!isWalkable(x, y) || hasEnemyAt(x, y));
            
            String[] names = {"Goblin", "Orc", "Troll", "Skeleton", "Zombie"};
            String name = names[random.nextInt(names.length)];
            int health = 20 + random.nextInt(20);
            int attack = 5 + random.nextInt(8);
            
            enemies.add(new Enemy(name, x, y, health, attack));
        }
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false; 
        }
        return map[y][x] == '.'; 
    }
    
    public synchronized boolean hasEnemyAt(int x, int y) {
        return getEnemyAt(x, y) != null;
    }
    
    public synchronized Enemy getEnemyAt(int x, int y) {
        for (Enemy enemy : enemies) {
            if (enemy.getX() == x && enemy.getY() == y) {
                return enemy;
            }
        }
        return null;
    }
    
    public synchronized void removeEnemy(Enemy enemy) {
        enemies.remove(enemy);
    }
    
    public synchronized List<Enemy> getEnemies() {
        return new ArrayList<>(enemies);
    }

    public synchronized int[] getRandomWalkablePosition() {
        int x, y;
        int attempts = 0;
        do {
            x = 1 + random.nextInt(width - 2);
            y = 1 + random.nextInt(height - 2);
            attempts++;
        } while (!isWalkable(x, y) && attempts < 100);
        return new int[]{x, y};
    }

    public void render(Map<Integer, Player> players) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Player playerAtPos = null;
                int playerIdAtPos = -1;
                
                for (Map.Entry<Integer, Player> entry : players.entrySet()) {
                    if (entry.getValue().getX() == x && entry.getValue().getY() == y) {
                        playerAtPos = entry.getValue();
                        playerIdAtPos = entry.getKey();
                        break;
                    }
                }
                
                if (playerAtPos != null) {
                    System.out.print(playerIdAtPos);
                } else if (hasEnemyAt(x, y)) {
                    System.out.print("E");
                } else {
                    System.out.print(map[y][x]);
                }
            }
            System.out.println();
        }
    }
}
