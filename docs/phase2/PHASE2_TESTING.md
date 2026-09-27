# Fase 2 — Pruebas

## Unitarias (JUnit, sin Minecraft)

| Área | Clases | Pruebas |
| --- | --- | --- |
| Navigation | `PathfindingTest`, `NavigationRuntimeTest` | 20 + 26 |
| Perception | `VisionTest`, `HearingTest`, `AwarenessTest`, `SensorSystemTest` | 13 + 11 + 20 + 16 |
| Scheduler | `TimelineTest`, `PersonalityEnergyTest`, `PriorityInterruptTest`, `ZoneFormationGroupTest`, `OptimizationTest`, `SchedulerRuntimeTest`, `SchedulerAdapterTest`, `SchedulerPerformanceTest` | 7 + 14 + 12 + 15 + 7 + 25 + 7 + 2 = 89 |
| Arquitectura | `ArchitectureRulesTest` | 7 |

Bancos de prueba: `NavHarness`/`GridWorldView`/`SimulatedBody` (un cuerpo cuyo destino solo vale un tick, como el de vanilla), `PerceptionHarness`/`GridPerceptionWorld`, `SchedulerHarness` (aldea simulada donde un sustituto del Brain+Navigation camina hacia el lugar aconsejado, cerrando el bucle sin Minecraft).

## Físicas (GameTest en servidor real; en ambos perfiles, con y sin CustomNPCs)

17 pruebas: 1 de ciclo de vida (previa), 7 de percepción, 4 de navegación y 5 del scheduler.

| Escenario exigido | Prueba física | Unitaria equivalente |
| --- | --- | --- |
| patrulla → sonido → investigar → volver | `aPatrolBrokenBySoundInvestigatesAndReturns` | `aPatrolInterruptedBySoundInvestigatesItAndThenReturnsToThePatrol` |
| emergencia | `fearOverridesTheNightsSleep` | `anEmergencyOverridesEvenASleepingNpc…`, `aFighterAnswersDanger…` |
| grupo / formación | `guardsPatrolAsAGroupAndReelectALeader` | `guardsFormAGroup…`, `whenTheLeaderIsRemoved…`, `oneGuardRaisingTheAlarm…` |
| línea temporal | `theTimelineAnnouncesPeriodsAndCommandsRespond`, `theClockShapesTheDayOfAMerchant` | `aVillagerLivesAWholeDay…`, `theCalendarAnnounces…` |
| visión → comportamiento | `seesAHostileAndTreatsItAsAThreat`, `aFarSoundMakesTheSamuraiInvestigate` | `AwarenessTest`, `VisionTest` |

Con CustomNPCs los NPC **caminan de verdad** (al puesto, hacia el ruido, en formación, por puertas, subiendo escalones); sin él se comprueban advice y objetivos y navegación falla con `NO_BODY` como debe.

## Regresiones descubiertas por pruebas físicas

Ver los registros `PHASE2_2/2_3/2_4_CHANGELOG`. Ejemplos del scheduler: `bucketTick`/`lastGroupSync` con `Long.MIN_VALUE` desbordaban la resta (los grupos y los buckets nunca funcionaban); FLEE 85 plano vencía a toda la agenda; una respuesta que perdía su disparador se cortaba sin respetar el margen de espera; hibernación por falta de jugadores.

## Estabilidad de las pruebas físicas (lo aprendido)

Las pruebas físicas fallaban por causas del **entorno de prueba**, no del producto, y se corrigieron en las propias pruebas:

- El mundo de GameTest es **terreno natural**: un suelo desnudo puede quedar sobre cuevas o **lava**, que percepción escanea a pocos bloques y navegación trata como peligro. `TestTerrain.flatten` reemplaza una caja amplia (10 bloques de margen, piedra maciza debajo, aire encima) y mantiene los chunks cargados.
- Cada prueba usa un trozo de mundo **propio y lejano** (arenas cercanas se pisaban las paredes: la prueba de la puerta esquivaba la puerta).
- Los bloques puestos con `setBlock` no lanzan eventos de Forge: la prueba avisa a navegación (`chunkChanged`) para invalidar su caché.
- Sin jugadores todos los NPC quedan en HIBERNATING (600 ticks): las pruebas del scheduler suben los intervalos de bucket.
- Los ruidos breves solo se oyen si el sensor escanea en su ventana: se repiten hasta oírlos, con un sonido **inofensivo** (repetir explosiones es, con razón, peligro).
- Las pruebas del scheduler congelan y **restauran** el reloj y la regla `doDaylightCycle` (dejarla en marcha hacía anochecer a media ejecución).
- Un fallo no debe dejar NPC vivos (contaminan la siguiente prueba): limpieza en `finally`/`finish`.
- `GameTestIsolation`: las pruebas de navegación/percepción desactivan el scheduler (dos conductores sobre un NPC medirían otra cosa); las del scheduler lo activan.

## Cómo ejecutar

`gradlew test` · `gradlew check` · `gradlew check -PwithCustomNpcs=true` · `-PnavTrace` para trazas.
