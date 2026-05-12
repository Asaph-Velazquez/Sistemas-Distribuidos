# Realm Of Shadows Microservices

Backend distribuido y frontend web para **Realm Of Shadows**, organizado como un stack de microservicios con Spring Boot, React, nginx y Docker Compose.

## Arquitectura

```text
┌─────────────────────────────────────────────────────────────┐
│                    Cliente / Frontend                       │
│            React + Vite + PWA - Navegador Web               │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               │ HTTP / WebSocket
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    nginx API Gateway                        │
│                     localhost:8000                          │
│                                                             │
│  /auth/*   /profile/*   /games/*   /game/*   /stats/*       │
└──────┬──────────┬──────────┬──────────┬──────────┬──────────┘
       │          │          │          │          │
       ▼          ▼          ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────┐
│  Auth    │ │ Profile  │ │ Matchmaking  │ │  Game    │ │  Stats   │
│ Service  │ │ Service  │ │   Service    │ │ Engine   │ │ Service  │
│  :8081   │ │  :8082   │ │    :8083     │ │  :8084   │ │  :8085   │
└────┬─────┘ └────┬─────┘ └──────┬───────┘ └────┬─────┘ └────┬─────┘
     │            │              │              │            │
     ▼            ▼              ▼              ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────┐
│ H2 Auth  │ │H2 Profile│ │ H2 Games     │ │ Memoria  │ │ H2 Stats │
│  authdb  │ │profiledb │ │matchmakingdb │ │Partidas  │ │ statsdb  │
└──────────┘ └──────────┘ └──────────────┘ └──────────┘ └──────────┘
```

## Servicios

| Servicio | Directorio | Puerto | Responsabilidad |
|---|---|---:|---|
| Auth Service | `auth-service/` | 8081 | Registro, login y validación JWT |
| Profile Service | `profile-service/` | 8082 | Perfil de usuario |
| Matchmaking Service | `matchmaking-service/` | 8083 | Lobby, creación y unión a partidas |
| Game Engine Service | `game-engine-service/` | 8084 | Estado de juego y WebSocket JSON |
| Stats Service | `stats-service/` | 8085 | Estadísticas y leaderboard |
| nginx Gateway | `nginx/` | 8000 | Reverse proxy para APIs |
| Frontend | `frontend/` | 3000 | Cliente React/PWA |

## Endpoints

| Servicio | Endpoint |
|---|---|
| Auth | `GET /auth/health`, `POST /auth/register`, `POST /auth/login`, `GET /auth/validate` |
| Profile | `GET /profile/health`, `GET /profile/{userId}`, `POST /profile/{userId}` |
| Matchmaking | `GET /games/health`, `POST /games/create`, `GET /games`, `POST /games/{gameId}/join` |
| Game Engine | `GET /game/health`, `WS /game/ws/{gameId}` |
| Stats | `GET /stats/health`, `GET /stats/user/{userId}`, `GET /stats/leaderboard` |

## Requisitos

- Docker y Docker Compose
- Java 17 y Maven para ejecutar servicios sin contenedores
- Node.js 20+ para ejecutar el frontend sin contenedores

## Ejecución con Docker Compose

```bash
cp .env.example .env
docker compose --env-file .env up --build -d
```

URLs locales:

| Componente | URL |
|---|---|
| Frontend | `http://localhost:3000` |
| Gateway | `http://localhost:8000` |
| Auth | `http://localhost:8000/auth/health` |
| Matchmaking | `http://localhost:8000/games/health` |
| Game Engine | `http://localhost:8000/game/health` |
| Stats | `http://localhost:8000/stats/health` |

## Configuración

El stack lee variables desde `.env`.

| Variable | Descripción |
|---|---|
| `FRONTEND_PORT` | Puerto público del frontend |
| `GATEWAY_PORT` | Puerto público del gateway |
| `AUTH_PORT`, `PROFILE_PORT`, `MATCHMAKING_PORT`, `GAME_ENGINE_PORT`, `STATS_PORT` | Puertos públicos opcionales de cada servicio |
| `JWT_SECRET` | Secreto usado para firmar JWT |
| `JWT_EXPIRATION` | Duración del token en milisegundos |
| `JAVA_OPTS` | Opciones de JVM para contenedores |

Para producción, cambia `JWT_SECRET` por un valor largo, aleatorio y privado.

## Desarrollo local

Ejecutar un servicio Java:

```bash
cd auth-service
mvn spring-boot:run
```

Ejecutar frontend:

```bash
cd frontend
npm install
npm run dev
```

El frontend en modo desarrollo usa el gateway local `http://localhost:8000` como proxy de APIs y WebSocket.

## Verificación

```bash
docker compose config
mvn -q -DskipTests package
cd frontend && npm run build
```

Pruebas rápidas:

```bash
curl http://localhost:8000/health
curl http://localhost:8000/auth/health
curl http://localhost:8000/games
curl http://localhost:8000/stats/leaderboard
```

## Despliegue en nube

1. Copia `.env.example` a `.env`.
2. Configura `JWT_SECRET`, puertos y `JAVA_OPTS`.
3. Publica el puerto del frontend (`3000`) o del gateway (`8000`) según el proveedor.
4. Ejecuta:

```bash
docker compose --env-file .env up --build -d
```

El `compose.yaml` incluye healthchecks, reinicio automático y dependencias por estado saludable.
