package backend.entities;

public abstract class Entity {

    protected String name;

    protected int x;
    protected int y;

    protected int health;
    protected int maxHealth;

    public Entity(String name, int x, int y, int maxHealth) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }
    public synchronized void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public synchronized int getX() {
        return x;
    }

    public synchronized int getY() {
        return y;
    }

    public synchronized void takeDamage(int damage) {
        health -= damage;
        if (health < 0) {
            health = 0;
        }
    }

    public synchronized void heal(int amount) {
        health += amount;
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    public synchronized boolean isAlive() {
        return health > 0;
    }

    public synchronized int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public String getName() {
        return name;
    }
}
