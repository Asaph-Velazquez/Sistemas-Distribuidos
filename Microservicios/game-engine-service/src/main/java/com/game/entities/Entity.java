package com.game.entities;

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

    public synchronized int getX() { return x; }
    public synchronized int getY() { return y; }
    public synchronized int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public String getName() { return name; }

    public synchronized void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    public synchronized boolean isAlive() { return health > 0; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}