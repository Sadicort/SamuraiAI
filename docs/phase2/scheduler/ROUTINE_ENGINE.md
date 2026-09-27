# Routine Engine

Clases: `routine.RoutineType`, `RoutineProfile`, `RoutineProfiles`, `RoutinePlanner`, `RoutineInstance`.

## Las 12 rutinas

`WAKE, PATROL, WORK, REST, EAT, MEDITATE, SLEEP, SOCIAL, TRAINING, PRAYER, GUARD, MERCHANT`. Cada una tiene un tipo de zona (`ZoneKind`: HOME, PATROL_ROUTE, WORK, REST_AREA, DINING, TEMPLE, HOME, PLAZA, TRAINING, TEMPLE, GUARD_POST, MARKET).

## Perfil (`RoutineProfile`, datos con override por configuración)

| Rutina | Duración (ticks) | Política | Cooldown | Carga social |
| --- | --- | --- | --- | --- |
| WAKE | 60–200 | CANCEL | 2400 | 0 |
| PATROL | 1200–4800 | PAUSE | 0 | 0 |
| WORK | 2400–7200 | PAUSE | 0 | 0.1 |
| REST | 600–2400 | RESUME | 600 | 0 |
| EAT | 400–1200 | RESTART | 6000 | 0.3 |
| MEDITATE | 600–2400 | CANCEL | 3000 | 0 |
| SLEEP | 3000–9600 | SUSPEND | 6000 | 0 |
| SOCIAL | 600–2400 | PAUSE | 600 | 1.0 |
| TRAINING | 1200–3600 | PAUSE | 1200 | 0.2 |
| PRAYER | 400–1200 | CANCEL | 3600 | 0 |
| GUARD | 2400–9600 | PAUSE | 0 | 0.1 |
| MERCHANT | 2400–7200 | PAUSE | 0 | 0.6 |

Además cada perfil fija cuánto **gasta o repone por tick** de energía, fatiga, foco, estrés y motivación (ver [ENERGY_SYSTEM](ENERGY_SYSTEM.md)). Override: `SLEEP;minTicks=3000;maxTicks=9600;policy=SUSPEND;energy=0.03;fatigue=-0.03` (los errores se reportan en `problems()`, no lanzan).

## Planificador (`RoutinePlanner.plan`)

Para cada rutina con peso > 0 (o sesgo positivo de calendario/ánimo), si no está en cooldown y tiene lugar:

`score = peso(periodo) × escala × (1 + influencia_personalidad × (afinidad − 1)) + calendario + ánimo`

- cansancio: las rutinas que gastan energía se multiplican por `1 − 0.6·necesidad_de_descanso` (≥ 0.15); las restauradoras suben con la necesidad;
- `+stickiness` (10) si ya se está haciendo (evita vaivén);
- además genera candidatos de **necesidad** (capa PERSONAL, urgencia ≥ 40) y de **ánimo** (PERSONAL, sesgo ≥ 20).

Cada candidato lleva su **razón** en texto (`AFTERNOON weight 50, personality x1.23, already doing it`) que muestra el inspector.

## Ciclo de vida (`RoutineInstance`)

`TRAVELLING → ACTIVE → COMPLETED/CANCELLED`, con `PAUSED/SUSPENDED` por interrupción. Solo cuenta (y gasta/repone) cuando el NPC **está en su lugar** (`arrivalTolerance`=2.5); si algo lo aparta, vuelve a TRAVELLING. La patrulla cuenta mientras camina y, al llegar a un punto, **avanza al siguiente** (`patrolPoints`=6). Una rutina que no se alcanza en `max(1200, maxTicks)` se abandona con cooldown (`unreachable`); el Brain también puede avisar (`SchedulerService.unreachable`). Duración: entre mínimo y máximo, mayor para personalidades pacientes, estable por NPC.
