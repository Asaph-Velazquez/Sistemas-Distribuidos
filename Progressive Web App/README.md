# Realm Of Shadows - PWA

## Descripción

**Realm Of Shadows** es un juego roguelike dungeon multijugador implementado como **Progressive Web App (PWA)** con soporte completo para modo offline.

### Modos de Juego

- **Online**: Multijugador en tiempo real via WebSocket + REST API
- **Offline**: Partida local contra IA, funciona sin conexión a internet

### Características PWA

- **Instalable**: Se puede instalar en dispositivos como una app nativa
- **Offline**: Juego local completo sin conexión + sincronización al reconnectar
- **Responsive**: Funciona en desktop, tablet y móvil
- **Auto-actualizable**: Service Worker con actualización automática
- **Detección automática**: Cambia entre modo online/offline según conectividad

## Tecnologías

### Backend
- Java 17+ / Spring Boot 4
- Spring Web (REST API)
- Spring WebSocket
- Maven

### Frontend
- React 19 + Vite 8
- Tailwind CSS 4
- **vite-plugin-pwa** (Service Worker + Manifest)
- **Dexie** (IndexedDB para modo offline)
- **Game Engine Local** (Reimplementación en JS de la lógica del servidor)

## Arquitectura PWA

### Modo Online (Con servidor)

```
┌─────────────────────────────────────────────────────────────┐
│                     NAVEGADOR (CLIENTE)                     │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    FRONTEND PWA                       │  │
│  │  ┌─────────────┐    ┌─────────────┐    ┌───────────┐  │  │
│  │  │   App UI    │    │  Service    │    │ IndexedDB │  │  │
│  │  │  (React)    │◄──►│   Worker    │◄──►│  (Dexie)  │  │  │
│  │  └─────────────┘    └──────┬──────┘    └───────────┘  │  │
│  │                            │                          │  │
│  │                      ┌─────┴─────┐                    │  │
│  │                      │  WS Client│                    │  │
│  │                      │  REST API │                    │  │
│  │                      └───────────┘                    │  │
│  └────────────────────────────┬──────────────────────────┘  │
└───────────────────────────────┼─────────────────────────────┘
                                │
         ┌──────────────────────┼──────────────────────┐
         │                      │                      │
   ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
   │ REST API  │          │ WebSocket │          │  Match    │
   │  :8080    │          │ /ws/game  │          │  maker    │
   └─────┬─────┘          └─────┬─────┘          └───────────┘
         │                      │
         └──────────┬───────────┘
                    │
          ┌─────────▼─────────┐
          │   BACKEND (Java)  │
          │   Spring Boot     │
          │ - GameController  │
          │ - GameWebSocket   │
          │ - GameService     │
          │ - GameState       │
          └───────────────────┘
```

### Modo Offline (Sin servidor)

```
┌─────────────────────────────────────────────────────────────┐
│                     NAVEGADOR (CLIENTE)                     │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐  │
│  │                 FRONTEND PWA (OFFLINE)                │  │
│  │  ┌─────────────┐    ┌─────────────┐    ┌───────────┐  │  │
│  │  │   App UI    │    │  Service    │    │ IndexedDB │  │  │
│  │  │  (React)    │◄──►│   Worker    │◄──►│  (Dexie)  │  │  │
│  │  └─────────────┘    └──────┬──────┘    └───────────┘  │  │
│  │                            │                          │  │
│  │  ┌─────────────────────────┴────────────────────────┐ │  │
│  │  │           GAME ENGINE (JavaScript)               │ │  │
│  │  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │ │  │
│  │  │  │  World   │  │  Enemy   │  │ GameEngine   │    │ │  │
│  │  │  │ (Map/AI) │  │ (Combat) │  │ (Movement)   │    │ │  │
│  │  │  └──────────┘  └──────────┘  └──────────────┘    │ │  │
│  │  └──────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│            SIN CONEXIÓN - Solo datos locales                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Flujo de Datos

| Modo | Componente | Destino |
|------|------------|---------|
| **Online** | REST API calls | `http://localhost:8080/api/*` |
| **Online** | WebSocket | `ws://localhost:8080/ws/game` |
| **Offline** | Game Engine (JS) | IndexedDB local |
| **Sync** | Sync Manager | Cola de acciones → servidor al reconnect |

## Servicio Worker y Caching

### Manifest.json (Generado automáticamente)

```javascript
{
  name: 'Realm Of Shadows',
  short_name: 'RealmShadows',
  description: 'Juego dungeon roguelike - Modo offline disponible',
  theme_color: '#0f0f0f',
  background_color: '#0f0f0f',
  display: 'standalone',
  orientation: 'portrait',
  start_url: '/',
  icons: [{ src: 'favicon.svg', sizes: 'any', type: 'image/svg+xml' }]
}
```

### Estrategias de Caching

| Recurso | Estrategia | Descripción |
|---------|------------|-------------|
| Assets estáticos (JS, CSS, HTML) | **Precache** | Cacheados en instalación |
| API REST | **NetworkFirst** | Intenta red, cae a cache |
| Imágenes/SVGs | **CacheFirst** | Sirve desde cache primero |
| WebSocket | **No caching** | Tiempo real, no cacheable |

### Runtime Caching para API

```javascript
{
  urlPattern: /^https:\/\/localhost:8080\/api\/.*/i,
  handler: 'NetworkFirst',
  options: {
    cacheName: 'api-cache',
    expiration: { maxEntries: 50, maxAgeSeconds: 86400 }
  }
}
```

## API REST

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/games/health` | Health check |
| `POST` | `/api/games/create` | Crear partida |
| `GET` | `/api/games` | Listar partidas |
| `POST` | `/api/games/{port}/join` | Unirse a partida |
| `DELETE` | `/api/games/{port}` | Eliminar partida |

## WebSocket

### Endpoint: `ws://localhost:8080/ws/game`

### Mensajes (Cliente → Servidor)

| Comando | Descripción |
|---------|-------------|
| `join` | Unirse a partida |
| `move` | Movimiento WASD |
| `attack` | Atacar enemigo |
| `getState` | Solicitar estado |

### Mensajes (Servidor → Cliente)

| Comando | Descripción |
|---------|-------------|
| `joined` | Confirmación de unión |
| `gameState` | Estado del juego |
| `combatResult` | Resultado de combate |
| `error` | Error |

## Estructura del Proyecto

```
Progressive Web App/
├── src/main/java/backend/         # Spring Boot (Backend Java)
│   ├── controller/                # REST API + WebSocket
│   ├── config/                    # WebSocket config
│   ├── service/                   # Game logic
│   ├── core/                      # Game state
│   └── combat/                    # Enemy logic
├── frontend/                      # React PWA
│   ├── src/
│   │   ├── lib/
│   │   │   ├── db.ts              # IndexedDB (Dexie)
│   │   │   ├── sync-manager.ts    # Sync online/offline
│   │   │   └── engine/
│   │   │       ├── world.ts       # World logic (JS)
│   │   │       └── game-engine.ts  # Game engine (JS)
│   │   └── App.jsx                # Main UI
│   ├── vite.config.js             # PWA + vite-plugin-pwa
│   ├── nginx.conf                  # Nginx config (Docker)
│   └── public/                    # Static assets
├── compose.yaml                   # Docker compose
└── Dockerfile                     # Backend Docker
```

## Instalación y Ejecución

### Requisitos
- Java 17+
- Maven 3.6+
- Node.js 18+
- npm

### Backend

```bash
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Docker (Opcional)

```bash
docker compose up
```

## Verificación

| Prueba | URL |
|--------|-----|
| Frontend (PWA) | `http://localhost:5173` |
| API REST | `http://localhost:8080/api/games/health` |
| WebSocket | `ws://localhost:8080/ws/game` |

## Modo Offline

La app detecta automáticamente pérdida de conexión y permite seguir jugando:

### Funcionalidades Offline
- **Partida Local**: Juego completo contra IA (mismos algoritmos que el servidor)
- **IndexedDB**: Almacena estado del juego, acciones pendientes, perfil del jugador
- **Sincronización**: Al reconectar, sincroniza acciones pendientes con el servidor

### Flujo Offline → Online
1. Sin conexión → La app automáticamente entra en "Modo Offline"
2. El jugador puede crear partida local y jugar contra bots
3. Al recuperar conexión → La app detecta y permite volver al modo online
4. Acciones offline se guardan para sincronizar (implementación futura)

### Detección de Conexión
- `navigator.onLine` + eventos `online`/`offline`
- Verificación activa contra `/api/games/health`
- Indicador visual en la UI (● En línea / ○ Offline)

## Repositorio

https://github.com/Asaph-Velazquez/Sistemas-Distribuidos.git
