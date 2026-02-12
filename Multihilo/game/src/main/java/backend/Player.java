package backend;

import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {

    private int attack;
    private int defense;
    private List<String> inventory;

    public Player(String name, int x, int y) {
        super(name, x, y, 100);
        this.attack = 15;
        this.defense = 5;
        this.inventory = new ArrayList<>();
    }

    public int dealDamage() {
        return attack + (int)(Math.random() * 5);
    }

    public int getDefense() {
        return defense;
    }

    public List<String> getInventory() {
        return inventory;
    }
}
