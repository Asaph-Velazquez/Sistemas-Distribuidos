import Dexie, { type Table } from 'dexie';

export interface GameAction {
  id?: number;
  uuid: string;
  type: 'move' | 'attack' | 'join' | 'leave' | 'useItem';
  payload: Record<string, unknown>;
  timestamp: number;
  gameId: string;
  status: 'pending' | 'applied' | 'rejected' | 'conflict';
  localStateHash?: string;
}

export interface SavedGame {
  id?: number;
  name: string;
  createdAt: number;
  lastPlayedAt: number;
  gameState: string;
  isOnlineGame: boolean;
  serverGameId?: string;
}

export interface PlayerProfile {
  id?: number;
  username: string;
  jwtToken?: string;
  refreshToken?: string;
  tokenExpiresAt?: number;
  stats: {
    totalGames: number;
    wins: number;
    losses: number;
    enemiesDefeated: number;
  };
  achievements: string[];
}

export interface SyncMetadata {
  id?: number;
  gameId: string;
  lastSyncedActionId: string | null;
  lastSyncTimestamp: number | null;
}

class GameDatabase extends Dexie {
  actions!: Table<GameAction>;
  savedGames!: Table<SavedGame>;
  profiles!: Table<PlayerProfile>;
  syncMetadata!: Table<SyncMetadata>;

  constructor() {
    super('RealmOfShadowsDB');
    this.version(1).stores({
      actions: '++id, uuid, gameId, timestamp, status',
      savedGames: '++id, name, createdAt, lastPlayedAt',
      profiles: '++id, username',
      syncMetadata: '++id, gameId'
    });
  }
}

export const db = new GameDatabase();