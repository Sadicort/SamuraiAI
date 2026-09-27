# Behavior Scheduler AAA

Paquete `yadi.samuraiai.ai.scheduler`. Decide **qué quiere hacer cada NPC a lo largo del día** y cómo responde a lo inesperado. Es **consejo**, no acción: el Brain decide, el Behavior planifica, Navigation mueve.

## Jerarquía (mundo → zona → NPC)

| Nivel | Clase | Responsabilidad |
| --- | --- | --- |
| Mundo | `engine.BehaviorScheduler` | reloj (`Timeline`), calendario, buckets de tick, cuota y presupuesto, grupos y zonas, métricas |
| Zona | `zone.ZoneScheduler` | ocupación, capacidad, propiedad, horarios, alertas de zona |
| NPC | `engine.NpcScheduler` + `NpcSchedule` | una evaluación: condición, ánimo, rutina en curso, candidatos, selección, cambio |

## Separación de responsabilidades (comprobada por `ArchitectureRulesTest`)

- El **núcleo** (`ai.scheduler.*` salvo `world/`) no importa Minecraft, Brain, behaviors, tasks, decision, goal, controller, emotion, npc, percepción ni navegación. Solo ve **datos**: `SchedulerInput`, `Perceived`, `EmotionInput`, `Light`.
- El **adaptador** (`ai.scheduler.world`) es lo único que lee `PerceptionService`/emociones/NPCManager y devuelve temperamento a percepción y navegación.
- El scheduler **nunca mueve entidades** ni crea rutas. El Brain lo consume solo por `WorldContext.advice()` y `GoalMapper`.

## Contrato de salida: `SchedulerAdvice`

rutina **o** respuesta, capa de prioridad, estado (`TRAVELLING/ACTIVE/PAUSED/…`), `Place` (destino), `emergency`, puntuación, periodo, ánimo, comportamientos de fondo, motivo, profundidad de la pila de interrupciones, `Temperament`, bucket, grupo/rol/formación.

## Flujo de un tick (`BehaviorScheduler.tick`)

1. `advanceClock`: periodo (`TimelineChangedEvent`) y calendario (`WorldScheduleEvent`).
2. Alta/baja de NPC; bucket por NPC (se refresca cada 20 ticks).
3. `CrowdManager.plan`: quién toca, más atrasado primero, hasta `maxEvaluationsPerTick`, y `budgetMicros` de tiempo.
4. Por cada NPC: `NpcScheduler.evaluate` (un fallo se cuenta y aísla; el resto sigue).
5. Cada `groupSyncTicks`: agrupar, elegir líder, roles, órdenes; cada `zoneScanTicks`: limpieza.
6. Métricas de población.

## Módulos con responsabilidad real

`time` (timeline, calendario), `routine` (perfiles, planificador, instancias), `priority` (4 capas), `interrupt` (pila y políticas), `conflict`, `personality`, `emotion`, `social`, `energy`, `cooldown`, `stack` (multi-comportamiento), `group`, `formation`, `zone`, `lifestyle`, `response`, `optimize`, `metrics`, `debug`, `events`. Cada uno tiene documento propio en esta carpeta o está cubierto en [PRIORITY_ENGINE](PRIORITY_ENGINE.md), [INTERRUPT_SYSTEM](INTERRUPT_SYSTEM.md) y [BEHAVIOR_STACK](BEHAVIOR_STACK.md).

## Configuración (todo configurable, nada de horarios ni personalidades en código)

`samuraiai-scheduler.toml` refleja uno a uno el record `SchedulerSettings` (83 valores, todos acotados por el propio record) más tres catálogos de datos: `lifestyles`, `calendar`, `routineOverrides` (lista vacía = catálogo incorporado). Recarga en caliente: `BehaviorScheduler` reconstruye sus motores al cambiar el snapshot de ajustes (probado).
