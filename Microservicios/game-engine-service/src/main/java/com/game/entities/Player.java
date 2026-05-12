package com.game.entities;

public class Player extends Entity {
    private int attack;
    private int defense;
    private Long userId;

    public Player(String name, int x, int y, Long userId) {
        super(name, x, y, 100);
        this.attack = 15;
        this.defense = 5;
        this.userId = userId;
    }

    public int dealDamage() {
        return attack + (int)(Math.random() * 5);
    }

    public int getDefense() { return defense; }
    public Long getUserId() { return userId; }
}