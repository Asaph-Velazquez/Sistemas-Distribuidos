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

## 🖥️ Arquitectura Cliente-Servidor

```
┌─────────────┐         ┌─────────────┐
│   Cliente   │◄───────►│  Servidor   │
│  (Client)   │  Socket │  (Server)   │
└─────────────┘   TCP    └─────────────┘
       │                      │
       │  - Comandos JSON     │  - GameState
       │  - Entrada usuario   │  - World
       │  - Renderizado       │  - Player
       ▼                      ▼
   Consola               Lógica del juego
```

## ✍️ Autor

Velazquez Parral Saul Asaph
