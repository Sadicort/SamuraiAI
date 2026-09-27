# Region Engine

**Código:** `living/world/regions/Region.java`, `RegionType.java`, `RegionCatalog.java`, `ResourceDeposit.java`.

## Qué es una región

Una celda del mapa (`regionCellSize` × `regionCellSize` bloques, 512 por defecto) con identidad propia (`Region`, el *Region Runtime*): clave y `scope()` (`region:<uuid>`), dimensión y celda, nombre, tipo, bioma, cultura, altitud, peligro (base + eventos), depósitos de recursos, poblaciones de fauna, asentamientos, relaciones con vecinas (`ADJACENT, ALLIED, TRADE, RIVAL, HOSTILE`), población agregada, contadores de historia (`CONSTRUCTIONS, FIRES, WARS, BATTLES, FESTIVALS, DISASTERS, HEROES, VISITS`), nivel de simulación, último paso simulado y última vez que un jugador estuvo cerca.

## Creación

`ensureRegion` crea la región la primera vez que algo ocurre en ella (primer asentamiento, primer NPC, un camino que la cruza, un jugador que la visita en el comando). El tipo lo decide el `RegionClassifier` (en el juego, el bioma: océano/playa → `COAST`, río → `RIVER`, pantano → `SWAMP`, montaña/colina/picos → `MOUNTAINS`, bosque/taiga/jungla/bambú → `FOREST`, resto → `FIELDS`). El nombre sale de una raíz y un sufijo por tipo, de forma determinista a partir de la clave (la misma región siempre se llama igual). Al crearse:

- recibe sus depósitos y peligro base del catálogo;
- se puebla de fauna según su hábitat;
- el hub registra su celda de clima en el calendario (`trackWeather(scope, microclima del tipo, altitud)`);
- se publica `RegionCreatedEvent`.

`VILLAGE` y `TEMPLE` son tipos de catálogo para tierras habitadas; `RUINS` está preparado (asignable por dato, nada lo genera aún).

## Peligro

`danger = baseDanger (catálogo + fauna peligrosa) + eventDanger (eventos de mundo que corren)`, acotado a [0, 1]. Un cambio de ≥ 0,05 publica `RegionDangerChangedEvent`. El peligro reevalúa solo los caminos que cruzan la región y decide la probabilidad de ataques y bandidos.

## Memoria

Los contadores son la memoria rápida de la región; su historia fechada está en la cronología bajo `region:<uuid>` (ver `WORLD_MEMORY_ENGINE.md`).
