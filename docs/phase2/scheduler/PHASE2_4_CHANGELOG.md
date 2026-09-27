# Fase 2.4 — Behavior Scheduler AAA: registro de cambios

Fecha: 2026-09-25.

## Estado del gate

`FOUNDATION_STATUS.json` sigue en **PARTIAL** (`phase2Unlocked: false`). Implementado por instrucción explícita del propietario; **no** se modificó ese estado ni se marcó READY.

## Añadido

- 92 ficheros en `yadi.samuraiai.ai.scheduler.*` (≈ 4 300 líneas): `time`, `routine`, `priority`, `interrupt`, `conflict`, `personality`, `emotion`, `social`, `energy`, `cooldown`, `stack`, `group`, `formation`, `zone`, `lifestyle`, `response`, `optimize`, `metrics`, `debug`, `events` (10), `engine` (settings, scheduler de mundo, de NPC, entradas/salidas) y el adaptador `world` (servicio, config, eventos Forge, comandos, almacén de zonas, overlay, `GoalMapper`).
- Jerarquía mundo → zona → NPC; línea temporal de 6 periodos y calendario mundial; 12 rutinas con perfiles configurables y planificador; Priority Engine 2.0 (4 capas + Emergency Override); pila de interrupciones con CANCEL/PAUSE/SUSPEND/RESUME/RESTART; 10 rasgos de personalidad; scheduler emocional; distancia social; coordinación de grupos (PATROL/GUARD/MERCHANT/VILLAGE/SQUAD; líder/guardia/explorador/reserva); 6 formaciones; propiedad de áreas y zonas; energía (fatiga, energía, foco, estrés, motivación); cooldowns; pila multi-comportamiento; resolutor de conflictos; planificador de respuesta a eventos; estilos de vida; buckets de tick y gestión de multitudes.
- Configuración `samuraiai-scheduler.toml` (83 valores + 3 catálogos), comandos `/samuraiai scheduler …`, overlay de partículas, persistencia de zonas (`data/samuraiai_zones.json`).
- Integración: `WorldContext.advice()`, `GoalMapper`, 10 `GoalType`, `RoutineBehavior`/`RoutineTask`, ganchos `PerceptionService.setSenseProfile` y `NavigationService.setPreferenceAdjuster`.
- 89 pruebas unitarias, 7 reglas de arquitectura y 5 GameTests del scheduler (más `GameTestIsolation` y `TestTerrain` para todas las pruebas físicas).

## Decisiones de diseño relevantes

- El scheduler **aconseja**; el Brain decide (bonus por capa) y el Behavior planifica. FLEE/COMBAT pierden 50 mientras se aconseja otra cosa.
- Sin jugadores en línea los NPC hibernan (600 ticks): intencionado.
- El hogar del NPC se fija la primera vez que se le ve.
- El tiempo es el del Overworld para todas las dimensiones.

## Corregido (hallado por las pruebas)

| Hallazgo | Dónde apareció | Corrección |
| --- | --- | --- |
| Centinelas `Long.MIN_VALUE` desbordaban la resta: grupos y buckets no funcionaban | pruebas de ejecución | centinelas `MIN_VALUE / 2` |
| Un grupo de dos no admitía a un tercero (no había líder aún) | pruebas de ejecución | se une si algún miembro está cerca |
| Respuesta que perdía su disparador se cortaba sin esperar | servidor real | candidato "en espera" durante `responseHoldTicks` |
| Mercader huía siempre (FLEE 85 plano) | servidor real | penalización mientras hay consejo |
| Hibernación sin jugadores (evaluación cada 600 ticks) | servidor real | pruebas suben los intervalos; documentado |
| Repetir explosiones se trataba como peligro real | servidor real | la prueba usa un ruido inofensivo |
| Prueba de la puerta esquivaba la puerta; lava bajo el suelo; reloj del mundo; NPC sin limpiar | servidor real | `TestTerrain`, parcelas propias, reloj restaurado, limpieza en `finally` |

## Verificación

`clean check` y `check -PwithCustomNpcs=true`, dos veces cada uno: 287 pruebas unitarias (0 fallos, 1 omitida previa), 17/17 GameTests, `verifyDistributionJar` correcto.

## Pendiente / límites conocidos

Ver [PHASE2_FINAL_REPORT](../PHASE2_FINAL_REPORT.md), sección 7 (velocidad del temperamento sin consumir, comportamientos de fondo sin acción visible, energía/personalidad no persistidas, un solo reloj, overlay sin verificación visual, sin prueba de carga con NPC físicos).
