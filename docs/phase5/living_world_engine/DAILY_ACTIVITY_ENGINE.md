# Daily Activity Engine

**Código:** `living/village/schedules/DailyScheduleEngine.java`, `ScheduleTemplate.java`, `DayPlan.java`.

## Plan del día

Para cada ciudadano y día se construye un `DayPlan` (bloques minuto → rutina) a partir de:

- la **plantilla cultural** (`templates` en `samuraiai-village.toml`; la integrada `village` va de despertar a dormir pasando por trabajo, comidas, oración y vida social);
- la **profesión** (`professionSchedules`: desplazamiento de horario y sustituciones, p. ej. el pescador madruga, el guardia de noche cambia SLEEP por GUARD — turno nocturno según `nightWatchShare`, `nightShiftMinutes`);
- el **amanecer** del día (`SunModel.sunriseShift`: en invierno se empieza más tarde);
- si es **día festivo**;
- el **ritmo personal** (`scheduleOffset` del ciudadano, ±90 min: de la diligencia cognitiva del NPC más un desfase determinista), para que no todos hagan lo mismo al mismo minuto.

`DayPlan.routineAt(minuto)` da la rutina planificada; `adjustments` explica cada ajuste («ritmo propio −12 min», «turno de noche»…). Los planes se cachean por (ciudadano, día).

## Dos usos del mismo plan

1. **Cerca de jugadores (LOD 0):** el plan es solo el sesgo `scheduleBias` que llega al Behavior Scheduler (`NPC_SCHEDULE_ENGINE.md`); el NPC real puede hacer otra cosa si su ánimo, energía o un evento lo piden.
2. **Simulación abstracta:** `plannedWorkHours(npc, desde, hasta)` da las horas de trabajo planificadas, que se acreditan como horas de profesión (producción económica y experiencia) sin entidades.

Nunca todas las rutinas son idénticas: difieren por profesión, estación, festival y persona (prueba `schedulesDifferByProfessionSeasonFestivalAndPerson`).
