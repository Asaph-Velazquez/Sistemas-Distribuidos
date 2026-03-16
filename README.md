# Sistemas Distribuidos

Proyectos desarrollados para la materia de Sistemas Distribuidos, implementando arquitecturas cliente-servidor y comunicación mediante sockets.

## 📁 Proyectos

### 1. Multihilo
Juego en consola Java con sistema multihilo para gestión concurrente.

**Características:**
- InputThread: Entrada de usuario
- GameLoop: Lógica principal
- RenderThread: Renderizado en consola
- WorldClock: Control del tiempo del mundo

### 2. Cliente-Servidor
Juego en consola Java con arquitectura cliente-servidor utilizando **sockets TCP**.

**Características:**
- **Servidor**: Gestiona el estado del juego, acepta múltiples clientes
- **Cliente**: Conecta al servidor, envía comandos, recibe actualizaciones
- **Comunicación**: Protocolo TCP/IP
- **Puerto**: 12345

**Tecnologías:**
- Java 17
- Maven
- Sockets TCP (java.net)
- JSON (org.json.simple)
- Multihilo

```
┌─────────────┐          ┌─────────────┐
│   Cliente   │◄───────► │  Servidor   │
│  (Client)   │  Socket  │  (Server)   │
└─────────────┘   TCP    └─────────────┘
       │                      │
       │  - Comandos JSON     │  - GameState
       │  - Entrada usuario   │  - World
       │  - Renderizado       │  - Player
       ▼                      ▼
   Consola               Lógica del juego
```

### 3. MultiplesClientes-Servidor
Juego en consola Java con **múltiples clientes** conectados a un servidor único, utilizando **sockets TCP**.

**Características:**
- **Servidor**: Acepta múltiples clientes simultáneamente, mundo compartido
- **Cliente**: Múltiples clientes pueden conectarse, cada uno con ID único (1, 2, 3...)
- **Comunicación**: TCP/IP con JSON
- **Puerto**: 12345
- **Sistema de Combate**: Combate individual por jugador
- **Multihilo**: InputThread, GameLoop, RenderThread, WorldClock, ReceiverThread

**Tecnologías:**
- Java 17
- Maven
- Sockets TCP (java.net)
- JSON (org.json.simple)
- Lanterna (interfaz gráfica)

```
┌─────────┐  ┌─────────┐  ┌─────────┐
│Cliente 1│  │Cliente 2│  │Cliente 3│
└────┬────┘  └────┬────┘  └────┬────┘
     │            │            │
     └────────────┼────────────┘
                  ▼
         ┌───────────────┐
         │    Servidor   │
         │ (Puerto 12345)│
         └───────┬───────┘
                 │
         ┌───────┴───────┐
         │  - GameState  │
         │  - World      │
         │  - Jugadores  │
         │  - Combates   │
         └───────────────┘
```

### 4. MultiplesClientes-MultiplesServidores
Sistema avanzado con **servidor de matchmaking** y **múltiples servidores de juego** utilizando sockets TCP.

**Características:**
- **MatchmakingServer**: Servidor central (puerto 12345) para crear, listar y unirse a partidas
- **Servidores de Juego**: Cada partida en servidor independiente (puertos 50001-50100)
- **Puerto Dinámico**: Los servidores de juego se crean automáticamente
- **Sistema de Combate**: Combate individual por jugador
- **Sincronización**: ConcurrentHashMap, AtomicInteger, volatile, synchronized

**Protocolo de Matchmaking:**
- `CREATE:{username}:{gameName}:{maxPlayers}` - Crear partida
- `LIST` - Listar partidas
- `JOIN:{port}` - Unirse a partida

**Tecnologías:**
- Java 17
- Maven
- Sockets TCP (java.net)
- JSON (org.json.simple)
- ScheduledExecutorService

```
        ┌──────────────────────────────────────────────────────┐
        │              MatchmakingServer (Puerto 12345)        │
        │  ┌─────────────────────────────────────────────────┐ │
        │  │  • Registro de partidas                         │ │
        │  │  • Listado de juegos disponibles                │ │
        │  │  • Asignación de puertos (50001-50100)          │ │
        │  │  • Inicio automático de servidores de juego     │ │
        │  └─────────────────────────────────────────────────┘ │
        └──────────────────────────┬───────────────────────────┘
                                   │
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
           ▼                       ▼                       ▼
    ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
    │ServidorJuego│          │ServidorJuego│          │ServidorJuego│
    │(Puerto 50001)          │(Puerto 50002)          │(Puerto 50003)
    └──────┬──────┘          └──────┬──────┘          └──────┬──────┘
           │                        │                        │
      ┌────┴────┐              ┌────┴────┐              ┌────┴────┐
      │Cliente 1│              │Cliente 3│              │Cliente 5│
      └─────────┘              └─────────┘              └─────────┘
```

### 5. ObjetosDistribuidos
Juego en consola Java con **Objetos Distribuidos** utilizando **RMI (Remote Method Invocation)** y sockets TCP.

**Características:**
- **RMI Broker**: Puerto 1099 para invocación de métodos remotos
- **RemoteMatchmaking**: Objeto remoto para gestión de partidas
- **Sistema de Matchmaking**: Servidor central (puerto 12345)
- **Múltiples Servidores de Juego**: Puertos dinámicos (50001-50100)
- **Sistema de Combate**: Combate individual por jugador
- **Sincronización**: ConcurrentHashMap, AtomicInteger, volatile, synchronized

**Dos Modelos de Comunicación:**
| Modelo | Descripción | Puerto |
|--------|-------------|--------|
| **Sockets TCP** | Comunicación texto/JSON | 12345 (matchmaking), 50001-50100 (juego) |
| **RMI** | Objetos distribuidos con invocación de métodos remotos | 1099 (broker) |

**Tecnologías:**
- Java 17
- Maven
- Java RMI (java.rmi)
- Sockets TCP (java.net)
- JSON (org.json.simple)
- Lanterna (interfaz gráfica)

```
        ┌──────────────────────────────────────────────────────┐
        │              MatchmakingServer (Puerto 12345)        │
        │  ┌─────────────────────────────────────────────────┐ │
        │  │  • Registro de partidas                         │ │
        │  │  • Listado de juegos disponibles                │ │
        │  │  • Asignación de puertos (50001-50100)          │ │
        │  │  • Inicio automático de servidores de juego     │ │
        │  └─────────────────────────────────────────────────┘ │
        └──────────────────────────┬───────────────────────────┘
                                    │
        ┌──────────────────────────┼───────────────────────────┐
        │         RMI Broker (Puerto 1099)                     │
        │  ┌─────────────────────────────────────────────────┐ │
        │  │  • Registry RMI                                 │ │
        │  │  • RemoteMatchmaking (objeto remoto)            │ │
        │  │  • Invocación de métodos remotos                │ │
        │  └─────────────────────────────────────────────────┘ │
        └──────────────────────────┬───────────────────────────┘
                                    │
        ┌──────────────────────────┼───────────────────────────┐
        │                          │                           │
        ▼                          ▼                           ▼
   ┌─────────────┐           ┌─────────────┐            ┌─────────────┐
   │ServidorJuego│           │ServidorJuego│            │ServidorJuego│
   │(Puerto 50001)           │(Puerto 50002)            │(Puerto 50003)
   └──────┬──────┘           └──────┬──────┘            └──────┬──────┘
          │                         │                          │
     ┌────┴────┐               ┌────┴────┐                ┌────┴────┐
     │Cliente 1│               │Cliente 3│                │Cliente 5│
     └─────────┘               └─────────┘                └─────────┘
```

## ✍️ Autor

Velazquez Parral Saul Asaph
