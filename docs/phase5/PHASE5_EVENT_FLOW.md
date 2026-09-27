# Fase 5 — Flujo de eventos

Todos los eventos de la Fase 5 son `LivingEvent` (`domain()`, `minute()`) y pasan **por el hub antes de salir al bus**:

```
motor ── publish ──► LivingWorld.dispatch ──► LivingReactions.handle (reacciones entre motores, profundidad ≤ 24)
                                  └──────────► NPCEventBus (adaptador, cognición, depuración, log)
```

Una reacción que falla se cuenta (`reactionFailures`, `lastError`) y no detiene al resto; una cadena de reacciones más profunda que 24 se corta (`droppedReactions`).

## Reacciones del hub (`LivingReactions`)

| Evento | Reacción |
| --- | --- |
| `RegionCreatedEvent` | el calendario empieza a seguir el clima de la región |
| `SettlementFoundedEvent` | se crea la aldea (con plano si se pidió) y su economía |
| `DayChangedEvent` | se encola el trabajo del día |
| `HolidayEvent` (ancestros) | las familias honran a sus antepasados |
| `HarvestOutlookEvent` | la economía aplica el rendimiento; gran cosecha → celebración |
| `WorldEventPhaseEvent` | consecuencias en aldeas, economía, misiones (`../living_world_engine/WORLD_EVENT_ENGINE.md`) |
| `RoadBlockedEvent` / `RoadReopenedEvent` | rutas comerciales invalidadas / restauradas |
| `ScarcityStartedEvent` / `ScarcityEndedEvent` | misión de escasez o de taller / resuelta por el mundo |
| `CaravanLostEvent` / `CaravanAmbushedEvent` / `CaravanArrivedEvent` | misión / giro en escoltas / visitante, escoltas cumplidas, misión resuelta |
| `TradeRouteDisruptedEvent` / `TradeRouteRestoredEvent` | misión de ruta / resuelta |
| `VillagePopulationChangedEvent` | población del mundo |
| `CitizenJoinedEvent` | familia (y migración si es el jefe), puesto de mercader, registro de población |
| `CitizenLeftEvent` | estado de vida en la familia, registro de población |
| `ProfessionAssignedEvent` | la familia lo anota; mercader → puesto |
| `HousingShortageEvent` | misión de casas |
| `LifeStateChangedEvent` (desaparecido) | misión de búsqueda |
| `BirthRegisteredEvent` | registro de población |

## Entradas desde el juego (`LivingService`)

`NPCActivatedEvent` → ciudadano; `NPCDeactivatedEvent` → se va (o queda sin cuerpo al apagar); `LivingDeathEvent` → muerte real / objetivo de combate; `RoutineCompletedEvent` → horas de oficio; `ThreatDetectedEvent`, `CombatStartedEvent` → amenaza de aldea; conversación → objetivo `TALK` y aviso de misiones; posición de jugadores → llegar, meditar, defender, LOD, HUD; comandos → entregas, comercio, contratos, misiones.

## Salidas hacia el juego

`WeatherChangedEvent` (celda `world`) → cielo vanilla si `driveVanillaWeather`; `QuestCreatedEvent`, `FestivalStartedEvent`, `VillageSecurityChangedEvent`, `VillageEventStartedEvent`, `WorldEventPhaseEvent` → mensajes a jugadores; `LivingEvent` → log si `debugLogging`.
