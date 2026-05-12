# Realm Of Shadows - Reporte de Microservicios

## Resumen

El modulo `Microservicios` migra el juego **Realm Of Shadows** desde una aplicacion web monolitica hacia una arquitectura distribuida basada en servicios independientes. La solucion separa autenticacion, perfiles, matchmaking, motor de juego, estadisticas y gateway HTTP, usando Spring Boot para los servicios Java y nginx como API Gateway.

La separacion conceptual esta bien planteada: cada dominio principal vive en su propio proyecto Maven, tiene su propia configuracion y puede construirse como contenedor independiente. Sin embargo, la integracion completa todavia requiere ajustes importantes para que frontend, gateway, REST y WebSocket hablen el mismo contrato.

## Servicios identificados

| Servicio | Ruta local | Puerto host | Responsabilidad | Estado actual |
|---|---|---:|---|---|
| Auth Service | `auth-service/` | 8081 | Registro, login, validacion JWT | Implementado, requiere correccion de clave JWT |
| Profile Service | `profile-service/` | 8082 | Perfil de usuario y resumen de stats | Implementado basico |
| Matchmaking Service | `matchmaking-service/` | 8083 | Crear, listar, consultar y unirse a partidas | Implementado basico |
| Game Engine Service | `game-engine-service/` | 8084 | Logica de juego y WebSocket | Implementado parcial |
| Stats Service | `stats-service/` | 8085 | Estadisticas de usuarios y leaderboard | Implementado basico |
| nginx Gateway | `nginx/nginx.conf` | 8000 | Reverse proxy hacia los servicios | Implementado, requiere alinear rutas |

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

### Flujo esperado

1. El usuario se registra o inicia sesion en `Auth Service`.
2. El cliente usa el token JWT para operar sobre perfiles, partidas y estadisticas.
3. `Matchmaking Service` administra el lobby y las partidas disponibles.
4. `Game Engine Service` mantiene el estado vivo de una partida y recibe eventos por WebSocket.
5. `Stats Service` registra resultados y expone rankings.
6. `nginx` centraliza el acceso externo para evitar exponer contratos internos directamente.

## Endpoints principales

### Auth Service

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/auth/health` | Health check |
| POST | `/auth/register` | Registra usuario |
| POST | `/auth/login` | Autentica usuario y devuelve token |
| GET | `/auth/validate` | Valida token Bearer |

### Profile Service

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/profile/health` | Health check |
| GET | `/profile/{userId}` | Obtiene perfil |
| POST | `/profile/{userId}` | Actualiza `displayName` y `avatar` |
| GET | `/profile/{userId}/stats` | Devuelve resumen de perfil/stats |

### Matchmaking Service

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/games/health` | Health check |
| POST | `/games/create` | Crea partida |
| GET | `/games` | Lista partidas en espera |
| GET | `/games/{gameId}` | Consulta una partida |
| POST | `/games/{gameId}/join` | Une un jugador |
| DELETE | `/games/{gameId}` | Elimina partida |

### Game Engine Service

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/game/health` | Health check |
| WS | `/game/{gameId}` | Canal WebSocket del juego |

### Stats Service

| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | `/stats/health` | Health check |
| GET | `/stats/user/{userId}` | Estadisticas de usuario |
| GET | `/stats/leaderboard` | Ranking |
| POST | `/stats/user/{userId}/win` | Registra victoria |
| POST | `/stats/user/{userId}/loss` | Registra derrota |

## Ejecucion

### Requisitos

- Java 17+
- Maven
- Docker y Docker Compose
- Node.js/npm si se ejecuta el frontend localmente

### Levantar servicios con Docker Compose

```bash
cd Microservicios
docker compose up --build
```

Gateway:

```text
http://localhost:8000
```

Health checks esperados por gateway:

```bash
curl http://localhost:8000/auth/health
curl http://localhost:8000/profile/health
curl http://localhost:8000/games/health
curl http://localhost:8000/game/health
curl http://localhost:8000/stats/health
curl http://localhost:8000/health
```

### Ejecutar un servicio individual

```bash
cd Microservicios/auth-service
mvn spring-boot:run
```

Cada servicio escucha internamente en `8080`; Docker Compose publica esos puertos como `8081` a `8085`.

## Hallazgos de revision

### Correcto

- La separacion por dominio es clara y permite desplegar servicios de forma independiente.
- Cada servicio Java tiene su propio `pom.xml`, `application.yml`, entidades, repositorios y controladores.
- `nginx` funciona como punto de entrada unico y evita que el cliente conozca todos los servicios directamente.
- Los endpoints de health existen en todos los servicios.
- Matchmaking no contiene logica de combate; la logica viva esta en `game-engine-service`.
- Las operaciones de persistencia estan separadas por servicio, lo cual respeta el principio de base de datos por microservicio.

### Problemas importantes

1. **Contratos del frontend no coinciden con los microservicios.**
   El frontend consulta `/api/games`, `/api/games/create` y `ws://.../ws/game`, pero el gateway de microservicios expone `/games/*` y `/game/ws/*`, mientras que el backend WebSocket registra `/game/{gameId}`. Asi, el modo online no queda integrado de punta a punta.

2. **El protocolo WebSocket no coincide.**
   El frontend envia JSON (`{ type: "move", direction: "w" }`), pero `GameWebSocketHandler` espera texto separado por dos puntos (`join:gameId:player:userId`, `move:dx:dy`). Esto provoca errores o mensajes ignorados.

3. **El JWT no usa la clave configurada.**
   `AuthService` define una clave aleatoria en memoria con `Keys.secretKeyFor(...)`. Aunque `application.yml` tiene `jwt.secret`, no se usa. Al reiniciar el servicio, los tokens previos dejan de validarse.

4. **Los servicios usan H2 en memoria con `create-drop`.**
   Esto es aceptable para una practica o demo, pero no conserva usuarios, partidas ni estadisticas despues de reiniciar contenedores.

5. **Los healthchecks de Docker pueden fallar.**
   `compose.yaml` usa `curl` dentro de contenedores basados en `eclipse-temurin:17-jre-alpine`, pero la imagen runtime no instala `curl`.

6. **No hay validacion/autorizacion real entre servicios.**
   `Auth Service` valida tokens, pero `Profile`, `Matchmaking`, `Game Engine` y `Stats` no exigen token ni verifican permisos.

7. **`depends_on` no espera servicios saludables.**
   Docker Compose arranca contenedores en orden, pero no garantiza que cada API ya este lista para recibir trafico.

8. **Falta integracion entre servicios de dominio.**
   Matchmaking crea partidas, pero no notifica al motor de juego; Game Engine crea estado en memoria al recibir WebSocket; Stats no se actualiza automaticamente al terminar una partida.

## Mejoras recomendadas

### Prioridad alta

- Alinear rutas entre frontend, nginx y backend:
  - REST: usar una sola convencion (`/games`, `/auth`, `/profile`, `/stats`).
  - WebSocket: decidir entre `/game/{gameId}` o `/game/ws/{gameId}` y aplicarlo en frontend, gateway y Spring.
- Unificar el formato de mensajes WebSocket. Recomendado: JSON tipado con campos `type`, `gameId`, `userId`, `playerName`, `direction`, `targetX`, `targetY`.
- Corregir `AuthService` para leer `jwt.secret` y `jwt.expiration` desde configuracion.
- Agregar validacion de token en servicios protegidos mediante filtro/interceptor o validacion contra `/auth/validate`.
- Instalar `curl` en la imagen runtime o cambiar healthchecks a una alternativa disponible.

### Prioridad media

- Reemplazar H2 en memoria por PostgreSQL o MySQL por servicio, con volumen persistente.
- Agregar contratos DTO consistentes entre frontend y backend. Ejemplo: `GameResponse` usa `id`, pero el frontend espera `port`.
- Agregar manejo de errores con respuestas JSON explicitas, no solo `400` o `401` vacios.
- Incorporar CORS y configuracion de origenes por ambiente.
- Usar `depends_on.condition: service_healthy` si la version de Compose lo permite.
- Agregar logs estructurados por servicio.

### Prioridad baja

- Agregar OpenAPI/Swagger por servicio para documentar contratos.
- Centralizar configuracion comun con perfiles `dev`, `test` y `prod`.
- Crear pruebas de contrato para REST y WebSocket.
- Agregar observabilidad basica: metricas, trazas y correlation id.

## Verificacion sugerida

### Compose

```bash
cd Microservicios
docker compose config
docker compose up --build
```

### Auth

```bash
curl -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo\",\"password\":\"demo123\",\"email\":\"demo@test.com\"}"
```

### Matchmaking

```bash
curl -X POST http://localhost:8000/games/create \
  -H "Content-Type: application/json" \
  -d "{\"gameName\":\"Partida Demo\",\"hostName\":\"demo\",\"hostUserId\":1,\"maxPlayers\":4}"

curl http://localhost:8000/games
```

### Stats

```bash
curl http://localhost:8000/stats/leaderboard
curl -X POST "http://localhost:8000/stats/user/1/win?kills=3"
curl http://localhost:8000/stats/user/1
```

## Estado general

La arquitectura de microservicios esta bien encaminada como separacion de componentes, pero el sistema todavia debe considerarse **parcialmente integrado**. Los servicios existen y exponen endpoints, pero la experiencia completa depende de alinear contratos entre frontend, gateway y backends, especialmente en matchmaking y WebSocket.

Para una entrega academica, el proyecto demuestra correctamente los conceptos principales de microservicios: servicios independientes, gateway, contenedores, persistencia por dominio y comunicacion REST/WS. Para una ejecucion estable de punta a punta, las mejoras de prioridad alta deben atenderse primero.
