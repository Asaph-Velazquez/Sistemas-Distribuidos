import { useEffect, useMemo, useRef, useState } from 'react'

function App() {
  const wsRef = useRef(null)
  const [connection, setConnection] = useState('Desconectado')
  const [status, setStatus] = useState('Listo para iniciar una partida.')
  const [playerName, setPlayerName] = useState('')
  const [gameName, setGameName] = useState('Partida Oscura')
  const [maxPlayers, setMaxPlayers] = useState(4)
  const [games, setGames] = useState([])
  const [joinPort, setJoinPort] = useState('')
  const [currentGamePort, setCurrentGamePort] = useState(null)
  const [gameState, setGameState] = useState(null)
  const [selectedEnemyKey, setSelectedEnemyKey] = useState(null)

  const players = gameState?.players ?? {}
  const enemies = gameState?.enemies ?? {}
  const me = useMemo(
    () => Object.values(players).find((p) => p.name === playerName),
    [players, playerName],
  )

  const nearbyEnemies = useMemo(() => {
    if (!me) return []
    return Object.entries(enemies).filter(([, e]) => Math.abs(e.x - me.x) + Math.abs(e.y - me.y) === 1)
  }, [enemies, me])

  const playersByPos = useMemo(() => {
    const map = {}
    for (const [id, p] of Object.entries(players)) {
      map[`${p.x},${p.y}`] = { ...p, id, isMe: p.name === playerName }
    }
    return map
  }, [players, playerName])

  const enemiesByPos = useMemo(() => {
    const map = {}
    for (const [key, e] of Object.entries(enemies)) {
      map[`${e.x},${e.y}`] = { ...e, key }
    }
    return map
  }, [enemies])

  useEffect(() => {
    connect()
    refreshGames()

    return () => {
      const ws = wsRef.current
      if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        ws.close()
      }
    }
  }, [])

  useEffect(() => {
    const movementHandler = (event) => {
      if (!currentGamePort) return
      const key = event.key.toLowerCase()
      if (['w', 'a', 's', 'd'].includes(key)) {
        event.preventDefault()
        send({ type: 'move', direction: key })
      }
    }

    window.addEventListener('keydown', movementHandler)
    return () => {
      window.removeEventListener('keydown', movementHandler)
    }
  }, [currentGamePort])

  useEffect(() => {
    if (currentGamePort) {
      send({ type: 'getState' })
    }
  }, [currentGamePort])

  useEffect(() => {
    if (nearbyEnemies.length === 0) {
      setSelectedEnemyKey(null)
      return
    }

    const selectedStillExists = nearbyEnemies.some(([key]) => key === selectedEnemyKey)
    if (!selectedStillExists) {
      setSelectedEnemyKey(nearbyEnemies[0][0])
    }
  }, [nearbyEnemies, selectedEnemyKey])

  const connect = () => {
    if (wsRef.current && (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING)) {
      return
    }

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${wsProtocol}//${window.location.host}/ws/game`
    const ws = new WebSocket(wsUrl)
    wsRef.current = ws

    ws.onopen = () => {
      setConnection('Conectado')
      setStatus('Conexion WebSocket activa.')
      if (currentGamePort) {
        send({ type: 'getState' })
      }
    }

    ws.onclose = () => {
      setConnection('Desconectado')
    }

    ws.onerror = () => {
      setConnection('Error')
      setStatus('No se pudo establecer el canal WebSocket.')
    }

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      handleMessage(data)
    }
  }

  const send = (payload) => {
    const ws = wsRef.current
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      setStatus('WebSocket desconectado. Usa Reconectar.')
      return
    }
    ws.send(JSON.stringify(payload))
  }

  const handleMessage = (data) => {
    if (data.type === 'gameState') {
      setGameState(data)
      return
    }

    if (data.type === 'joined') {
      setCurrentGamePort(data.port)
      setStatus(`Te uniste a ${data.gameName || data.port} como ${data.playerName}.`)
      return
    }

    if (data.type === 'combatResult') {
      if (data.result === 'enemyDefeated') {
        setStatus(`Derrotaste a ${data.enemy} con ${data.damageToEnemy} de daño.`)
        return
      }

      if (data.result === 'playerDefeated') {
        setStatus(`${data.enemy} te vencio. Intercambio: ${data.damageToEnemy} / ${data.damageToPlayer}.`)
        return
      }

      setStatus(`Intercambio de golpes: tu ${data.damageToEnemy}, enemigo ${data.damageToPlayer}.`)
      return
    }

    if (data.type === 'error') {
      setStatus(`Error: ${data.message}`)
    }
  }

  const refreshGames = async () => {
    try {
      const response = await fetch('/api/games')
      const data = await response.json()
      setGames(data.games || [])
    } catch {
      setStatus('No se pudo cargar la lista de partidas.')
    }
  }

  const createGame = async () => {
    if (!playerName.trim() || !gameName.trim()) {
      setStatus('Ingresa nombre de jugador y nombre de partida.')
      return
    }

    try {
      const response = await fetch('/api/games/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: playerName.trim(),
          gameName: gameName.trim(),
          maxPlayers: Number(maxPlayers),
        }),
      })

      const data = await response.json()
      setCurrentGamePort(data.port)
      setStatus(`Partida ${data.gameName} creada en puerto ${data.port}.`)
      send({ type: 'join', port: data.port, playerName: playerName.trim() })
      refreshGames()
    } catch {
      setStatus('No se pudo crear la partida.')
    }
  }

  const joinGame = async () => {
    if (!playerName.trim() || !joinPort) {
      setStatus('Completa nombre y selecciona puerto.')
      return
    }

    try {
      const response = await fetch(`/api/games/${joinPort}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerName: playerName.trim() }),
      })

      if (!response.ok) {
        setStatus('No fue posible unirse a la partida.')
        return
      }

      const data = await response.json()
      setCurrentGamePort(data.port)
      send({ type: 'join', port: Number(joinPort), playerName: playerName.trim() })
    } catch {
      setStatus('Error al unirse a la partida.')
    }
  }

  const attackSelectedEnemy = () => {
    const target = selectedEnemyKey ? enemies[selectedEnemyKey] : null
    if (target) {
      send({ type: 'attack', targetX: target.x, targetY: target.y })
      return
    }
    send({ type: 'attack' })
  }

  const renderBoard = () => {
    if (!gameState?.world) return null

    const rows = []
    for (let y = 0; y < gameState.world.height; y += 1) {
      const rowText = gameState.world.map?.[`row${y}`] || ''
      const rowCells = []

      for (let x = 0; x < rowText.length; x += 1) {
        const tile = rowText[x]
        const player = playersByPos[`${x},${y}`]
        const enemy = enemiesByPos[`${x},${y}`]
        const tileClass = tile === '#' ? 'tile-wall' : 'tile-floor'

        rowCells.push(
          <div key={`${x}-${y}`} className={`board-tile ${tileClass}`}>
            {tile === '#' ? <span className="tile-wall-glyph">▦</span> : <span className="tile-floor-glyph">·</span>}
            {enemy && (
              <div className="unit unit-enemy" title={`${enemy.name} (${enemy.hp}/${enemy.maxHp})`}>
                <span className="unit-icon">👹</span>
                <span className="unit-hp">{enemy.hp}</span>
              </div>
            )}
            {player && (
              <div className={`unit ${player.isMe ? 'unit-me' : 'unit-player'}`} title={`${player.name} (${player.hp}/${player.maxHp})`}>
                <span className="unit-icon">{player.isMe ? '🧙' : '🛡'}</span>
                <span className="unit-hp">{player.hp}</span>
              </div>
            )}
          </div>,
        )
      }

      rows.push(
        <div key={`row-${y}`} className="board-row">
          {rowCells}
        </div>,
      )
    }

    return rows
  }

  return (
    <main className="mx-auto max-w-7xl p-4 md:p-8">
      <header className="mb-6 rounded-2xl border border-white/10 bg-black/20 p-4 backdrop-blur md:p-6">
        <p className="text-xs uppercase tracking-[0.3em] text-amber-300/80">Dungeon Control Panel</p>
        <h1 className="mt-2 text-3xl font-bold text-stone-100 md:text-5xl">Realm Of Shadows</h1>
        <p className="mt-2 text-sm text-stone-300 md:text-base">Estado de red: {connection}</p>
      </header>

      <section className="grid gap-4 lg:grid-cols-[360px_1fr]">
        <aside className="game-card rounded-2xl p-4 md:p-6">
          <h2 className="mb-3 text-xl font-semibold text-stone-100">Lobby</h2>

          <div className="space-y-3">
            <input
              className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100 outline-none focus:border-amber-400"
              placeholder="Nombre del jugador"
              value={playerName}
              onChange={(event) => setPlayerName(event.target.value)}
            />

            <input
              className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100 outline-none focus:border-amber-400"
              placeholder="Nombre de la partida"
              value={gameName}
              onChange={(event) => setGameName(event.target.value)}
            />

            <input
              type="number"
              min={2}
              max={8}
              className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100 outline-none focus:border-amber-400"
              value={maxPlayers}
              onChange={(event) => setMaxPlayers(event.target.value)}
            />

            <div className="grid grid-cols-2 gap-2">
              <button
                className="rounded-lg bg-emerald-700 px-3 py-2 font-semibold text-emerald-50 transition hover:bg-emerald-600"
                onClick={createGame}
              >
                Crear
              </button>
              <button
                className="rounded-lg bg-sky-800 px-3 py-2 font-semibold text-sky-50 transition hover:bg-sky-700"
                onClick={refreshGames}
              >
                Actualizar
              </button>
            </div>
          </div>

          <div className="mt-5 rounded-lg border border-white/10 bg-black/25 p-3">
            <p className="mb-2 text-xs uppercase tracking-[0.2em] text-stone-400">Partidas activas</p>
            <div className="max-h-52 space-y-2 overflow-auto pr-1">
              {games.length === 0 && <p className="text-sm text-stone-500">No hay partidas registradas.</p>}
              {games.map((game) => (
                <button
                  key={game.port}
                  className="w-full rounded-lg border border-white/10 bg-slate-900/60 p-3 text-left transition hover:border-amber-300/40"
                  onClick={() => setJoinPort(String(game.port))}
                >
                  <p className="text-sm font-semibold text-stone-200">{game.gameName}</p>
                  <p className="text-xs text-stone-400">{game.hostName} · {game.currentPlayers}/{game.maxPlayers}</p>
                </button>
              ))}
            </div>

            <div className="mt-3 flex gap-2">
              <input
                className="w-full rounded-lg border border-white/15 bg-black/30 px-3 py-2 text-stone-100 outline-none focus:border-amber-400"
                placeholder="Puerto"
                value={joinPort}
                onChange={(event) => setJoinPort(event.target.value)}
              />
              <button
                className="rounded-lg bg-violet-700 px-4 py-2 font-semibold text-violet-50 transition hover:bg-violet-600"
                onClick={joinGame}
              >
                Unirse
              </button>
            </div>
          </div>

          <button
            className="mt-4 w-full rounded-lg border border-white/20 bg-black/20 px-3 py-2 text-sm text-stone-200 transition hover:bg-black/40"
            onClick={connect}
          >
            Reconectar WebSocket
          </button>
        </aside>

        <section className="space-y-4">
          <div className="game-card rounded-2xl p-4 md:p-6">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-xl font-semibold text-stone-100">Arena</h2>
              <p className="text-xs text-stone-400">Puerto actual: {currentGamePort || '-'}</p>
            </div>

            <div className="overflow-auto rounded-xl border border-white/10 bg-black/45 p-3">
              {renderBoard() || <p className="text-stone-500">Esperando estado del juego...</p>}
            </div>

            <p className="mt-3 text-xs text-stone-400">Usa W A S D para moverte.</p>
          </div>

          <div className="grid gap-4 xl:grid-cols-2">
            <div className="game-card rounded-2xl p-4 md:p-6">
              <h3 className="mb-3 text-lg font-semibold text-stone-100">Jugadores</h3>
              <div className="space-y-2">
                {Object.entries(players).length === 0 && <p className="text-sm text-stone-500">Sin jugadores visibles.</p>}
                {Object.entries(players).map(([id, p]) => (
                  <div key={id} className="rounded-lg border border-white/10 bg-black/30 p-2 text-sm">
                    <p className="text-stone-200">{p.name} {p.name === playerName ? '(tu)' : ''}</p>
                    <p className="text-stone-400">HP {p.hp}/{p.maxHp} · Pos {p.x},{p.y}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="game-card rounded-2xl border-rose-400/20 p-4 md:p-6">
              <h3 className="mb-3 text-lg font-semibold text-rose-100">Combate</h3>
              {me ? (
                <p className="mb-3 text-sm text-rose-100/90">Tu vida: {me.hp}/{me.maxHp}</p>
              ) : (
                <p className="mb-3 text-sm text-stone-500">Aun no hay jugador asociado a esta sesion.</p>
              )}

              {nearbyEnemies.length === 0 ? (
                <p className="text-sm text-stone-500">No hay enemigos adyacentes para combatir.</p>
              ) : (
                <>
                  <div className="mb-3 flex flex-wrap gap-2">
                    {nearbyEnemies.map(([key, enemy]) => (
                      <button
                        key={key}
                        className={`rounded-md border px-3 py-1 text-sm transition ${
                          key === selectedEnemyKey
                            ? 'border-rose-200 bg-rose-600 text-rose-50'
                            : 'border-white/15 bg-black/25 text-stone-300 hover:border-rose-300/70'
                        }`}
                        onClick={() => setSelectedEnemyKey(key)}
                      >
                        {enemy.name} ({enemy.hp}/{enemy.maxHp})
                      </button>
                    ))}
                  </div>

                  <button
                    className="w-full rounded-lg bg-rose-700 px-3 py-2 font-semibold text-rose-50 transition hover:bg-rose-600"
                    onClick={attackSelectedEnemy}
                  >
                    Atacar objetivo
                  </button>
                </>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-white/10 bg-black/25 p-4 text-sm text-stone-200">{status}</div>
        </section>
      </section>
    </main>
  )
}

export default App
