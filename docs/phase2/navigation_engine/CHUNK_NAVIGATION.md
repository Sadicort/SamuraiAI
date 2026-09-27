# Chunk Navigation

Clases: `chunks.ChunkLoadGuard`, `ChunkRoutePlanner`, `ChunkRoute`, `ChunkPathIndex`, `ChunkWaitPolicy`.

## Consciencia de carga

- La lectura del mundo usa `getChunkNow`: navegación **nunca** carga chunks. Un chunk no cargado es infranqueable (`UNLOADED`) y `DestinationValidator` rechaza metas en él.
- `ChunkRoutePlanner.route(inicio, meta)` recorre la línea en pasos de 8 bloques y devuelve los chunks cruzados y el primer no cargado. Sirve para explicar un fallo (`CHUNK_UNLOADED` vs `UNREACHABLE`).
- `ChunkPathIndex`: qué sesiones cruzan qué chunk. Una descarga o un cambio de bloque marca esas sesiones para reverificar de inmediato (`lastVerifyTick = -1`).

## Chunk que desaparece con el NPC en camino

`verify` detecta `CHUNK_UNLOADED` en los próximos nodos → `BLOCKED(reason="chunk-unloaded")`, el cuerpo se detiene y se espera. Al volver a cargarse (`chunksReady`) la sesión se reanuda;
si pasa `chunkWaitTicks` (200 por defecto) falla con `CHUNK_UNLOADED`. Verificado: `unloadedChunkAheadBlocksThenResumesWhenItLoads`, `chunkThatNeverLoadsEndsWithChunkUnloadedFailure`.

## Rutas parciales entre chunks

Con `allowPartial`, una búsqueda que no llega a la meta devuelve el nodo más cercano; al terminarla se replanifica (`partial-end`). Si el último tramo no avanzó ≥ `partialMinGain` bloques, falla
(`no progress past …`) en lugar de girar en círculos.

## NPC dormidos (preparado)

`ChunkWaitPolicy` (esperar / recalcular / fallar) y el frecuencia adaptativa del runtime (`farUpdateInterval`) son la base que la Fase 2.4 usará para los buckets de hibernación.
