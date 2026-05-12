# PWA to Microservices Migration Plan

## TL;DR

> Migrar el PWA "Realm of Shadows" de arquitectura monolítica Spring Boot + React a arquitectura de microservicios con 6 servicios independientes desplegables con Docker Compose.

> **Deliverables**:
> - 6 microservicios Dockerizados (Auth, Matchmaking, Game Engine, Stats, User Profile, Gateway)
> - nginx como API Gateway y balanceador
> - PWA actualizada con modo offline completo
> - compose.yaml orchestrating todos los servicios

> **Estimated Effort**: Large
> **Parallel Execution**: YES - 4 waves
> **Critical Path**: Auth → Matchmaking → Game Engine → Integration

---

## Context

### Original Request
Migrar PWA "Realm of Shadows" (Spring Boot monolítico + React PWA) a arquitectura de microservicios.

### Interview Summary

**Key Discussions**:
- **Objective**: Separación de responsabilidades entre servicios independientes
- **Services requested**: 6 (Gateway, Auth, Matchmaking, Game Engine, Stats, User Profile)
- **Infrastructure**: Docker Compose
- **Communication**: REST + Nginx como API Gateway
- **Offline**: Mantener modo offline completo del PWA actual

**Current Architecture**:
- Frontend: React 19 + Vite + Tailwind CSS + PWA (Service Worker + Dexie)
- Backend: Spring Boot monolítico (REST + WebSocket en puerto 8080)
- Modo Offline: Game Engine reimplementado en JavaScript

### Metis Review

**Identified Gaps** (addressed):
- **Gap 1**: Comunicación WebSocket entre servicios - RESOLVED: Game Engine Service maneja WebSocket directamente, otros servicios REST
- **Gap 2**: Estado compartido entre servicios - RESOLVED: User Profile Service centraliza datos de usuario, Stats Service para leaderboards
- **Gap 3**: Sincronización offline - RESOLVED: PWA mantiene cola de acciones, sincroniza al reconectar

---

## Work Objectives

### Core Objective
Migrar de arquitectura monolítica a microservicios con 6 servicios independientes, manteniendo funcionalidad completa del PWA y modo offline.

### Concrete Deliverables
- [ ] Auth Service: Login/registro de usuarios, JWT tokens
- [ ] Matchmaking Service: Crear/listar/unirse a partidas (REST)
- [ ] Game Engine Service: WebSocket para tiempo real, lógica del juego
- [ ] Stats Service: Leaderboards, estadísticas de partida
- [ ] User Profile Service: Perfiles de usuario, historial
- [ ] API Gateway (nginx): Enrutamiento y balanceo
- [ ] PWA actualizada pointing a Gateway
- [ ] Docker Compose orchestrating todo

### Definition of Done
- [ ] curl localhost:8000/auth/health → OK
- [ ] curl localhost:8000/matchmaking/games → lista vacía
- [ ] ws://localhost:8000/game → Connection established
- [ ] docker compose up → todos los servicios corriendo
- [ ] PWA online mode funcionando
- [ ] PWA offline mode funcionando

### Must Have
- [ ] Autenticación JWT funcional
- [ ] Matchmaking REST API funcionando
- [ ] WebSocket game connectivity
- [ ] Stats persistidos
- [ ] Perfiles de usuario
- [ ] Gateway en nginx puerto 8000
- [ ] Modo offline PWA intacto

### Must NOT Have
- [ ] Acoplamiento entre servicios (cada uno independiente)
- [ ] Base de datos monolítica (cada servicio su propia DB o compartida)
- [ ] Single point of failure (redundancia en nginx)
- [ ] Estados en memoria que se pierdan (persistir en BD)

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: YES (Maven, npm)
- **Automated tests**: Tests-after
- **Framework**: JUnit (Java) + Vitest (Frontend)
- **Cada microservicio**: Tests unitarios propios

### QA Policy
Every task MUST include agent-executed QA scenarios.

- **Java Services**: mvn test + curl testing
- **Frontend**: Playwright - navegación PWA, offline detection
- **Integration**: docker compose up + integration tests

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Foundation - start immediately):
├── T1: Project scaffolding - crear estructura microservicios
├── T2: Auth Service - Core + JWT
├── T3: User Profile Service - entidades + repository
├── T4: Shared config - propiedades comunes
└── T5: Docker base image + compose skeleton

Wave 2 (Core Services):
├── T6: Matchmaking Service - REST API lobby
├── T7: Game Engine Service - WebSocket handler
├── T8: Stats Service - leaderboards
├── T9: Database setup - PostgreSQL/MySQL per service
└── T10: Service-to-service communication

Wave 3 (Gateway + Integration):
├── T11: API Gateway nginx config
├── T12: Service discovery config
├── T13: Auth integration with User Profile
├── T14: Matchmaking integration with Game Engine
└── T15: PWA updates - new API endpoints

Wave 4 (Final):
├── T16: Full docker compose orchestation
├── T17: Offline mode enhancement
├── T18: Integration tests
└── T19: Documentation

Critical Path: T1 → T2 → T6 → T7 → T11 → T16 → T18
Parallel Speedup: ~60% faster than sequential
Max Concurrent: 5 (Waves 1 & 2)
```

---

## TODOs

- [ ] 1. **Project Structure + Docker Skeleton**

  **What to do**:
  - Crear directorio microservices/ con subdirectorios por servicio
  - Crear compose.yaml base con servicios vacíos
  - Definir network común game-network
  - Crear Dockerfile base para Java services

  **QA Scenarios**:
  ```
  Scenario: Docker compose syntax valid
    Tool: Bash
    Preconditions: Ninguno
    Steps:
      1. docker compose config
    Expected Result: No errors, valid YAML
    Evidence: .sisyphus/evidence/task-1-compose-valid.txt
  ```

  **Commit**: YES
  - Message: feat(microservices): initial structure
  - Files: compose.yaml, Dockerfile.base

- [ ] 2. **Auth Service - Core Implementation**

  **What to do**:
  - Spring Boot service con Spring Security + JWT
  - Endpoints: /auth/register, /auth/login, /auth/validate
  - Entity: User con username, password (BCrypt), email
  - Repository: JPA con H2 (dev) / PostgreSQL (prod)
  - Token JWT con expiration

  **Must NOT do**:
  - No guardar passwords en plain text
  - No exponer user passwords en responses

  **Recommended Agent Profile**:
  > Category: deep (lógica de seguridad requiere cuidado)
  - Skills: java, spring-boot, security

  **References**:
  - ServicioWeb/src/main/java/backend/ - patrones a seguir

  **QA Scenarios**:
  ```
  Scenario: User registration
    Tool: Bash
    Preconditions: Service running
    Steps:
      1. curl -X POST localhost:8081/auth/register -H "Content-Type: application/json" -d '{"username":"test","password":"pass123","email":"test@test.com"}'
    Expected Result: 200 OK, user created

  Scenario: User login
    Tool: Bash
    Preconditions: User registered
    Steps:
      1. curl -X POST localhost:8081/auth/login -H "Content-Type: application/json" -d '{"username":"test","password":"pass123"}'
    Expected Result: 200 OK, JWT token returned
  ```

  **Evidence**: task-2-auth-register.json, task-2-auth-login.json

  **Commit**: YES
  - Message: feat(auth): JWT authentication service
  - Files: auth-service/, compose.yaml

- [ ] 3. **User Profile Service**

  **What to do**:
  - Spring Boot service independiente
  - Endpoints: /profile/{userId}, /profile/{userId}/stats, /profile/{userId}/history
  - Entity: UserProfile (userId, displayName, avatar, createdAt)
  - Repository: JPA
  - Comunicación REST con Auth Service

  **References**:
  - ServicioWeb/src/main/java/backend/ - patrones similares

  **QA Scenarios**:
  ```
  Scenario: Get user profile
    Tool: Bash
    Preconditions: Userregistered
    Steps:
      1. curl localhost:8082/profile/1
    Expected Result: 200 OK, profile JSON
  ```

  **Evidence**: task-3-profile.json

  **Commit**: YES
  - Message: feat(profile): user profile service
  - Files: profile-service/

- [ ] 4. **Matchmaking Service**

  **What to do**:
  - Spring Boot REST service
  - Endpoints migrados de GameController actual
  - /games/create, /games, /games/{id}/join, /games/{id}
  - Gestión de lobbys, NO lógica de juego
  - Comunicación con Game Engine para crear instancia

  **References**:
  - ServicioWeb/src/main/java/backend/controller/GameController.java - copiar endpoints
  - ServicioWeb/src/main/java/backend/service/MatchmakingService.java - lógica a adaptar

  **QA Scenarios**:
  ```
  Scenario: Create game
    Tool: Bash
    Preconditions: Service running, auth token
    Steps:
      1. curl -X POST localhost:8083/games/create -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"name":"testGame","maxPlayers":4}'
    Expected Result: 200 OK, game info with port

  Scenario: List games
    Tool: Bash
    Preconditions: Game created
    Steps:
      1. curl localhost:8083/games
    Expected Result: 200 OK, games list
  ```

  **Evidence**: task-4-matchmaking.json

  **Commit**: YES
  - Message: feat(matchmaking): lobby management service
  - Files: matchmaking-service/

- [ ] 5. **Game Engine Service**

  **What to do**:
  - WebSocket service para tiempo real
  - Handler migrado de GameWebSocketHandler
  - Lógica de juego: World, Player, Enemy, Combat
  - Instancias de juego por puerto/sesión
  - Solo maneja lógica de juego, NO matchmaking

  **References**:
  - ServicioWeb/src/main/java/backend/config/GameWebSocketHandler.java
  - ServicioWeb/src/main/java/backend/service/GameService.java
  - ServicioWeb/src/main/java/backend/core/GameState.java

  **QA Scenarios**:
  ```
  Scenario: WebSocket connection
    Tool: Bash
    Preconditions: Game created
    Steps:
      1. wscat -c ws://localhost:8084/game/{gameId}
      2. Send: join
      3. Send: getState
    Expected Result: Connected, game state received
  ```

  **Evidence**: task-5-websocket.json

  **Commit**: YES
  - Message: feat(game-engine): WebSocket game logic service
  - Files: game-engine-service/

- [ ] 6. **Stats Service**

  **What to do**:
  - Spring Boot REST service
  - Endpoints: /stats/leaderboard, /stats/game/{gameId}, /stats/user/{userId}
  - entity: GameStats (gameId, winner, duration, players)
  - entity: UserStats (userId, wins, losses, kills)
  - Calcular leaderboards

  **QA Scenarios**:
  ```
  Scenario: Get leaderboard
    Tool: Bash
    Preconditions: Games played
    Steps:
      1. curl localhost:8085/stats/leaderboard
    Expected Result: 200 OK, ranked users
  ```

  **Evidence**: task-6-stats.json

  **Commit**: YES
  - Message: feat(stats): statistics and leaderboards
  - Files: stats-service/

- [ ] 7. **API Gateway (nginx)**

  **What to do**:
  - nginx configurado como reverse proxy
  - Rutas:
    - /auth/* → auth-service:8081
    - /profile/* → profile-service:8082
    - /games/* → matchmaking-service:8083
    - /game/ws/* → game-engine-service:8084
    - /stats/* → stats-service:8085
  - Rate limiting opcional
  - Health checks

  **References**:
  - nginx documentation for reverse proxy

  **QA Scenarios**:
  ```
  Scenario: Gateway routing
    Tool: Bash
    Preconditions: All services running
    Steps:
      1. curl localhost:8000/auth/health
      2. curl localhost:8000/games
      3. curl localhost:8000/stats/leaderboard
    Expected Result: All routed correctly
  ```

  **Evidence**: task-7-gateway.json

  **Commit**: YES
  - Message: feat(gateway): nginx API Gateway
  - Files: nginx/, compose.yaml

- [ ] 8. **PWA Integration**

  **What to do**:
  - Actualizar API base URL en PWA
  - Endpoints:
    - Auth: /auth/register, /auth/login
    - Profile: /profile/me
    - Games: /games/*
    - Stats: /stats/*
    - WebSocket: /game/ws/{gameId}
  - Mantener Dexie para offline
  - Detección online/offline

  **References**:
  - PWA actual en Progressive Web App/frontend/

  **QA Scenarios**:
  ```
  Scenario: PWA online mode
    Tool: Playwright
    Preconditions: PWA running
    Steps:
      1. Go to http://localhost:5173
      2. Login with credentials
      3. Create game
      4. Play game
    Expected Result: Full flow working

  Scenario: PWA offline mode
    Tool: Playwright
    Preconditions: PWA online worked
    Steps:
      1. Disconnect network
      2. App shows offline indicator
      3. Game continues against AI
    Expected Result: Offline mode works
  ```

  **Evidence**: task-8-pwa.json, task-8-pwa-offline.json

  **Commit**: YES
  - Message: feat(pwa): integrate with microservices
  - Files: Progressive Web App/frontend/

- [ ] 9. **Docker Compose Orchestration**

  **What to do**:
  - compose.yaml completo con todos los servicios
  - Dependencies: auth → profile → matchmaking → game-engine → stats
  - Health checks para todos
  - Volume para PostgreSQL
  - nginx como entrypoint

  **QA Scenarios**:
  ```
  Scenario: Full stack up
    Tool: Bash
    Preconditions: Ninguno
    Steps:
      1. docker compose down -v (clean)
      2. docker compose up -d --build
      3. Wait 60s
      4. docker ps
      5. Check all services running
    Expected Result: 6 containers running
  ```

  **Evidence**: task-9-compose.json

  **Commit**: YES
  - Message: feat(orchestration): docker compose with all services
  - Files: compose.yaml

- [ ] 10. **Final Integration Tests**

  **What to do**:
  - Test flow completo:
    1. Register user
    2. Login
    3. Create game → matchmaking
    4. Join game → WebSocket
    5. Play (move, attack)
    6. Win/Lose → stats updated
  - Test failover offline

  **QA Scenarios**:
  ```
  Scenario: Full user journey
    Tool: Bash
    Preconditions: Stack running
    Steps:
      1. Register → Login → Create Game → Join → Play → Stats
    Expected Result: All steps successful

  Scenario: Offline mode
    Tool: Playwright
    Preconditions: Full journey worked
    Steps:
      1. Disconnect
      2. Play offline
      3. Reconnect
      4. Sync
    Expected Result: Works
  ```

  **Evidence**: task-10-integration.json

  **Commit**: YES
  - Message: test(integration): full flow tests
  - Files: Test reports

---

## Final Verification Wave

- [ ] F1. **Plan Compliance Audit** — oracle
  Verificar que todos los servicios especificados están implementados
  Output: Must Have [10/10] | VERDICT

- [ ] F2. **Code Quality Review** — deep
  Verificar: código limpio, sin duplicación, documentación
  Output: Build [PASS/FAIL] | VERDICT

- [ ] F3. **Docker Integration** — deep
  Verificar: todos los servicios containerizados, comunican correctamente
  Output: Containers [6/6] | VERDICT

- [ ] F4. **PWA Offline Test** — deep
  Verificar: modo offline funciona igual que antes
  Output: Offline [WORKING/NOT WORKING] | VERDICT

---

## Commit Strategy

- **1**: feat(microservices): initial structure
- **2**: feat(auth): JWT authentication service
- **3**: feat(profile): user profile service
- **4**: feat(matchmaking): lobby management service
- **5**: feat(game-engine): WebSocket game logic service
- **6**: feat(stats): statistics and leaderboards
- **7**: feat(gateway): nginx API Gateway
- **8**: feat(pwa): integrate with microservices
- **9**: feat(orchestration): docker compose with all services
- **10**: test(integration): full flow tests

---

## Success Criteria

### Verification Commands
```bash
curl localhost:8000/auth/health  # → OK
curl localhost:8000/games      # → []
docker compose ps             # → 6 services running
```

### Final Checklist
- [ ] Auth Service responde en puerto 8081
- [ ] Profile Service responde en puerto 8082
- [ ] Matchmaking Service responde en puerto 8083
- [ ] Game Engine WebSocket en puerto 8084
- [ ] Stats Service responde en puerto 8085
- [ ] nginx Gateway en puerto 8000 routing correctly
- [ ] PWA online mode working
- [ ] PWA offline mode working
- [ ] Docker compose orchestration working