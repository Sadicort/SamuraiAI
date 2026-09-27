# Navigation Graph

Clases: `graph.NavigationGraph`, `NavNode`, `NavEdge`, `EdgeType`, `NavPos`, `NavWorldView`, `NavEnvironment`, `NodeAnalyzer` (privada), `EdgeSink`.

## Nodo

Un nodo es la posición del bloque donde están los **pies** (`NavPos`). `NavNode` guarda hechos independientes de quién camina:
`standable`, `deepWater`, `door`, `ironDoor`, `climbable`, `water`, `terrain`, `floor` (material del suelo) y `staticDanger`.
Lo que depende del caminante (puertas de hierro, nadar, vadear) se decide en `NavigationGraph.standable(pos, prefs)`.

Reglas de "cabe aquí" (`NodeAnalyzer`): pies y cabeza ocupables (sin colisión, sin lava, sin daño), suelo que sostiene, o escalera de mano,
o escalón bajo (losa, nieve) que se sostiene a sí mismo (entonces también se exige el bloque +2 libre).

## Aristas (`neighbors`)

| Tipo | Cuándo |
|---|---|
| `WALK`, `DIAGONAL` | mismo nivel; la diagonal exige las dos columnas contiguas libres (no corta esquinas) |
| `STEP` | subir 1 sobre escaleras/losa, sin saltar |
| `JUMP` | subir 1 sobre un bloque completo (no valla), con espacio para saltar |
| `DESCEND` | caída de 1..`maxDrop` bloques; coste creciente, penalización extra desde 3 |
| `CLIMB` | arriba/abajo en una escalera de mano |
| `DOOR` | entrar en un nodo cuya cavidad es una puerta abrible |
| `BRIDGE`, `WADE`, `SWIM` | nodo de puente, agua poco profunda, agua profunda (solo si `allowSwim`) |

## Grafo dinámico

Perezoso: los nodos se calculan al pedirlos y se cachean **por chunk**. `invalidate(pos)` descarta el chunk (y los vecinos si el cambio está a ≤1
del borde); `invalidateChunk`; `refresh(pos)` recalcula un nodo; `liveStandable(pos)` hace una lectura barata (3 bloques) y solo recalcula si la
caché discrepa. Los eventos de Forge (romper/colocar bloque, fluidos, explosiones, carga/descarga de chunk) llaman a esas invalidaciones vía `NavigationService`.
La caché se vacía al superar `nodeCacheMax`.

## Lectura del mundo

`NavWorldView` es la única puerta al mundo. `world.MinecraftNavWorldView` usa `getChunkNow` (nunca fuerza la carga de un chunk; uno no cargado
es `BlockProfile.UNLOADED`) y clasifica cada `BlockState` una sola vez (`Map<BlockState, BlockProfile>`).

## Límites conocidos

- No hay salto en horizontal sobre huecos (`JUMP` solo sube 1).
- Natación vertical no implementada: `allowSwim` permite superficie a nivel constante.
