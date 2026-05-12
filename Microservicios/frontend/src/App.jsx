import { useEffect, useMemo, useRef, useState } from 'react'
import { GameEngine } from './lib/engine/game-engine'
import { syncManager } from './lib/sync-manager'

function App() {
  const wsRef = useRef(null)
  const engineRef = useRef(null)
  const [isOnline, setIsOnline] = useState(false)
  const [status, setStatus] = useState('Detectando conexión...')
  const [playerName, setPlayerName] = useState('')
  const [gameName, setGameName] = useState('Mi Partida')
  const [maxPlayers, setMaxPlayers] = useState(4)
  const [games, setGames] = useState([])
  const [joinPort, setJoinPort] = useState('')
  const [currentGamePort, setCurrentGamePort] = useState(null)
  const [gameState, setGameState] = useState(null)
  const [selectedEnemyKey, setSelectedEnemyKey] = useState(null)
  const [gameMode, setGameMode] = useState('online')

  const players = gameState?.players ?? {}
  const enemies = gameState?.enemies ?? {}
  const me = useMemo(() => Object.values(players).find((p) => p.name === playerName), [players, playerName])
  const nearbyEnemies = useMemo(() => {
    if (!me) return []
    return Object.entries(enemies).filter(([, e]) => Math.abs(e.x - me.x) + Math.abs(e.y - me.y) === 1)
  }, [enemies, me])
  const playersByPos = useMemo(() => {
    const map = {}
    for (const [id, p] of Object.entries(players)) map[`${p.x},${p.y}`] = { ...p, id, isMe: p.name === playerName }
    return map
  }, [players, playerName])
  const enemiesByPos = useMemo(() => {
    const map = {}
    for (const [key, e] of Object.entries(enemies)) map[`${e.x},${e.y}`] = { ...e, key }
    return map
  }, [enemies])

  const checkConnection = async () => {
    try { await fetch('/api/games/health', { method: 'HEAD', signal: AbortSignal.timeout(2000) }); return true } catch { return false }
  }

  useEffect(() => {
    const init = async () => {
      const online = await checkConnection()
      setIsOnline(online)
      if (online) {
        setGameMode('online')
        setStatus('Conectado. Crea o únete a una partida.')
        refreshGames()
        connect()
      } else {
        setGameMode('offline')
        setStatus('Sin conexión. Usa Partida Local.')
      }
    }
    init()
    const onOnline = () => { setIsOnline(true); checkConnection().then(c => { if(c) { setGameMode('online'); setStatus('Conexión restaurada'); refreshGames() }})}
    const onOffline = () => { setIsOnline(false); setGameMode('offline'); setStatus('Sin conexión') }
    window.addEventListener('online', onOnline)
    window.addEventListener('offline', onOffline)
    return () => { window.removeEventListener('online', onOnline); window.removeEventListener('offline', onOffline) }
  }, [])

  useEffect(() => {
    const movementHandler = (event) => {
      const target = event.target
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return
      const key = event.key.toLowerCase()
      if (!['w', 'a', 's', 'd'].includes(key)) return
      event.preventDefault()
      if (gameMode === 'offline' && engineRef.current) {
        const result = engineRef.current.movePlayer(playerName, key)
        if (result.success) { setGameState(engineRef.current.getState()); setStatus(`Te moviste ${key.toUpperCase()}`) }
        else setStatus(result.reason || 'No se puede mover')
      } else if (gameMode === 'online' && currentGamePort) {
        send({ type: 'move', direction: key })
      }
    }
    window.addEventListener('keydown', movementHandler)
    return () => window.removeEventListener('keydown', movementHandler)
  }, [gameMode, playerName, currentGamePort])

  const connect = () => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const ws = new WebSocket(`${wsProtocol}//${window.location.host}/ws/game`)
    wsRef.current = ws
    ws.onopen = () => setStatus('WebSocket activo')
    ws.onclose = () => setStatus('WebSocket desconectado')
    ws.onerror = () => setStatus('Error WebSocket')
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      if (data.type === 'gameState') { setGameState(data); return }
      if (data.type === 'joined') { setCurrentGamePort(data.port); setStatus(`Unido a ${data.gameName}`); return }
      if (data.type === 'combatResult') {
        if (data.result === 'enemyDefeated') setStatus(`Derrotaste a ${data.enemy}`)
        else if (data.result === 'playerDefeated') setStatus(`${data.enemy} te venció`)
        else setStatus(`Intercambio: ${data.damageToEnemy} vs ${data.damageToPlayer}`)
        return
      }
      if (data.type === 'error') setStatus(`Error: ${data.message}`)
    }
  }

  const send = (payload) => { if (wsRef.current?.readyState === WebSocket.OPEN) wsRef.current.send(JSON.stringify(payload)) }
  const refreshGames = async () => { try { const res = await fetch('/api/games'); const data = await res.json(); setGames(data.games || []) } catch { setStatus('Error al cargar') } }
  const createGame = async () => {
    if (!playerName.trim() || !gameName.trim()) { setStatus('Ingresa jugador y nombre'); return }
    try {
      const res = await fetch('/api/games/create', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: playerName.trim(), gameName: gameName.trim(), maxPlayers: Number(maxPlayers) }) })
      const data = await res.json()
      setCurrentGamePort(data.port)
      setStatus(`Partida ${data.gameName} creada`)
      send({ type: 'join', port: data.port, playerName: playerName.trim() })
      refreshGames()
    } catch { setStatus('Error al crear') }
  }
  const createGameOffline = () => {
    const name = playerName.trim() || 'Jugador'
    const game = gameName.trim() || 'Partida Local'
    const engine = new GameEngine(game, name, maxPlayers)
    engine.addPlayer(name)
    engineRef.current = engine
    setGameState(engine.getState())
    setGameMode('offline')
    setStatus(`Partida local "${game}" iniciada. Usa W A S D.`)
  }
  const joinGame = async () => {
    if (!playerName.trim() || !joinPort) { setStatus('Ingresa jugador y selecciona'); return }
    try {
      const res = await fetch(`/api/games/${joinPort}/join`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ playerName: playerName.trim() }) })
      if (!res.ok) { setStatus('No se pudo unir'); return }
      const data = await res.json()
      setCurrentGamePort(data.port)
      send({ type: 'join', port: Number(joinPort), playerName: playerName.trim() })
    } catch { setStatus('Error al unirse') }
  }
  const exitOfflineMode = () => { engineRef.current = null; setGameMode('online'); setGameState(null); setStatus('Saliste del modo offline'); connect(); refreshGames() }
  const attackSelectedEnemy = () => {
    if (gameMode === 'offline' && engineRef.current) {
      const target = selectedEnemyKey ? enemies[selectedEnemyKey] : null
      const result = engineRef.current.attack(playerName, target?.x, target?.y)
      if (result.result === 'enemyDefeated') setStatus(`Derrotaste a ${result.enemy}`)
      else if (result.result === 'playerDefeated') { setStatus(`${result.enemy} te venció`); engineRef.current = null; setGameMode('online') }
      else if (result.result === 'exchange') setStatus(`Intercambio: ${result.damageToEnemy} vs ${result.damageToPlayer}`)
      else setStatus(result.message || 'Error')
      setGameState(engineRef.current?.getState() || null)
      return
    }
    const target = selectedEnemyKey ? enemies[selectedEnemyKey] : null
    send({ type: 'attack', targetX: target?.x, targetY: target?.y })
  }

  const renderBoard = () => {
    if (!gameState?.world) return null
    const rows = []
    for (let y = 0; y < gameState.world.height; y++) {
      const rowText = gameState.world.map?.[`row${y}`] || ''
      const rowCells = []
      for (let x = 0; x < rowText.length; x++) {
        const tile = rowText[x]
        const player = playersByPos[`${x},${y}`]
        const enemy = enemiesByPos[`${x},${y}`]
        rowCells.push(<div key={`${x}-${y}`} className={`board-tile ${tile === '#' ? 'tile-wall' : 'tile-floor'}`}>{tile === '#' ? <span>▦</span> : <span>·</span>}{enemy && <div className="unit unit-enemy"><span>👹</span><span className="unit-hp">{enemy.hp}</span></div>}{player && <div className={`unit ${player.isMe ? 'unit-me' : 'unit-player'}`}><span>{player.isMe ? '🧙' : '🛡'}</span><span className="unit-hp">{player.hp}</span></div>}</div>)
      }
      rows.push(<div key={`row-${y}`} className="board-row">{rowCells}</div>)
    }
    return rows
  }

  return (
    <main className="mx-auto max-w-7xl p-4 md:p-8">
      {!isOnline && <div className="mb-4 rounded-lg bg-amber-600/20 border border-amber-500/50 px-4 py-2 text-amber-200">Modo offline - Puedes jugar localmente</div>}
      <header className="mb-6 rounded-2xl border border-white/10 bg-black/20 p-4 backdrop-blur md:p-6">
        <div className="flex items-center justify-between">
          <div><p className="text-xs uppercase tracking-[0.3em] text-amber-300/80">Dungeon Control Panel</p><h1 className="mt-2 text-3xl font-bold text-stone-100 md:text-5xl">Realm Of Shadows</h1></div>
          <div className="text-right"><p className={`text-sm ${isOnline ? 'text-emerald-400' : 'text-amber-400'}`}>{isOnline ? '● En línea' : '○ Offline'}</p><p className="text-xs text-stone-400">Modo: {gameMode}</p></div>
        </div>
      </header>
      <section className="grid gap-4 lg:grid-cols-[360px_1fr]">
        <aside className="game-card rounded-2xl p-4 md:p-6">
          <h2 className="mb-3 text-xl font-semibold text-stone-100">Lobby</h2>
          <div className="space-y-3">
            <input className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100 outline-none focus:border-amber-400" placeholder="Tu nombre" value={playerName} onChange={(e) => setPlayerName(e.target.value)} />
            <input className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100 outline-none focus:border-amber-400" placeholder="Nombre de partida" value={gameName} onChange={(e) => setGameName(e.target.value)} />
            <input type="number" min={2} max={8} className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100" value={maxPlayers} onChange={(e) => setMaxPlayers(e.target.value)} />
            <div className="grid grid-cols-2 gap-2">
              {isOnline && <button className="rounded-lg bg-emerald-700 px-3 py-2 font-semibold text-emerald-50" onClick={createGame}>Crear Online</button>}
              <button className={`rounded-lg bg-amber-700 px-3 py-2 font-semibold text-amber-50 ${!isOnline ? 'col-span-2' : ''}`} onClick={createGameOffline}>Partida Local</button>
            </div>
          </div>
          <div className="mt-5 rounded-lg border border-white/10 bg-black/25 p-3">
            <p className="mb-2 text-xs uppercase tracking-[0.2em] text-stone-400">{gameMode === 'offline' ? 'Partida Local' : isOnline ? 'Partidas disponibles' : 'Sin conexión'}</p>
            {gameMode === 'offline' ? (<div className="rounded-lg border border-amber-500/30 bg-amber-900/20 p-3"><p className="text-sm text-amber-200">Jugando offline</p><p className="text-xs text-amber-300/70">{gameName}</p></div>) : isOnline ? (<><div className="max-h-52 space-y-2 overflow-auto pr-1">{games.length === 0 && <p className="text-sm text-stone-500">No hay partidas</p>}{games.map((g) => <button key={g.port} className="w-full rounded-lg border border-white/10 bg-slate-900/60 p-3 text-left" onClick={() => setJoinPort(String(g.port))}><p className="text-sm font-semibold text-stone-200">{g.gameName}</p><p className="text-xs text-stone-400">{g.hostName} · {g.currentPlayers}/{g.maxPlayers}</p></button>)}</div><div className="mt-3 flex gap-2"><input className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100" placeholder="Puerto" value={joinPort} onChange={(e) => setJoinPort(e.target.value)} /><button className="rounded-lg bg-violet-700 px-4 py-2 font-semibold text-violet-50" onClick={joinGame}>Unirse</button></div></>) : <p className="text-sm text-stone-400">Sin conexión al servidor</p>}
          </div>
          {gameMode === 'offline' ? (<button className="mt-4 w-full rounded-lg border border-rose-500/30 bg-rose-900/20 px-3 py-2 text-sm text-rose-200" onClick={exitOfflineMode}>Salir del Modo Local</button>) : isOnline && (<button className="mt-4 w-full rounded-lg border border-white/20 bg-black/20 px-3 py-2 text-sm text-stone-200" onClick={connect}>Reconectar</button>)}
        </aside>
        <section className="space-y-4">
          <div className="game-card rounded-2xl p-4 md:p-6">
            <div className="mb-3 flex items-center justify-between"><h2 className="text-xl font-semibold text-stone-100">Arena</h2><p className="text-xs text-stone-400">{gameMode === 'offline' ? 'Local' : currentGamePort ? `Puerto ${currentGamePort}` : '-'}</p></div>
            <div className="overflow-auto rounded-xl border border-white/10 bg-black/45 p-3">{renderBoard() || <p className="text-stone-500">{isOnline ? 'Elige o crea una partida' : 'Inicia una partida local'}</p>}</div>
            <p className="mt-3 text-xs text-stone-400">W A S D para moverte</p>
          </div>
          <div className="grid gap-4 xl:grid-cols-2">
            <div className="game-card rounded-2xl p-4 md:p-6"><h3 className="mb-3 text-lg font-semibold text-stone-100">Jugadores</h3><div className="space-y-2">{Object.entries(players).length === 0 && <p className="text-sm text-stone-500">Sin jugadores</p>}{Object.entries(players).map(([id, p]) => <div key={id} className="rounded-lg border border-white/10 bg-black/30 p-2 text-sm"><p className="text-stone-200">{p.name} {p.name === playerName ? '(Tu)' : ''}</p><p className="text-stone-400">HP {p.hp}/{p.maxHp}</p></div>)}</div></div>
            <div className="game-card rounded-2xl border-rose-400/20 p-4 md:p-6"><h3 className="mb-3 text-lg font-semibold text-rose-100">Combate</h3>{me ? <p className="mb-3 text-sm text-rose-100/90">Tu vida: {me.hp}/{me.maxHp}</p> : <p className="mb-3 text-sm text-stone-500">Sin jugador</p>}{nearbyEnemies.length === 0 ? <p className="text-sm text-stone-500">No hay enemigos cerca</p> : <><div className="mb-3 flex flex-wrap gap-2">{nearbyEnemies.map(([key, enemy]) => <button key={key} className={`rounded-md border px-3 py-1 text-sm ${key === selectedEnemyKey ? 'border-rose-200 bg-rose-600 text-rose-50' : 'border-white/15 bg-black/25 text-stone-300'}`} onClick={() => setSelectedEnemyKey(key)}>{enemy.name} ({enemy.hp}/{enemy.maxHp})</button>)}</div><button className="w-full rounded-lg bg-rose-700 px-3 py-2 font-semibold text-rose-50" onClick={attackSelectedEnemy}>Atacar</button></>}</div>
          </div>
          <div className="rounded-xl border border-white/10 bg-black/25 p-4 text-sm text-stone-200">{status}</div>
        </section>
      </section>
    </main>
  )
}

export default App