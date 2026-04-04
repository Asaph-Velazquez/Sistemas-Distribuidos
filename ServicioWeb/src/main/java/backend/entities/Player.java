package backend.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Player - Representa un jugador en el juego
 * 
 * Extiende Entity agregando capacidades de combate (ataque, defensa) e inventario.
 * Cada jugador es controlado por un cliente conectado al servidor.
 */
public class Player extends Entity {

    private int attack;
    private int defense;

    /**
     * Constructor de Player
     * Inicializa un jugador con stats por defecto (100 HP, 15 ataque, 5 defensa)
     * @param name Nombre del jugador
     * @param x Posición X inicial
     * @param y Posición Y inicial
     */
    public Player(String name, int x, int y) {
        super(name, x, y, 100);
        this.attack = 15;
        this.defense = 5;
    }

    /**
     * Calcula el daño que el jugador inflige en un ataque
     * Incluye variación aleatoria para hacer el combate más dinámico
     * @return Daño a infligir (ataque base + 0-4 aleatorio)
     */
    public int dealDamage() {
        return attack + (int)(Math.random() * 5);
    }

    /**
     * Obtiene el valor de defensa del jugador
     * @return Defensa actual
     */
    public int getDefense() {
        return defense;
    }
}
