# Juego Multihilo - Sistemas Distribuidos

## 📋 Descripción

Proyecto de juego en consola desarrollado en Java que implementa un sistema multihilo para la gestión concurrente de entrada de usuario, lógica del juego, renderizado y reloj del mundo. Este proyecto demuestra conceptos de programación concurrente.

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

### Ejecutar el Juego

```bash
mvn exec:java
```

O ejecutar directamente especificando la clase Main:

```bash
mvn exec:java -Dexec.mainClass="backend.core.Main"
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

1. Inicia el juego con `mvn exec:java`
2. Usa comandos de texto para interactuar con el juego
3. Navega por el mundo en modo exploración
4. Enfrenta enemigos en modo combate

## 🧵 Arquitectura Multihilo

El juego utiliza un diseño multihilo para separar responsabilidades:

- **Sincronización**: Uso de `BlockingQueue` para comunicación thread-safe entre hilos
- **Estado Compartido**: La clase `GameState` maneja el estado global con variables `volatile`
- **Concurrencia**: Cada hilo opera independientemente con su propio ciclo de vida

## ✍️ Autor

Velazquez Parral Saul Asaph