# World Streaming Engine — nivel de detalle

**Código:** `living/world/streaming/StreamingEngine.java`, `SimulationLevel.java`.

Cada `streamingIntervalTicks` (40) el motor clasifica cada región según la distancia de los jugadores (`Viewer(dim, x, z)` que el adaptador construye cada tick con los jugadores conectados):

| Nivel | LOD | Condición por defecto | Qué se simula |
| --- | --- | --- | --- |
| `FULL` | 0 | jugador a ≤ `fullRadius` (96) | entidades, Brain y navegación reales; el mundo vivo solo añade sesgo de rutinas e interacciones físicas |
| `ACTIVE` | 1 | ≤ `nearRadius` (320) | aldeas, economía y familias hora a hora |
| `SETTLEMENT` | 2 | ≤ `settlementRadius` (1200) | asentamientos cada pocas horas |
| `ABSTRACT` | 3 | más lejos | agregados económicos y comunitarios una vez al día |
| `HISTORICAL` | 4 | nadie cerca en `historicalAfterDays` (30) días | pasos semanales |

Solo informa de **transiciones**: al despertar una región (`RegionActivatedEvent`) el World Engine la pone al día con pasos acotados (`SIMULATION_ENGINE.md`); al dormirse, `RegionSleepingEvent`. La distancia se mide desde el borde de la región (0 si el jugador está dentro), no desde el centro; una región con un jugador a ≤ `settlementRadius` actualiza su `lastPlayerSeen`.

Consecuencia importante para las pruebas: el servidor de GameTest no tiene jugadores, así que ninguna región llega a `FULL` allí; las acciones físicas que exigen `FULL` (encender fuegos, cuidar cultivos) solo se ejercen con jugadores.

Prueba: `streamingWakesRegionsAndCatchesThemUpInBoundedSteps`, `aRegionAbandonedForMonthsIsCaughtUpWhenAPlayerReturns`.
