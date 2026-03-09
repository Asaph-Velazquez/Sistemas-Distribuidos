# Juego Distribuido - Múltiples Clientes - Múltiples Servidores

## 📋 Descripción

Proyecto de juego en consola desarrollado en Java que implementa una **arquitectura de múltiples clientes y múltiples servidores** utilizando **sockets TCP**. El sistema cuenta con un servidor de matchmaking que gestiona el descubrimiento de partidas y servidores de juego independientes para cada partida, demostrando conceptos avanzados de sistemas distribuidos, sincronización y programación con sockets.

## 🎮 Características

- **Sistema de Matchmaking**: Servidor central que permite crear, listar y unirse a partidas
- **Múltiples Servidores de Juego**: Cada partida se ejecuta en su propio servidor
- **Puerto Dinámico**: Los servidores de juego se crean en puertos automáticos (50001-50100)
- **Inicio Automático**: Los servidores de juego se inician automáticamente al crear una partida
- **Mundo Compartido**: Todos los jugadores en una partida ven el mismo mundo en tiempo real
- **Identificación por ID**: Cada jugador se diferencia por un número (1, 2, 3...)

- **Sistema Multihilo**: Hilos concurrentes trabajando en paralelo
  - **InputThread**: Captura y procesa la entrada del usuario
  - **GameLoop**: Gestiona la lógica principal del juego
  - **RenderThread**: Renderiza el estado del juego en consola
  - **WorldClock**: Controla el tiempo del mundo del juego
  - **ReceiverThread**: Recibe actualizaciones del servidor

- **Sistema de Combate**:
  - Combate por turnos individual por jugador
  - Sistema de ataque y defensa
  - Enemigos con diferentes tipos

- **Técnicas de Sincronización**:
  - **ConcurrentHashMap**: Almacenamiento thread-safe de partidas
  - **AtomicInteger**: Generación de IDs únicos
  - **volatile**: Visibilidad de flags entre hilos
  - **synchronized**: Métodos con exclusión mutua

## 🏗️ Arquitectura del Sistema

```
        ┌──────────────────────────────────────────────────────┐
        │              MatchmakingServer (Puerto 12345)      │
        │  ┌─────────────────────────────────────────────────┐ │
        │  │  • Registro de partidas                         │ │
        │  │  • Listado de juegos disponibles                │ │
        │  │  • Asignación de puertos (50001-50100)         │ │
        │  │  • Inicio automático de servidores de juego    │ │
        │  └─────────────────────────────────────────────────┘ │
        └──────────────────────────┬───────────────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
         ▼                         ▼                         ▼
  ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
  │ServidorJuego│          │ServidorJuego│          │ServidorJuego│
  │ (Puerto 50001)         │ (Puerto 50002)         │ (Puerto 50003)
  └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
         │                         │                         │
    ┌────┴────┐               ┌────┴────┐               ┌────┴────┐
    │Cliente 1│               │Cliente 3│               │Cliente 5│
    └─────────┘               └─────────┘               └─────────┘
```

### Componentes del Sistema

| Componente | Descripción |
|------------|-------------|
| **MatchmakingServer** | Servidor central en puerto 12345 que gestiona el registro y descubrimiento de partidas |
| **MatchmakingClient** | Cliente que permite conectar al servidor de matchmaking para crear/listar/unirse a partidas |
| **Servidor de Juego** | Cada partida se ejecuta en un servidor independiente (puertos 50001-50100) |
| **Cliente de Juego** | Se conecta al servidor de juego específico para participar en una partida |

## 🖥️ Componentes del Servidor

### MatchmakingServer.java
- **Puerto**: 12345
- **Protocolo**: TCP/IP mediante `ServerSocket`
- **Funciones**:
  - Registro de nuevas partidas
  - Listado de partidas disponibles
  - Asignación dinámica de puertos (50001-50100)
  - Inicio automático de servidores de juego
  - Actualización del número de jugadores
  - Limpieza automática de partidas inactivas

**Comandos del protocolo:**
```
CREATE:{username}:{gameName}:{maxPlayers}  → Crea una nueva partida
LIST                                        → Lista todas las partidas
JOIN:{port}                                 → Se une a una partida
UPDATE:{port}:{currentPlayers}             → Actualiza el conteo de jugadores
DELETE:{port}                              → Elimina una partida
```

### Server.java (Servidor de Juego)
- **Puerto**: Dinámico (50001-50100)
- **Protocolo**: TCP/IP mediante `ServerSocket`
- **Funciones**:
  - Acepta múltiples clientes simultáneamente
  - Asigna un ID único a cada jugador
  - Gestiona el estado global del juego (GameState)
  - Procesa comandos de cada cliente independientemente
  - Permite combate individual por jugador
  - Envía actualizaciones a todos los clientes (~10 fps)

## 📦 Estructura del Proyecto

```
game/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── backend/
│   │           ├── core/
│   │           │   ├── GameState.java        # Estado global thread-safe
│   │           │   └── GameLoop.java        # Loop principal
│   │
│   │           ├── threads/
│   │           │   ├── InputThread.java     # Entrada de usuario
│   │           │   ├── RenderThread.java    # Renderizado
│   │           │   ├── ReceiverThread.java  # Receptor de datos
│   │           │   └── WorldClock.java     # Reloj del mundo
│   │
│   │           ├── entities/
│   │           │   ├── Entity.java         # Clase base
│   │           │   └── Player.java        # Jugador
│   │
│   │           ├── world/
│   │           │   └── World.java         # Mundo 2D
│   │
│   │           ├── combat/
│   │           │   ├── CombatSystem.java  # Sistema de combate
│   │           │   └── Enemy.java        # Enemigos
│   │
│   │           ├── commands/
│   │           │   └── CommandProcessor.java
│   │
│   │           ├── Sockets/
│   │           │   ├── MatchmakingServer.java  # Servidor central de matchmaking
│   │           │   ├── MatchmakingClient.java  # Cliente de matchmaking
│   │           │   ├── Server.java             # Servidor de juego
│   │           │   └── Client.java            # Cliente del juego
│   │
│   │           └── game/
│   │               └── GameMode.java        # Modos de juego
│   │
│   └── test/
│       └── java/
│           └── backend/
│               └── AppTest.java
├── pom.xml
└── README.md
```

## 🚀 Instalación y Ejecución

### Requisitos Previos

- Java 17 o superior
- Maven 3.6 o superior

### Compilar el Proyecto

```bash
mvn clean compile
```

### Ejecución del Sistema

#### Paso 1: Iniciar el MatchmakingServer

```bash
mvn exec:java "-Dexec.mainClass=backend.Sockets.MatchmakingServer"
```

El servidor de matchmaking escuchará en el puerto 12345.

#### Paso 2: Iniciar el Cliente

```bash
mvn exec:java "-Dexec.mainClass=backend.Sockets.Client"
```

El cliente mostrará un menú para:
1. Crear nueva partida
2. Listar partidas disponibles
3. Unirse a una partida
4. Salir

### Ejecutar Tests

```bash
mvn test
```

### Crear JAR

```bash
mvn package
```

## 🎯 Cómo Jugar

1. Inicia el MatchmakingServer:
   ```bash
   mvn exec:java "-Dexec.mainClass=backend.Sockets.MatchmakingServer"
   ```

2. Inicia el Cliente:
   ```bash
   mvn exec:java "-Dexec.mainClass=backend.Sockets.Client"
   ```

3. En el menú del cliente, selecciona una opción:
   - **Crear partida**: Ingresa tu nombre, nombre de la partida y máximo de jugadores
   - **Listar partidas**: Ve las partidas disponibles con su información
   - **Unirse a partida**: Ingresa el puerto de la partida a la que quieres unirte

4. Usa comandos de texto para interactuar:
   - `w`, `a`, `s`, `d` - Moverse
   - `attack` / `a` - Atacar (en combate)
   - `run` / `r` - Huir (en combate)
   - `quit` - Salir

5. ¡Invita a más amigos! Pueden conectarse al mismo servidor de juego.

## 🔄 Flujo de Ejecución

1. El MatchmakingServer se inicia en el puerto 12345
2. El Cliente 1 se conecta y crea una partida
3. El MatchmakingServer asigna el puerto 50001 y registra la partida
4. El MatchmakingServer inicia automáticamente un servidor de juego en el puerto 50001
5. El Cliente 1 recibe la información del puerto y se conecta al servidor de juego
6. El Cliente 1 recibe su ID y el estado inicial del juego
7. El Cliente 2 se conecta, lista las partidas y se une a la del Cliente 1
8. Ambos clientes ven el mismo mundo y jugadores en tiempo real
9. El MatchmakingServer actualiza periódicamente el número de jugadores

## 🧵 Técnicas de Sincronización

### ConcurrentHashMap
Almacenamiento thread-safe de partidas activas en el servidor de matchmaking:
```java
private static final ConcurrentHashMap<Integer, GameInfo> activeGames = 
    new ConcurrentHashMap<>();
```

### AtomicInteger
Generación de IDs únicos para jugadores y asignación de puertos:
```java
private static final AtomicInteger nextPlayerId = new AtomicInteger(1);
private static final AtomicInteger nextGamePort = new AtomicInteger(MIN_GAME_PORT);
```

### volatile
Visibilidad inmediata de flags entre hilos:
```java
private volatile boolean running = true;
private volatile GameMode.Mode currentGameMode = GameMode.Mode.Exploration;
```

### synchronized
Exclusión mutua para métodos críticos:
```java
public synchronized void addPlayer(int id, Player player) {
    players.put(id, player);
}
```

### ScheduledExecutorService
Tareas periódicas thread-safe:
```java
ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
cleaner.scheduleAtFixedRate(() -> {...}, 30, 30, TimeUnit.MINUTES);
```

## 📊 Formato de Comunicación JSON

### Del cliente al servidor de juego (comando):
```json
{"playerId": 1, "command": "w"}
{"playerId": 2, "command": "attack"}
```

### Del servidor de juego al cliente (estado):
```json
{
  "playerId": 1,
  "gameName": "MiPartida",
  "hostName": "Player1",
  "maxPlayers": 4,
  "currentPlayers": 2,
  "players": [
    {"id": 1, "name": "Player1", "x": 5, "y": 3, "hp": 100, "maxHp": 100},
    {"id": 2, "name": "Player2", "x": 8, "y": 6, "hp": 100, "maxHp": 100}
  ],
  "world": {
    "width": 20,
    "height": 10,
    "map": [...],
    "enemies": [...]
  },
  "combats": [...]
}
```

## 📋 Diferencias con Práctica Anterior

| Característica | Práctica Anterior | Práctica Actual |
|----------------|-------------------|-----------------|
| Estructura | 1 servidor, múltiples clientes | Sistema de matchmaking + múltiples servidores de juego |
| Puertos | Puerto fijo (12345) | Matchmaking: 12345, Juegos: 50001-50100 |
| Partidas | Una sola partida | Múltiples partidas simultáneas |
| Descubrimiento | No disponible | Servidor de matchmaking |
| Inicio de servidores | Manual | Automático al crear partida |

## ✍️ Autor

Velazquez Parral Saul Asaph

## 📚 Repositorio

https://github.com/Asaph-Velazquez/Sistemas-Distribuidos.git
