# Obstacle Engine

Clases: `obstacles.ObstacleScanner`, `Obstacle`, `ObstacleType`, `TemporaryObstacleTracker`, `ObstacleDecision`, `EntityObstacleSource`, `EntityObstacleInfo`, `EntityKind`; adaptador `world.MinecraftEntityObstacleSource`.

## Dos clases de obstáculo

- **Estáticos** (bloques, muros, puertas, agua, lava, cactus, fuego): un nodo de la ruta dejó de ser transitable. `NavigationEngine.verify` los detecta con `PathValidator`
  (refrescando los próximos nodos y muestreando las celdas de los tramos rectos) y **recalcula localmente** (`detourResume`): busca desde la posición actual hasta el primer
  nodo válido posterior y lo empalma con el resto de la ruta, sin rehacer todo el camino. Si el desvío falla, replanifica completo.
- **Temporales** (jugadores, NPC, hostiles, animales): `ObstacleScanner.temporaryAhead` muestrea la **polilínea** de la ruta hasta `entityLookaheadBlocks`
  (no solo los nodos: tras el suavizado un tramo recto tiene dos nodos). `TemporaryObstacleTracker` decide: `WAIT` (el caminante se detiene) hasta `tempObstacleWaitTicks`, luego `DETOUR` (desvío local
  evitando las celdas del obstáculo). Un obstáculo que se aparta libera al caminante sin recalcular.

## Cuerpo propio excluido

El escáner excluye `MovementBody.entityId()` (UUID de la **entidad**), no el UUID de identidad del NPC. Un primer servidor real mostró que, al excluir el equivocado,
cada NPC se detectaba a sí mismo y agotaba sus recálculos. Cubierto por `aWalkerNeverTreatsItsOwnEntityAsAnObstacle`.

## Rendimiento

`MinecraftEntityObstacleSource` agrupa consultas en celdas de 4 bloques y las cachea durante el tick: muchos caminantes cercanos comparten una consulta al mundo.
