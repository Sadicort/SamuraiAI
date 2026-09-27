# World Timeline Engine — la cronología oficial

**Código:** `living/calendar/timeline/WorldTimeline.java`, `TimelineEntry.java`, `TimelineCategory.java`.

## Qué es

La **única cronología objetiva** de Deiliora: qué pasó y cuándo. Cada `TimelineEntry` tiene minuto, categoría, título, detalle, **ámbitos** (`region:<id>`, `settlement:<id>`, `village:<id>`, `family:<id>`, `npc:<uuid>`, `player:<uuid>`, `lineage:<id>`, `festival:<id>`, `crop:<id>`…), significancia 0..1 y procedencia (`Provenance`).

La historia de una aldea o de una familia **no es una segunda copia**: es una consulta `of(ámbito, límite)` sobre esta cronología, resuelta con un índice por ámbito (nunca un barrido de todo).

## Relación con Knowledge

La historia de las comunidades de Knowledge (`SocietyEngine`) sigue siendo lo que la comunidad **recuerda** (con testigos, olvido y leyenda). El hub copia a la comunidad solo los hechos públicos significativos (`Outside.rememberInCommunity`), como testigo; la cronología objetiva sigue siendo esta.

## Quién escribe

Calendario (festivales, Año Nuevo, cosechas notables), World (fundaciones, eventos de mundo, caminos), Village (construcciones, ataques rechazados, llegadas notables, a través de su `Chronicle`), Quest (misiones completadas o fallidas relevantes), Family (nacimientos, muertes, sucesiones, legados). Cada entrada lleva su causa.

## Recorte

Por encima de `timelineMax` (20000) entradas se descartan las **menos significativas y más antiguas**; las de significancia ≥ `timelineKeepSignificance` (0,7) no se descartan nunca. `dropped()` cuenta lo descartado. `TimelineRecordedEvent` al anotar.

## Consultas

`of(ámbito, límite)`, `between(desde, hasta, filtro, límite)`, `latest(límite)`, `scopes()`. Comando: `/samuraiai living calendar timeline`.

## Pruebas

`timelineIsIndexedByScopeAndTrimmedBySignificance`, `everythingSurvivesARestart`.
