# Fase 2 — Integración

## Percepción → Brain

`NPCTickService.think` obtiene el `WorldContext` de `PerceptionSystems.current()` (motor nuevo si `PerceptionService.enabled()`, si no la percepción antigua; el motor cae a la antigua antes de su primer snapshot). `UtilityDecisionEngine.evidenceModifier` traduce amenaza/sospecha/conciencia/investigación en puntuaciones. `PerceptionEmotionBridge` (suscrito al EventBus) traduce amenaza/sospecha/interés en miedo, ansiedad y sorpresa.

## Scheduler → Brain

`think` adjunta `SchedulerService.adviceFor(id)` al contexto (`WorldContext.withAdvice`). Después:

- `DefaultBrain.candidates` añade el objetivo aconsejado si la definición no lo lista (`GoalMapper`: 12 rutinas y 4 respuestas ↔ objetivos; se añadieron 10 `GoalType`: WAKE, WORK, EAT, SLEEP, MEDITATE, SOCIAL, TRAINING, PRAYER, GUARD, TRADE).
- `UtilityDecisionEngine.scheduleModifier`: el objetivo aconsejado recibe un bonus por capa (BASELINE +55, PERSONAL +70, SITUATIONAL +75, EMERGENCY +110); dormir/meditar/rezar restan a TALK; **FLEE y COMBAT pierden 50 mientras el scheduler aconseja otra cosa** (su prioridad plana 85/80 hacía que un mercader sin motivo huyera siempre; ahora han de ganárselo con evidencia).
- `RoutineBehavior` (para todos los objetivos de la agenda) y las rutas advice-aware de `PatrolBehavior`, `FleeBehavior`, `ProtectBehavior`, `InvestigateBehavior` devuelven una `RoutineTask`.

## Brain → Navigation

`RoutineTask` y `MoveToTask` piden viajes a `NavigationService` (`NavigateTask`); nunca hay pathfinding en el Brain ni en el scheduler. `RoutineTask` sigue el lugar aconsejado si se mueve (puesto de formación), reporta lugares inalcanzables (`SchedulerService.unreachable` → la rutina se abandona con cooldown) y termina cuando el advice termina.

## Scheduler → Perception y Navigation (personalidad)

`PerceptionService.setSenseProfile` (visión, oído, curiosidad, sospecha, miedo, atención; sube con `STAY_ALERT/SCAN`) y `NavigationService.setPreferenceAdjuster` (peso del peligro × aversión). Ambos son ganchos genéricos: ni percepción ni navegación conocen rasgos ni el scheduler.

## Perception → Navigation

`PerceptionService.collectThreats` registra las amenazas percibidas como `DangerZone` (fuente nombrada); `NavigationService.refreshDanger` al olvidar un NPC para que no queden zonas fantasma.

## CustomNPCs

Solo `integration/customnpcs` importa `noppes.*`: controla el cuerpo (`movementBody`, `ownedEntityIds`, `refreshDimensions`, `setReturnsHome(false)` + `setMovingType(0)` para que su IA no compita). Sin CustomNPCs (controlador de chat) los NPC no tienen cuerpo: navegación falla con `NO_BODY`, el scheduler y el Brain siguen razonando con `advice` y objetivos (probado en ambos perfiles).

## Ciclo de vida

`NPCSpawnService.remove` → `NavigationService.forget`, `PerceptionService.forget`, `SchedulerService.forget` (zonas, grupos, temperamento, hogar). `Samuraiai.starting` instala percepción/puente emocional y carga zonas; `stopping` guarda zonas y resetea los tres servicios.

## Compatibilidad

Todas las APIs existentes se conservan: los métodos nuevos de `NPCController`/`WorldContext`/`NPCInstance` son `default`/aditivos; con el scheduler desactivado (`scheduler.enabled=false`) el Brain se comporta como antes.
