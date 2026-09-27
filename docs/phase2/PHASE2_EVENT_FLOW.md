# Fase 2 — Flujo de eventos

Todos implementan `NpcEvent` (record inmutable = hecho pasado) y salen por `EventSink` (en producción `NPCEventBus`).

| Motor | Eventos |
| --- | --- |
| Navigation (11) | `PathRequestedEvent`, `PathCreatedEvent`, `PathStartedEvent`, `PathRecalculatedEvent`, `PathBlockedEvent`, `PathCompletedEvent`, `PathFailedEvent`, `PathCancelledEvent`, `NPCStuckEvent`, `DoorOpenedEvent`, `DoorClosedEvent` |
| Perception (8) | `VisionDetectedEvent`, `VisionLostEvent`, `SoundHeardEvent`, `SuspicionRaisedEvent`, `SuspicionClearedEvent`, `InterestDetectedEvent`, `ThreatDetectedEvent`, `AwarenessChangedEvent` |
| Scheduler (10) | `RoutineStartedEvent`, `RoutineCompletedEvent`, `RoutineInterruptedEvent`, `TimelineChangedEvent`, `EmotionPriorityChangedEvent`, `PersonalityUpdatedEvent`, `SchedulerOptimizationEvent`, `GroupLeaderChangedEvent`, `BehaviorConflictResolvedEvent`, `WorldScheduleEvent` |

## Quién publica y quién escucha

- Publican los propios motores (nunca los adaptadores por ellos). Los eventos de mundo (bloque roto, explosión, daño) **no** son estos eventos: son entradas (`PerceptionEvents`, `NavigationEvents`) que los motores convierten en hechos.
- Escuchan: `PerceptionEmotionBridge` (ThreatDetected, SuspicionRaised, InterestDetected → emociones), las pruebas físicas y cualquier módulo futuro. **Ningún motor depende de que alguien escuche.**
- Los eventos de percepción son evidencia, no órdenes; los del scheduler describen decisiones ya tomadas; los de navegación describen el viaje.

## Cadena típica (sonido → investigación)

`SoundHeardEvent` → (emoción: sorpresa) → `SuspicionRaisedEvent` → `RoutineInterruptedEvent` → `RoutineStartedEvent(INVESTIGATE)` → `PathRequested/Created/Started` → `PathCompletedEvent` → `RoutineCompletedEvent` → `RoutineStartedEvent("resumed after …")`.

## Emergencia

`ThreatDetectedEvent`/daño → `EmotionPriorityChangedEvent(→ PANICKED)` → `BehaviorConflictResolvedEvent` (capa EMERGENCY vence) → `RoutineInterruptedEvent(SLEEP, SUSPEND)` → respuesta FLEE.
