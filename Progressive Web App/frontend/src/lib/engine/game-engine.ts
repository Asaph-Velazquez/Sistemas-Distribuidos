import { type Position, WorldEntity, type EnemyEntity } from './world';

export interface PlayerEntity {
  name: string;
  x: number;
  y: number;
  hp: number;
  maxHp: number;
}

export interface GameStateData {
  players: Record<string, PlayerEntity>;
  enemies: Record<string, ReturnType<EnemyEntity['toJSON']>>;
  world: ReturnType<WorldEntity['getMapForClient']>;
  gameName: string;
  hostName: string;
  maxPlayers: number;
  currentPlayers: number;
}

export class GameEngine {
  private world: WorldEntity;
  private players: Map<string, PlayerEntity>;
  private gameName: string;
  private hostName: string;
  private maxPlayers: number;
  private gameMode: 'offline' | 'online';

  constructor(gameName: string = 'Local Game', hostName: string = 'Player', maxPlayers: number = 4) {
    this.world = new WorldEntity(15, 10);
    this.players = new Map();
    this.gameName = gameName;
    this.hostName = hostName;
    this.maxPlayers = maxPlayers;
    this.gameMode = 'offline';
  }

  addPlayer(name: string): PlayerEntity {
    if (this.players.size >= this.maxPlayers) {
      throw new Error('Game is full');
    }

    const pos = this.world.getRandomWalkablePosition();
    const player: PlayerEntity = {
      name,
      x: pos.x,
      y: pos.y,
      hp: 50,
      maxHp: 50
    };

    this.players.set(name, player);
    return player;
  }

  removePlayer(name: string): boolean {
    return this.players.delete(name);
  }

  getPlayer(name: string): PlayerEntity | undefined {
    return this.players.get(name);
  }

  movePlayer(playerName: string, direction: 'w' | 'a' | 's' | 'd'): { success: boolean; reason?: string } {
    const player = this.players.get(playerName);
    if (!player) {
      return { success: false, reason: 'Player not found' };
    }

    let newX = player.x;
    let newY = player.y;

    switch (direction) {
      case 'w': newY--; break;
      case 's': newY++; break;
      case 'a': newX--; break;
      case 'd': newX++; break;
    }

    if (!this.world.isWalkable(newX, newY)) {
      return { success: false, reason: 'Cannot move there' };
    }

    const enemyAtTarget = this.world.getEnemyAt(newX, newY);
    if (enemyAtTarget) {
      return { success: false, reason: 'Enemy in the way' };
    }

    for (const [, p] of this.players) {
      if (p.name !== playerName && p.x === newX && p.y === newY) {
        return { success: false, reason: 'Another player is there' };
      }
    }

    player.x = newX;
    player.y = newY;
    return { success: true };
  }

  attack(playerName: string, targetX?: number, targetY?: number): AttackResult {
    const player = this.players.get(playerName);
    if (!player) {
      return { result: 'error', message: 'Player not found' };
    }

    let targetEnemy: EnemyEntity | undefined;

    if (targetX !== undefined && targetY !== undefined) {
      targetEnemy = this.world.getEnemyAt(targetX, targetY);
    } else {
      const adjacentPositions = [
        { x: player.x - 1, y: player.y },
        { x: player.x + 1, y: player.y },
        { x: player.x, y: player.y - 1 },
        { x: player.x, y: player.y + 1 }
      ];

      for (const pos of adjacentPositions) {
        const enemy = this.world.getEnemyAt(pos.x, pos.y);
        if (enemy) {
          targetEnemy = enemy;
          break;
        }
      }
    }

    if (!targetEnemy) {
      return { result: 'error', message: 'No enemy to attack' };
    }

    const playerDamage = 10 + Math.floor(Math.random() * 6);
    const enemyDefeated = targetEnemy.takeDamage(playerDamage);

    if (enemyDefeated) {
      this.world.removeEnemy(targetEnemy.x, targetEnemy.y);
      return {
        result: 'enemyDefeated',
        enemy: targetEnemy.name,
        damageToEnemy: playerDamage
      };
    }

    const enemyDamage = targetEnemy.dealDamage();
    player.hp = Math.max(0, player.hp - enemyDamage);

    if (player.hp === 0) {
      return {
        result: 'playerDefeated',
        enemy: targetEnemy.name,
        damageToEnemy: playerDamage,
        damageToPlayer: enemyDamage
      };
    }

    return {
      result: 'exchange',
      damageToEnemy: playerDamage,
      damageToPlayer: enemyDamage
    };
  }

  getState(): GameStateData {
    const playersRecord: Record<string, PlayerEntity> = {};
    for (const [name, player] of this.players) {
      playersRecord[name] = { ...player };
    }

    const enemiesRecord: Record<string, ReturnType<EnemyEntity['toJSON']>> = {};
    for (const enemy of this.world.getEnemies()) {
      enemiesRecord[`${enemy.x},${enemy.y}`] = enemy.toJSON();
    }

    return {
      players: playersRecord,
      enemies: enemiesRecord,
      world: this.world.getMapForClient(),
      gameName: this.gameName,
      hostName: this.hostName,
      maxPlayers: this.maxPlayers,
      currentPlayers: this.players.size
    };
  }

  setGameMode(mode: 'offline' | 'online') {
    this.gameMode = mode;
  }

  getGameMode(): 'offline' | 'online' {
    return this.gameMode;
  }
}

export interface AttackResult {
  result: 'enemyDefeated' | 'playerDefeated' | 'exchange' | 'error';
  enemy?: string;
  damageToEnemy?: number;
  damageToPlayer?: number;
  message?: string;
}