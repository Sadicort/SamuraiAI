# Fase 2 — Flujo en ejecución

Bucle completo **Percepción → Brain → Scheduler → Behavior → Navegación → Movimiento → Mundo**, por tick de servidor (fase END).

1. `PerceptionEvents.onServerTick` → `PerceptionService.tick`: NPC vencidos (según tier y presupuesto) hacen un pase; se publican eventos; se actualiza el `DangerMap`.
2. `SchedulerEvents.onServerTick` → `SchedulerService.tick`: reloj del Overworld → `BehaviorScheduler.tick` (evaluaciones por bucket/cuota/presupuesto) → temperamentos a percepción y navegación. Un NPC cuyo nivel de amenaza acaba de subir, o que empieza a tener un objetivo de investigación, se **fuerza** a reevaluar.
3. `NPCTickService.onServerTick`: cada `brainTickInterval` (escalonado) un NPC piensa: `synchronize` → `perceive` (+ `withAdvice`) → eventos de jugador → `Brain.tick`.
4. `DefaultBrain.tick`: `UtilityDecisionEngine.decide` sobre las candidatas (con evidencia y agenda) → si cambia el objetivo cancela tareas → `Behavior.plan` → `Task.tick`.
5. `RoutineTask` / `NavigateTask` → `NavigationService.navigate` (sesión).
6. `NavigationEvents.onServerTick` → `NavigationRuntime.tick`: A* reanudable con presupuesto, verificación, seguimiento; `MovementController` dirige el cuerpo **cada tick** (un `MoveControl` de vanilla solo honra el destino un tick; los NPC lejanos reaplican la última orden).
7. El cuerpo se mueve en el mundo; el siguiente pase de percepción y la siguiente evaluación del scheduler lo ven.

## Ejemplo verificado en servidor real (CustomNPCs)

Samurái en patrulla (tarde) → una rotura de bloque a 10 bloques → `SoundHeardEvent`, sospecha, objetivo de investigación → forzado el scheduler → `RoutineInterruptedEvent(PATROL, INVESTIGATE, PAUSE)` → advice `INVESTIGATE` → objetivo `INVESTIGATE` → `RoutineTask` → sesión de navegación → el samurái camina ≥ 6 bloques hacia el ruido → el ruido se desvanece → `RoutineCompletedEvent(INVESTIGATE, "trigger gone")` → `RoutineStartedEvent("resumed after INVESTIGATE (PAUSE)")` → vuelve a patrullar.
