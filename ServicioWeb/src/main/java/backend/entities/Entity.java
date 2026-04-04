package backend.entities;

/**
 * Entity - Clase base abstracta para todas las entidades del juego
 * 
 * Representa cualquier entidad que existe en el mundo del juego (jugadores, enemigos, etc.)
 * Proporciona funcionalidad básica de posición, salud y movimiento.
 * 
 * Sincronización:
 *   - Métodos synchronized para garantizar thread-safety en acceso concurrente
 *   - Importante para servidor multi-cliente donde múltiples hilos acceden a las entidades
 */
public abstract class Entity {

    protected String name;

    protected int x;
    protected int y;

    protected int health;
    protected int maxHealth;

    /**
     * Constructor de Entity
     * @param name Nombre de la entidad
     * @param x Posición X inicial
     * @param y Posición Y inicial
     * @param maxHealth Salud máxima de la entidad
     */
    public Entity(String name, int x, int y, int maxHealth) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }
    
    /**
     * Obtiene la posición X actual
     * Synchronized: Garantiza lectura consistente de la posición
     * @return Coordenada X
     */
    public synchronized int getX() {
        return x;
    }

    /**
     * Obtiene la posición Y actual
     * Synchronized: Garantiza lectura consistente de la posición
     * @return Coordenada Y
     */
    public synchronized int getY() {
        return y;
    }

    /**
     * Reduce la salud de la entidad por daño recibido
     * Synchronized: Evita condiciones de carrera al modificar salud
     * @param damage Cantidad de daño a recibir
     */
    public synchronized void takeDamage(int damage) {
        health -= damage;
        if (health < 0) {
            health = 0;
        }
    }

    /**
     * Verifica si la entidad sigue viva
     * Synchronized: Garantiza lectura consistente del estado
     * @return true si la entidad tiene salud > 0
     */
    public synchronized boolean isAlive() {
        return health > 0;
    }

    /**
     * Obtiene la salud actual
     * Synchronized: Garantiza lectura consistente
     * @return Salud actual
     */
    public synchronized int getHealth() {
        return health;
    }

    /**
     * Obtiene la salud máxima
     * @return Salud máxima
     */
    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * Obtiene el nombre de la entidad
     * @return Nombre
     */
    public String getName() {
        return name;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
