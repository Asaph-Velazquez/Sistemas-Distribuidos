package backend.combat;

import backend.entities.Entity;

/**
 * Enemy - Representa un enemigo en el juego
 * 
 * Extiende Entity para crear enemigos que los jugadores pueden encontrar y combatir.
 * Cada enemigo tiene stats configurables (salud, ataque) que determinan su dificultad.
 */
public class Enemy extends Entity {

    private int attack;

    /**
     * Constructor de Enemy
     * @param name Nombre del enemigo (Goblin, Orc, Troll, etc.)
     * @param x Posición X inicial
     * @param y Posición Y inicial
     * @param maxHealth Salud máxima del enemigo
     * @param attack Valor de ataque base del enemigo
     */
    public Enemy(String name, int x, int y, int maxHealth, int attack) {
        super(name, x, y, maxHealth);
        this.attack = attack;
    }

    /**
     * Calcula el daño que el enemigo inflige en un ataque
     * Incluye variación aleatoria para hacer el combate más dinámico
     * @return Daño a infligir (ataque base + 0-4 aleatorio)
     */
    public int dealDamage() {
        return attack + (int)(Math.random() * 5);
    }
}
