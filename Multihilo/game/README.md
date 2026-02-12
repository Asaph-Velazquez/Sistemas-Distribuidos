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

```
game/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── backend/
│   │           ├── Main.java              # Punto de entrada
│   │           ├── GameLoop.java          # Loop principal del juego
│   │           ├── GameState.java         # Estado global del juego
│   │           ├── InputThread.java       # Hilo de entrada
│   │           ├── RenderThread.java      # Hilo de renderizado
│   │           ├── WorldClock.java        # Reloj del mundo
│   │           ├── CommandProcessor.java  # Procesador de comandos
│   │           ├── Player.java            # Clase del jugador
│   │           ├── Entity.java            # Entidad base
│   │           ├── World.java             # Mundo del juego
│   │           ├── GameMode.java          # Modos de juego
│   │           └── Combat/
│   │               ├── CombatSystem.java  # Sistema de combate
│   │               └── Enemy.java         # Enemigos
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

### Ejecutar el Juego

```bash
mvn exec:java
```

O ejecutar directamente la clase Main:

```bash
mvn exec:java -Dexec.mainClass="backend.Main"
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