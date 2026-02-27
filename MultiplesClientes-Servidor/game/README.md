# Juego Distribuido - Sistemas Cliente-Servidor con Sockets

## 📋 Descripción

Proyecto de juego en consola desarrollado en Java que implementa una **arquitectura cliente-servidor** utilizando **sockets TCP** para la comunicación en red. El servidor gestiona el estado del juego y múltiples clientes se conectan para interactuar con él en un **mundo compartido**, demostrando conceptos de sistemas distribuidos y programación con sockets.

## 🎮 Características

- **Modo Multijugador**: Múltiples clientes pueden conectarse simultáneamente al servidor
- **Mundo Compartido**: Todos los jugadores ven el mismo mundo en tiempo real
- **Identificación por ID**: Cada jugador se diferencia por un número (1, 2, 3...)
- **Combate Individual**: Cada jugador puede estar en combate independientemente

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

- **Mundo del Juego**:
  - Mundo 2D con sistema de coordenadas
  - Generación aleatoria de enemigos
  - Posiciones aleatorias para nuevos jugadores

## 🖥️ Arquitectura Cliente-Servidor con Sockets

### Servidor (Server.java)
- **Puerto**: 12345
- **Protocolo**: TCP/IP mediante `ServerSocket`
- **Funciones**:
  - Acepta múltiples conexiones de clientes simultáneamente
  - Asigna un ID único a cada jugador (1, 2, 3...)
  - Genera posición aleatoria para cada nuevo jugador
  - Gestiona el estado global del juego (GameState)
  - Procesa comandos de cada cliente independientemente
  - Permite combate individual por jugador
  - Elimina jugadores al desconectarse
  - Envía actualizaciones del estado a cada cliente

### Cliente (Client.java)
- **Conexión**: TCP al servidor en `localhost:12345`
- **Funciones**:
  - Se conecta al servidor mediante `Socket`
  - Recibe su ID de jugador al conectar
  - Envía comandos con su ID al servidor
  - Recibe actualizaciones del estado del juego
  - Interfaz gráfica mediante Lanterna

### Comunicación
- **Formato**: JSON para el intercambio de datos
- **Flujo**:
  1. El cliente recibe su ID al conectar
  2. El cliente envía comandos con su ID (movimiento, ataque, etc.)
  3. El servidor procesa el comando para el jugador específico
  4. El servidor retorna el nuevo estado del juego
  5. El cliente muestra la información actualizada

### Módulos de Red
```
backend/
├── Sockets/
│   ├── Server.java    # Servidor con multiclientes
│   └── Client.java   # Cliente para conectar al servidor
```

## 📦 Estructura del Proyecto

```
game/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── backend/
│   │           ├── core/                 
│   │ # Núcleo del juego           │   ├── GameState.java     # Estado global (multijugador)
│   │           │   └── GameLoop.java      # Loop principal
│   │
│   │           ├── threads/               # Hilos del sistema
│   │           │   ├── InputThread.java   # Entrada de usuario
│   │           │   ├── RenderThread.java  # Renderizado
│   │           │   ├── ReceiverThread.java # Receptor de datos
│   │           │   └── WorldClock.java    # Reloj del mundo
│   │
│   │           ├── entities/              # Entidades del juego
│   │           │   ├── Entity.java        # Clase base
│   │           │   └── Player.java        # Jugador
│   │
│   │           ├── world/                 # Sistema del mundo
│   │           │   └── World.java         # Mundo 2D
│   │
│   │           ├── combat/                # Sistema de combate
│   │           │   ├── CombatSystem.java  # Lógica de combate (individual)
│   │           │   └── Enemy.java         # Enemigos
│   │
│   │           ├── commands/              # Procesamiento de comandos
│   │           │   └── CommandProcessor.java
│   │
│   │           ├── Sockets/               # Comunicación cliente-servidor
│   │           │   ├── Server.java        # Servidor multicliente
│   │           │   └── Client.java       # Cliente del juego
│   │
│   │           └── game/                  # Configuración del juego
│   │               └── GameMode.java     # Modos de juego
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

### Ejecutar el Servidor

El servidor debe iniciarse primero:

```bash
mvn exec:java "-Dexec.mainClass=backend.Sockets.Server"
```

### Ejecutar Clientes

En otras terminales, ejecuta clientes adicionales:

```bash
mvn exec:java "-Dexec.mainClass=backend.Sockets.Client"
```

Puedes ejecutar tantos clientes como desees. Cada uno recibirá un ID único.

### Ejecutar Tests

```bash
mvn test
```

### Crear JAR

```bash
mvn package
```

## 🎯 Cómo Jugar

1. Inicia el servidor: `mvn exec:java "-Dexec.mainClass=backend.Sockets.Server"`
2. Conecta el primer cliente: `mvn exec:java "-Dexec.mainClass=backend.Sockets.Client"`
3. Conecta clientes adicionales en otras terminales
4. Cada jugador ve su ID y se representa con ese número en el mapa
5. Usa comandos de texto para interactuar:
   - `w`, `a`, `s`, `d` - Moverse
   - `attack` / `a` - Atacar (en combate)
   - `run` / `r` - Huir (en combate)
   - `quit` - Salir
6. Cada jugador puede estar en combate independientemente
7. Los demás jugadores pueden moverse mientras uno está en combate

## 🧵 Arquitectura Multihilo

El juego utiliza un diseño multihilo en ambos lados (cliente y servidor):

### En el Servidor:
- **WorldClock**: Hilo independiente para el tiempo del mundo
- **Manejo de Clientes**: Cada cliente conectado se maneja en un hilo separado

### En el Cliente:
- **InputThread**: Captura y procesa la entrada del usuario
- **ReceiverThread**: Recibe mensajes del servidor en paralelo

- **Sincronización**: Uso de `BlockingQueue` y `ConcurrentHashMap` para comunicación thread-safe
- **Estado Compartido**: La clase `GameState` maneja el estado global con variables `volatile`
- **Concurrencia**: Cada hilo opera independientemente con su propio ciclo de vida

## 📊 Formato de Comunicación JSON

### Del servidor al cliente (estado del juego):
```json
{
  "playerId": 1,
  "worldTime": 150,
  "gameMode": "Exploration",
  "players": [
    {"id": 1, "name": "Player1", "x": 5, "y": 3, "hp": 100, "maxHp": 100},
    {"id": 2, "name": "Player2", "x": 8, "y": 6, "hp": 100, "maxHp": 100}
  ],
  "world": {
    "width": 20,
    "height": 10,
    "map": ["###################...", ...],
    "enemies": [...]
  },
  "combats": [
    {"playerId": 1, "playerTurn": true, "enemy": {...}, "lastMessage": "..."}
  ]
}
```

### Del cliente al servidor (comando):
```json
{
  "playerId": 1,
  "command": "w"
}
```

## ✍️ Autor

Velazquez Parral Saul Asaph
