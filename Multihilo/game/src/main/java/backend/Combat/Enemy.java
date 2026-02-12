package backend.Combat;

import backend.Entity;

public class Enemy extends Entity {

    private int attack;

    public Enemy(String name, int x, int y, int maxHealth, int attack) {
        super(name, x, y, maxHealth);
        this.attack = attack;
    }

    public int dealDamage() {
        return attack + (int)(Math.random() * 5);
    }
}
