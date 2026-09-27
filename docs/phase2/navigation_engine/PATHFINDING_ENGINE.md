# Pathfinding Engine

Clases: `pathfinding.PathfindingEngine`, `PathSearch`, `NavigationPath`, `PathNode`, `PathSmoother`, `PathValidator`, `PathValidation`, `PathState`.

## A\* reanudable con presupuesto

`PathSearch.advance(n)` expande como máximo `n` nodos y devuelve `IN_PROGRESS | FOUND | NO_PATH | LIMIT`. El estado (abiertos, cerrados, mejor
coste) vive en la instancia, así que una búsqueda larga se reparte entre ticks. El runtime reparte `searchNodesPerTick` entre las sesiones
(round-robin). Cota: `maxSearchNodes` y una elipse alrededor del segmento inicio→meta (`maxSearchRadius`).

- Heurística: octil horizontal + 0,5·|Δy|. Admisible porque todos los multiplicadores de coste son ≥ 1 (`TerrainCostTable` y `PathPreferences` los fuerzan).
- Si no hay camino, `result()` devuelve el nodo alcanzable más cercano a la meta (ruta **parcial**); solo se acepta con `allowPartial`.
- Nodos bloqueados por recuperación (`blocked`) son infranqueables en esa búsqueda; **nunca** se bloquea la meta.

## Estados (`PathState`)

`REQUESTED → BUILDING → READY → RUNNING ⇄ BLOCKED / RECALCULATING → COMPLETED | FAILED | CANCELLED`. `canMoveTo` rechaza transiciones ilegales
(un camino terminado no revive).

## Suavizado (`PathSmoother`)

Elimina la escalera de la rejilla en tramos planos, pero **conserva** todo nodo con escalón, salto, caída, puerta, escalera de mano, puente,
agua o precipicio. Cada muestra del atajo debe cumplir `NavigationGraph.footprintStandable`: las cuatro esquinas del cuerpo
(radio = ancho real/2 + margen) sobre celdas transitables, y sin peligro por encima de los extremos. Antes de esto un atajo rozaba la esquina de
un muro y el NPC se atascaba (hallazgo del servidor real).

## Validación (`PathValidator`)

Revisa los próximos `lookaheadNodes` nodos contra el mundo vivo: estabilidad, chunk cargado, coste no infinito, arista todavía legal, y en
tramos rectos suavizados cada celda de la huella. Devuelve la primera posición inválida y el motivo.

## Fallos

`NO_PATH`/`LIMIT` → `DESTINATION_UNREACHABLE` / `SEARCH_LIMIT` / `CHUNK_UNLOADED` (si el corredor cruza un chunk no cargado). Nunca se lanza excepción al comportamiento.
