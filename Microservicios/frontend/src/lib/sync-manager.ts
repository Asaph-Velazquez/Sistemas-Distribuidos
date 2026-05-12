import { db, type GameAction, type SyncMetadata } from './db';
import { v4 as uuidv4 } from 'uuid';

export interface SyncResult {
  success: boolean;
  reason?: string;
  appliedActions?: string[];
  rejectedActions?: { id: string; reason: string }[];
  conflicts?: ConflictInfo[];
}

interface ConflictInfo {
  actionId: string;
  serverState: unknown;
  localState: unknown;
}

class SyncManager {
  private isOnline = navigator.onLine;
  private syncInProgress = false;
  private serverUrl: string;

  constructor() {
    this.serverUrl = import.meta.env.VITE_SERVER_URL || 'http://localhost:8080';
    
    window.addEventListener('online', () => this.handleOnline());
    window.addEventListener('offline', () => this.handleOffline());
  }

  private handleOnline() {
    this.isOnline = true;
    console.log('[Sync] Connection restored');
    this.triggerSync();
  }

  private handleOffline() {
    this.isOnline = false;
    console.log('[Sync] Offline mode');
  }

  public getConnectionStatus() {
    return this.isOnline;
  }

  public async queueAction(
    type: GameAction['type'],
    payload: GameAction['payload'],
    gameId: string
  ): Promise<string> {
    const uuid = uuidv4();
    const action: GameAction = {
      uuid,
      type,
      payload,
      timestamp: Date.now(),
      gameId,
      status: 'pending'
    };

    await db.actions.add(action);
    console.log(`[Sync] Action queued: ${type}`, uuid);

    if (this.isOnline) {
      this.triggerSync();
    }

    return uuid;
  }

  public async getPendingActions(gameId: string): Promise<GameAction[]> {
    return db.actions
      .where('gameId')
      .equals(gameId)
      .and(action => action.status === 'pending')
      .toArray();
  }

  public async triggerSync(gameId?: string): Promise<SyncResult> {
    if (this.syncInProgress || !this.isOnline) {
      return { success: false, reason: this.syncInProgress ? 'sync_in_progress' : 'offline' };
    }

    this.syncInProgress = true;

    try {
      const metadata = await db.syncMetadata.toArray();
      const gameIds = gameId ? [gameId] : [...new Set(metadata.map(m => m.gameId))];

      for (const gid of gameIds) {
        await this.syncGame(gid);
      }

      return { success: true };
    } catch (error) {
      console.error('[Sync] Error:', error);
      return { success: false, reason: 'sync_error' };
    } finally {
      this.syncInProgress = false;
    }
  }

  private async syncGame(gameId: string): Promise<SyncResult> {
    const pendingActions = await this.getPendingActions(gameId);
    
    if (pendingActions.length === 0) {
      return { success: true };
    }

    const metadata = await db.syncMetadata.where('gameId').equals(gameId).first();
    const lastSyncedId = metadata?.lastSyncedActionId || null;

    try {
      const response = await fetch(`${this.serverUrl}/api/games/${gameId}/sync`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          actions: pendingActions.map(a => ({
            uuid: a.uuid,
            type: a.type,
            payload: a.payload,
            timestamp: a.timestamp
          })),
          lastSyncedActionId: lastSyncedId
        })
      });

      if (!response.ok) {
        throw new Error(`Sync failed: ${response.status}`);
      }

      const result = await response.json();

      for (const action of pendingActions) {
        if (result.appliedActions?.includes(action.uuid)) {
          await db.actions.where('uuid').equals(action.uuid).modify({ status: 'applied' });
        } else if (result.rejectedActions?.find((r: { id: string }) => r.id === action.uuid)) {
          await db.actions.where('uuid').equals(action.uuid).modify({ status: 'rejected' });
        }
      }

      await db.syncMetadata.put({
        gameId,
        lastSyncedActionId: result.lastActionId || pendingActions[pendingActions.length - 1].uuid,
        lastSyncTimestamp: Date.now()
      });

      return {
        success: true,
        appliedActions: result.appliedActions,
        rejectedActions: result.rejectedActions
      };
    } catch (error) {
      console.error('[Sync] Game sync error:', error);
      return { success: false, reason: 'network_error' };
    }
  }

  public async updateSyncMetadata(gameId: string, data: Partial<SyncMetadata>) {
    const existing = await db.syncMetadata.where('gameId').equals(gameId).first();
    if (existing) {
      await db.syncMetadata.update(existing.id!, data);
    } else {
      await db.syncMetadata.add({ gameId, lastSyncedActionId: null, lastSyncTimestamp: null, ...data });
    }
  }
}

export const syncManager = new SyncManager();