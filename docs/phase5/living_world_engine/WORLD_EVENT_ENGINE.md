# World Event Engine

**Código:** `living/world/events/WorldEventEngine.java`, `WorldEventRecord.java`, `WorldEventType.java`, `WorldEventPhase.java`; eventos de bus `events_api/WorldEventPhaseEvent`.

## Ciclo de vida

`PREPARATION → START → DEVELOPMENT → END → CONSEQUENCES → CLOSED` (archivado). Cola de prioridad: solo se tocan los eventos cuya transición toca. Cada transición publica `WorldEventPhaseEvent` y se entrega a los `Listener`s: el propio mundo aplica peligro regional y bloqueos de caminos; el hub traduce el resto en consecuencias para aldeas, economía, misiones y familias. Dos eventos abiertos del mismo tipo nunca comparten ámbito.

## Tipos

| Tipo | Preparación | Duración | Ámbito | Peligro | Bloquea caminos | Hostil |
| --- | --- | --- | --- | --- | --- | --- |
| FESTIVAL | 1 día | 3 días | asentamiento | 0 | no | no |
| RAIN | 0 | 12 h | región | 0 | no | no |
| STORM | 1 h | 8 h | región | 0,15 | no | no |
| FLOOD | 6 h | 2 días | región | 0,2 | sí | no |
| FIRE | 0 | 6 h | asentamiento | 0,1 | no | no |
| ATTACK | 30 min | 4 h | asentamiento | 0,35 | no | sí |
| BANDITS | 2 h | 5 días | región | 0,4 | no | sí |
| WAR | 3 días | 20 días | región | 0,5 | sí | sí |
| MARKET | 12 h | 1 día | asentamiento | 0 | no | no |
| CELEBRATION | 12 h | 1 día | asentamiento | 0 | no | no |
| DUEL | 4 h | 1 h | asentamiento | 0 | no | no |
| EMERGENCY | 0 | 12 h | asentamiento | 0,2 | no | no |

**Guerra y combate:** no existe en el código un «Battlefield Engine» de una fase de combate anterior. `WAR`, `ATTACK` y `BANDITS` son la forma en que el conflicto armado entra en el mundo vivo; el combate físico real de los NPCs (`CombatStartedEvent`) sube la amenaza de su aldea (`combatThreat`).

## Eventos espontáneos (`daily`)

Salen del **estado del mundo**, con `Dice` determinista:

- **Incendio** por asentamiento: `fireChancePerDay` (0,003) × 2,5 en verano soleado, × 0,3 con lluvia, × 1,5 en campamentos.
- **Ataque** a asentamientos con residentes: `attackChancePerDay` (0,002) × (0,2 + 3·peligro de la región), a una hora entre las 12:00 y las 22:00.
- **Duelo** en aldeas: `duelChancePerDay` (0,002).
- **Gran mercado** periódico cada `specialMarketEveryDays` (30) en asentamientos con mercado.
- **Bandidos** en regiones cruzadas por caminos: `banditChancePerDay` (0,004) × (0,3 + 2·peligro base).
- **Crecida** en ríos, costas y pantanos con tormenta: `floodChanceInStorm` (0,15).

Además: **celebración** por gran cosecha (hub, `celebrateGreatHarvests`), eventos por comando (`/samuraiai living world event <tipo> <severidad>`) y los que abre una misión (`QuestPorts.World.spawnEvent`).

## Consecuencias (hub `LivingReactions.worldEvent`)

- **START:** ataque → la aldea entra en ataque (`attackStarted`) y se abre la condición de misión ATTACK; incendio → pérdidas en el almacén (`economy.fire`), emergencia de aldea, una casa dañada o destruida (severidad > 0,75), memoria comunitaria, misión FIRE; bandidos/guerra → amenaza en las aldeas de la región (20·sev / 45·sev) y misión; mercado → evento de mercado especial en la aldea y en la economía; tormenta → clima forzado en la región; crecida → emergencia y pérdida del 10 %·sev de los almacenes.
- **CONSEQUENCES:** un ataque no defendido termina en saqueo (25 %·sev); uno resuelto suma renombre y memoria («Ataque rechazado»); las misiones de defensa se resuelven (`worldEventEnded`) y se cierran las condiciones del evento.

`resolve(id, éxito, por, resultado)` termina un evento antes de tiempo (una misión cumplida, un comando). Se archivan hasta `eventArchive` (500).

## Pruebas

`resolvingAnEventEndsItEarly`, `theWorldGeneratesEventsFromItsStateDeterministically`, `warBlocksRoadsWhileItRunsAndTheyReopenAfterwards`, `LivingWorldTest.warCutsTradeAndOpensQuests`.
