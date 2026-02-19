# Juego Distribuido - Sistemas Cliente-Servidor con Sockets

## 📋 Descripción

Proyecto de juego en consola desarrollado en Java que implementa una **arquitectura cliente-servidor** utilizando **sockets TCP** para la comunicación en red. El servidor gestiona el estado del juego y los clientes se conectan para interactuar con él, demostrando conceptos de sistemas distribuidos y programación con sockets.

## 🎮 Características

- **Sistema Multihilo**: 4 hilos concurrentes trabajando en paralelo
  - **InputThread**: Captura y procesa la entrada del usuario
  - **GameLoop**: Gestiona la lógica principal del juego
  - **RenderThread**: Renderiza el estado del juego en consola
  - **WorldClock**: Controla el tiempo del mundo del juego

- **Modos de Juego**:
  - **Exploración**: Navega por el mundo y descubre
  - **Combate**: Sistema de combate por turnos contra enemigos

- **Sistema de Combate**:
  - Combate basado en turnos
  - Sistema de ataque y defensa
  - Enemigos con IA básica

- **Mundo del Juego**:
  - Mundo 2D con sistema de coordenadas
  - Jugador con inventario
  - Entidades y enemigos

## 🖥️ Arquitectura Cliente-Servidor con Sockets

### Servidor (Server.java)
- **Puerto**: 12345
- **Protocolo**: TCP/IP mediante `ServerSocket`
- **Funciones**:
  - Acepta múltiples conexiones de clientes simultáneamente
  - Gestiona el estado global del juego (GameState)
  - Procesa comandos recibidos de los clientes
  - Envía actualizaciones del estado del juego a cada cliente
  - Usa hilos para manejar múltiples clientes en paralelo

### Cliente (Client.java)
- **Conexión**: TCP al servidor en `localhost:12345`
- **Funciones**:
  - Se conecta al servidor mediante `Socket`
  - Envía comandos del usuario al servidor
  - Recibe actualizaciones del estado del juego
  - Interfaz de consola para entrada/salida

### Comunicación
- **Formato**: JSON para el intercambio de datos
- **Flujo**:
  1. El cliente envía comandos (movimiento, ataque, etc.)
  2. El servidor procesa el comando y actualiza el estado
  3. El servidor retorna el nuevo estado del juego al cliente
  4. El cliente muestra la información actualizada

### Módulos de Red
```
backend/
├── Sockets/
│   ├── Server.java    # Servidor con multiclientes
│   └── Client.java   # Cliente para conectar al servidor
```

## 📦 Estructura del Proyecto

El proyecto está organizado en módulos según su funcionalidad:

```
game/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── backend/
│   │           ├── core/                  # Núcleo del juego
│   │           │   ├── Main.java          # Punto de entrada
│   │           │   ├── GameState.java     # Estado global
│   │           │   └── GameLoop.java      # Loop principal
│   │           │
│   │           ├── threads/               # Hilos del sistema
│   │           │   ├── InputThread.java   # Entrada de usuario
│   │           │   ├── RenderThread.java  # Renderizado
│   │           │   └── WorldClock.java    # Reloj del mundo
│   │           │
│   │           ├── entities/              # Entidades del juego
│   │           │   ├── Entity.java        # Clase base
│   │           │   └── Player.java        # Jugador
│   │           │
│   │           ├── world/                 # Sistema del mundo
│   │           │   └── World.java         # Mundo 2D
│   │           │
│   │           ├── combat/                # Sistema de combate
│   │           │   ├── CombatSystem.java  # Lógica de combate
│   │           │   └── Enemy.java         # Enemigos
│   │           │
│   │           ├── commands/              # Procesamiento de comandos
│   │           │   └── CommandProcessor.java
│   │           │
│   │           ├── Sockets/               # Comunicación cliente-servidor
│   │           │   ├── Server.java       # Servidor multicliente
│   │           │   └── Client.java       # Cliente del juego
│   │           │
│   │           └── game/                  # Configuración del juego
│   │               └── GameMode.java      # Modos de juego
│   │
│   └── test/
│       └── java/
│           └── backend/
│               └── AppTest.java
├── pom.xml
└── README.md
```

## 📋 Organización Modular

El proyecto sigue una arquitectura modular clara para facilitar el mantenimiento y escalabilidad:

- **core**: Contiene las clases fundamentales del juego (Main, GameState, GameLoop)
- **threads**: Agrupa todos los hilos del sistema (InputThread, RenderThread, WorldClock)
- **entities**: Define las entidades del juego (Entity, Player)
- **world**: Maneja la lógica del mundo 2D (World)
- **combat**: Sistema completo de combate (CombatSystem, Enemy)
- **commands**: Procesamiento de comandos del usuario (CommandProcessor)
- **game**: Configuración y modos de juego (GameMode)

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
mvn exec:java -Dexec.mainClass="backend.Sockets.Server"
```

### Ejecutar el Cliente

En otra terminal, ejecuta el cliente:

```bash
mvn exec:java -Dexec.mainClass="backend.Sockets.Client"
```

### Ejecutar Tests

```bash
mvn test
```

### Crear JAR

```bash
mvn package
```

## 🎯 Cómo Jugar

1. Inicia el servidor con `mvn exec:java "-Dexec.mainClass=backend.Sockets.Server"`
2. Conecta el cliente con `mvn exec:java "-Dexec.mainClass=backend.Sockets.Client"`
3. Usa comandos de texto en el cliente para interactuar
4. Navega por el mundo en modo exploración
5. Enfrenta enemigos en modo combate
6. Escribe "quit" para salir

## 🧵 Arquitectura Multihilo

El juego utiliza un diseño multihilo en ambos lados (cliente y servidor):

### En el Servidor:
- **WorldClock**: Hilo independiente para el tiempo del mundo
- **Manejo de Clientes**: Cada cliente conectado se maneja en un hilo separado

### En el Cliente:
- **InputThread**: Captura y procesa la entrada del usuario
- **ReceiverThread**: Recibe mensajes del servidor en paralelo

- **Sincronización**: Uso de `BlockingQueue` para comunicación thread-safe entre hilos
- **Estado Compartido**: La clase `GameState` maneja el estado global con variables `volatile`
- **Concurrencia**: Cada hilo opera independientemente con su propio ciclo de vida

## ✍️ Autor

Velazquez Parral Saul Asaph