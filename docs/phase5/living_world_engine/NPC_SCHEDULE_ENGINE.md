# NPC Schedule Engine — el mundo vivo y el Behavior Scheduler

**Código:** `ai/scheduler/engine/RoutineBiasSource.java`, `BehaviorScheduler.registerBiasSource/externalBias`, `NpcScheduler.Environment.externalBias`, `routine/RoutinePlanner.Input.external`; fuente `living/server/LivingService.bias` → `LivingWorld.routineBias` → `VillageEngine.routineBias`.

## Principio

El **Behavior Scheduler sigue decidiendo**. El mundo vivo no mueve NPCs ni sustituye rutinas: solo añade puntos a las rutinas (`WAKE, PATROL, WORK, REST, EAT, MEDITATE, SLEEP, SOCIAL, TRAINING, PRAYER, GUARD, MERCHANT`) que el planificador suma a su puntuación junto al estilo de vida, la personalidad, la energía, el ánimo y los eventos.

## Contrato

```java
@FunctionalInterface
public interface RoutineBiasSource { Map<RoutineType, Double> bias(UUID npc); }

scheduler.registerBiasSource("living", source);   // LivingService.install
scheduler.unregisterBiasSource("living");        // LivingService.shutdown
```

- `externalBias(npc)` suma todas las fuentes y acota cada rutina a ±80 (`MAX_EXTERNAL_BIAS`); una fuente que lanza una excepción se salta y cuenta como fallo del scheduler.
- `RoutinePlanner`: `score = peso·(1 + influencia_personalidad·(afinidad − 1)) + calendario + ánimo + externo`; una rutina con peso 0 puede aparecer si el sesgo externo es positivo; la razón del candidato muestra `life +N` (visible en los inspectores del scheduler).

## Qué contiene el sesgo de la aldea (`VillageEngine.routineBias`)

1. La rutina **planificada** para esta hora del plan del día del ciudadano (`scheduleBias`, 35 puntos) — `DAILY_ACTIVITY_ENGINE.md`.
2. El sesgo de su **profesión** (`ProfessionDef.bias` × `professionBiasScale`) mientras el plan dice que trabaja.
3. **Guardias**: turno de guardia o fuera de turno (`GuardEngine`); **templo**: ritos según la fase del día (`TempleLifeEngine`, más fuerte para monjes).
4. Los **eventos de aldea** activos (festival, mercado especial, emergencia, ataque…) × `eventBiasScale` (y su sesgo propio para guardias).
5. **Seguridad**: en peligro, los civiles prefieren quedarse en casa (REST +30, SOCIAL −20, WORK −15).
6. **Clima**: si el clima reduce el trabajo exterior (`outdoorFactor` < 1), los oficios de exterior y los guardias pierden `weatherPenalty`·(1 − factor) en WORK/PATROL y ganan algo de REST.
7. **Frío** por debajo de `coldThreshold`: REST +10, SOCIAL −8.
8. **Vecinos**: si es seguro reunirse, atracción social hacia los vecinos de casa que están en SOCIAL (`neighbourSocialBias`).
9. **Edad** (Family): los niños no trabajan (WORK −100, SOCIAL +20); los ancianos trabajan menos.

Las razones quedan en `Bias.reasons()` (`/samuraiai living village citizen <npc>`).

## Coste

Una consulta por evaluación del scheduler (que ya está acotada por sus propios buckets). El plan del día se cachea por ciudadano y día; `VillageMetrics.biasQueries` cuenta las consultas.

## Pruebas

`ExternalBiasTest` (suma, recorte, fallos, razón «life»), `VillageEngineTest.theVillageBiasesTheSchedulerAndRespondsToAttacksAndRain`, GameTest `anNpcBecomesACitizenWithABedAFamilyAndATimetable`.
