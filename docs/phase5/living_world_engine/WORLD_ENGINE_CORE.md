# World Engine — núcleo

**Código:** `living/world/engine/WorldEngine.java`, `WorldSettings.java` (`samuraiai-world.toml`), `WorldPorts.java`, `WorldRuntime.java`.

## Construcción y puertos

`new WorldEngine(settings, bus, clock, seed)`; el hub conecta:

- `WorldPorts.RegionClassifier` — qué tierra hay en una posición. En el juego lo implementa `living/server/BiomeClassifier` (bioma del generador, sin cargar chunks; altura real solo si el chunk ya está cargado). En pruebas, `FIELDS_EVERYWHERE`.
- `WorldPorts.Environment` — estación, actividad animal de la estación, clima de una región y excedente de comida de una región (lo da la economía a través del hub).
- `WorldPorts.Chronicle` — dónde se escribe la historia: la cronología del calendario y sus aniversarios.

## API principal

| Método | Qué hace |
| --- | --- |
| `ensureRegion(dim, x, z)` | crea (una sola vez) la región de esa celda: nombre determinista, tipo, recursos, fauna, vecinos |
| `regionAt`, `region`, `regions` | consultas |
| `foundSettlement(nombre, tipo, dim, x, y, z, radio, origen)` | funda un asentamiento (o devuelve el activo que ya contiene ese punto), lo conecta a la red de caminos, lo anota en la cronología y registra su aniversario |
| `buildRoad`, `blockRoad`, `reopenRoad`, `route` | red de caminos |
| `extract(región, recurso, cantidad)`, `available` | saca recursos de un depósito (origen de la economía) |
| `harvestAnimals(región, recurso, cantidad, cuotaMáx)` | caza/pesca/ganadería de la fauna |
| `scheduleEvent(...)`, `resolveEvent(...)` | eventos de mundo |
| `daily(día)` | eventos espontáneos que salen del estado del mundo |
| `tick(viewers)` | streaming, eventos y simulación de regiones |
| `reportPopulation(asentamiento, n, profesiones)` | la aldea informa de su población |
| `runtime(...)` | `WorldRuntime`: regiones por LOD, eventos abiertos, NPCs activos, regiones abstractas, referencias del calendario/clima/economía, historia, pasos de simulación, asentamientos, caminos, población |

## Configuración

`regionCellSize` (512 bloques por región), radios de LOD, pasos de simulación, presupuesto, caminos (`roadsPerSettlement` 2, `maxRoadLength` 3000, `roadTortuosity` 1,25), probabilidades de eventos espontáneos, archivo de eventos, registro de población, `maxInteractionOrders`, datos (`regions`, `wildlife`, `professions`). Detalle por tema en cada documento.

## Pruebas

`src/test/java/yadi/samuraiai/living/world/WorldEngineTest.java` (9 pruebas).
