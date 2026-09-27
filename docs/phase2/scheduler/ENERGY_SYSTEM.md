# Energy System

Clases `energy.{EnergyState, EnergyModel, EnergyNeed}`. Cinco magnitudes 0–100: **energía** (90), **fatiga** (10), **foco** (80), **estrés** (0), **motivación** (60).

## Modelo (por tick transcurrido)

- Pasivo despierto: energía −0.002, fatiga +0.001; estrés −0.005, foco +0.003, motivación deriva a 50 (0.0005).
- Rutina en curso: suma sus deltas (perfil). Un NPC diligente se cansa menos (`workRate`).
- Dormir (perfil restaurador con fatiga ≤ −0.02) **no** gasta energía pasiva.
- Se integra por **tiempo transcurrido**: un NPC evaluado raramente no pierde nada (probado: 100 pasos de 10 ticks = 1 de 1000).
- El miedo/ansiedad añaden estrés.

Un día entero despierto lleva la fatiga por encima de 75 (probado).

## Necesidades (`EnergyNeed` → rutina que las cubre)

`SLEEP←fatiga`, `REST←fatiga`, `REFUEL←energía baja (EAT)`, `CALM_DOWN←estrés (MEDITATE)`, `MOTIVATE←motivación baja (SOCIAL)`. Urgencia 0–100; ≥ 40 crea un candidato PERSONAL. Umbrales: `sleepNeedFatigue`=75, `restNeedFatigue`=55, `lowEnergy`=25, `highStress`=70.

## Efectos

El cansancio baja las rutinas que gastan energía y las respuestas de investigación (×0.6 si agotado); el estrés alto (×0.8).

**No se persiste** entre reinicios del servidor (límite conocido): al reaparecer, el NPC empieza descansado.

Pruebas: `beingAwakeWearsAnNpcDownAndSleepRestoresIt`, `elapsedTimeIsWhatCounts…`, `stressCanBeAddedAndDecaysWithTime`, `aWorkingDayWearsAnNpcDownSoItSeeksRestOnItsOwnAccount`.
