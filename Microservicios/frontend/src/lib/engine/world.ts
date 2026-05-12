export interface Position {
  x: number;
  y: number;
}

export class EnemyEntity {
  public readonly name: string;
  public x: number;
  public y: number;
  public hp: number;
  public maxHp: number;
  public readonly attack: number;

  constructor(name: string, x: number, y: number, maxHp: number, attack: number) {
    this.name = name;
    this.x = x;
    this.y = y;
    this.maxHp = maxHp;
    this.hp = maxHp;
    this.attack = attack;
  }

  dealDamage(): number {
    return this.attack + Math.floor(Math.random() * 5);
  }

  takeDamage(damage: number): boolean {
    this.hp = Math.max(0, this.hp - damage);
    return this.hp === 0;
  }

  moveTo(x: number, y: number) {
    this.x = x;
    this.y = y;
  }

  toJSON() {
    return {
      name: this.name,
      x: this.x,
      y: this.y,
      hp: this.hp,
      maxHp: this.maxHp
    };
  }
}

export class WorldEntity {
  public map: string[][];
  public readonly width: number;
  public readonly height: number;
  private enemies: Map<string, EnemyEntity>;
  private random: Random;

  constructor(width: number, height: number, seed?: number) {
    this.width = width;
    this.height = height;
    this.map = [];
    this.enemies = new Map();
    this.random = seed !== undefined ? new Random(seed) : new Random();
    this.generateMap();
    this.generateEnemies();
  }

  private generateMap() {
    for (let y = 0; y < this.height; y++) {
      const row: string[] = [];
      for (let x = 0; x < this.width; x++) {
        if (y === 0 || y === this.height - 1 || x === 0 || x === this.width - 1) {
          row.push('#');
        } else {
          row.push('.');
        }
      }
      this.map.push(row);
    }

    this.map[3][3] = '#';
    this.map[4][5] = '#';
    this.map[6][2] = '#';
  }

  private generateEnemies() {
    const numEnemies = 3 + this.random.nextInt(3);
    const names = ['Goblin', 'Orc', 'Troll', 'Skeleton', 'Zombie'];

    for (let i = 0; i < numEnemies; i++) {
      let x: number, y: number;
      do {
        x = 1 + this.random.nextInt(this.width - 2);
        y = 1 + this.random.nextInt(this.height - 2);
      } while (!this.isWalkable(x, y) || this.getEnemyAt(x, y));

      const name = names[this.random.nextInt(names.length)];
      const health = 20 + this.random.nextInt(20);
      const attack = 5 + this.random.nextInt(8);

      const enemy = new EnemyEntity(name, x, y, health, attack);
      this.enemies.set(`${x},${y}`, enemy);
    }
  }

  isWalkable(x: number, y: number): boolean {
    if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
      return false;
    }
    return this.map[y][x] === '.';
  }

  getEnemyAt(x: number, y: number): EnemyEntity | undefined {
    return this.enemies.get(`${x},${y}`);
  }

  removeEnemy(x: number, y: number): boolean {
    return this.enemies.delete(`${x},${y}`);
  }

  getEnemies(): EnemyEntity[] {
    return Array.from(this.enemies.values());
  }

  getRandomWalkablePosition(): Position {
    let x: number, y: number;
    let attempts = 0;
    do {
      x = 1 + this.random.nextInt(this.width - 2);
      y = 1 + this.random.nextInt(this.height - 2);
      attempts++;
    } while (!this.isWalkable(x, y) && attempts < 100);
    return { x, y };
  }

  getMapForClient(): { map: Record<string, string>; width: number; height: number } {
    const map: Record<string, string> = {};
    for (let y = 0; y < this.height; y++) {
      map[`row${y}`] = this.map[y].join('');
    }
    return { map, width: this.width, height: this.height };
  }
}

class Random {
  private seed: number;

  constructor(seed?: number) {
    this.seed = seed ?? Math.floor(Math.random() * 2147483647);
  }

  nextInt(max: number): number {
    this.seed = (this.seed * 16807) % 2147483647;
    return (this.seed - 1) % max;
  }
}