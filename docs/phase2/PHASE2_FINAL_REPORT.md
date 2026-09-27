# Fase 2 (2.2 + 2.3 + 2.4) — Informe final

Fecha: 2026-09-25. Alcance: Navigation Engine AAA (2.2), Perception Engine 2.0 (2.3), Behavior Scheduler AAA (2.4), su integración con el Brain existente y la documentación exigida por el documento maestro.

## 1. Estado

| Fase | Estado | Código | Docs |
| --- | --- | --- | --- |
| 2.2 Navigation | completa | `ai.navigation` (93 ficheros) + adaptador | `navigation_engine/` (18) |
| 2.3 Perception | completa | `ai.perception` (91 ficheros, ≈ 3 800 líneas) + adaptador | `perception_engine/` (16) |
| 2.4 Scheduler | completa | `ai.scheduler` (92 ficheros, ≈ 4 300 líneas) + adaptador | `scheduler/` (16 + 3 extra) |
| Globales | completas | — | `PHASE2_*` (8) |

**Puerta de Foundation:** `FOUNDATION_STATUS.json` sigue en **PARTIAL / `phase2Unlocked: false`**. Se implementó por instrucción expresa del propietario; **no se falseó el estado ni se marcó READY**. Cerrar ese gate sigue siendo una tarea propia (ver `FOUNDATION_GATE_V2.md`).

## 2. Verificación final (2026-09-25)

| Comprobación | Resultado |
| --- | --- |
| `gradlew clean check` (sin CustomNPCs) ×2 | **OK** — 17/17 GameTests, `verifyDistributionJar` correcto |
| `gradlew check -PwithCustomNpcs=true` ×2 | **OK** — 17/17 GameTests (los NPC caminan de verdad), `verifyDistributionJar` correcto |
| Pruebas unitarias | **287**, 0 fallos, 1 omitida (previa, ajena a la Fase 2) |
| — Navigation | 46 (`PathfindingTest` 20, `NavigationRuntimeTest` 26) |
| — Perception | 60 (visión 13, oído 11, conciencia 20, sensores 16) |
| — Scheduler | 89 (línea temporal 7, personalidad/energía/emoción 14, prioridad/interrupciones 12, zonas/formaciones/grupos 15, optimización 7, ejecución 25, adaptador 7, rendimiento 2) |
| — Arquitectura | 7 reglas ejecutables |

Antes de dar por estables las pruebas físicas hubo fallos intermitentes; **todos eran del entorno de prueba** y están explicados en [PHASE2_TESTING](PHASE2_TESTING.md) (terreno natural con lava bajo el suelo, arenas superpuestas, reloj del mundo, NPC sin limpiar tras un fallo).

## 3. Criterios del documento maestro → evidencia

| Criterio | Evidencia |
| --- | --- |
| Brain no enruta | `ArchitectureRulesTest`; el Brain solo crea tareas (`NavigateTask`, `RoutineTask`) |
| Perception no decide ni mueve | reglas de arquitectura; `PerceptionSnapshot` es solo evidencia |
| Navigation no elige metas | reglas de arquitectura; recibe destino y opciones |
| Scheduler no mueve entidades | regla `theSchedulerNeverMovesEntitiesDirectly`; solo emite `SchedulerAdvice` |
| Movement Controller sin estrategia | `MovementController` solo dirige un cuerpo hacia un punto |
| Sin ciclos entre subsistemas | ver [PHASE2_ARCHITECTURE](PHASE2_ARCHITECTURE.md) |
| Sin valores hardcodeados | costes de terreno, estilos de vida/personalidades, calendario, perfiles de rutina, radios, umbrales: todo en `*Settings` + `.toml` y catálogos de datos |
| Configuración, presupuestos, cachés | 3 `.toml` (`navigation`, `perception`, `scheduler`), `RecordConfigBinder`; presupuestos de nodos, rayos y evaluaciones |
| Métricas y depuración por subsistema | `nav|perception|scheduler status/inspect/debug`; `*_METRICS.md`, `DEBUG_*.md` |
| Fail-safe con estados y recuperación | escalera de recuperación de navegación; sensores `FAILED` con reintento; evaluación aislada; configuración inválida ignorada |
| Compatibilidad Forge 1.19.2 / Java 17, CustomNPCs solo adaptador | compila y pasa en ambos perfiles; regla `noppes` |
| No se rompen APIs | métodos nuevos `default`/aditivos; con el scheduler apagado el Brain se comporta como antes |
| Prohibiciones (pathfinding en Brain, decisiones en Perception, raycasts globales por tick, recalcular todo cada tick, teletransporte como navegación normal) | ninguna presente; el teletransporte seguro es el último peldaño de recuperación |

### Escenarios de integración exigidos

| Escenario | Estado |
| --- | --- |
| patrulla → sonido → investigar → volver (pila de interrupciones) | verificado en servidor real, ambos perfiles |
| emergencia (override) | verificado en servidor real y unitariamente (civil huye, combatiente asiste) |
| grupo / formación / reelección de líder | verificado en servidor real (los guardias se mueven en formación con CustomNPCs) |
| línea temporal (periodos y calendario) | verificado en servidor real y unitariamente |
| visión → comportamiento | verificado en servidor real |
| bucle completo Percepción→Brain→Scheduler→Behavior→Navegación→Movimiento→Mundo | verificado con CustomNPCs; ver [PHASE2_RUNTIME_FLOW](PHASE2_RUNTIME_FLOW.md) |

## 4. Errores reales que salieron a la luz (y se corrigieron)

Navigation y Perception: ver sus changelogs. Scheduler e integración:

- Los centinelas `Long.MIN_VALUE` desbordaban la resta: **los grupos y los buckets nunca funcionaban** (lo destapó la primera ejecución de las pruebas de ejecución).
- La prioridad plana de FLEE (85) vencía a toda la agenda: un mercader sin motivo huía siempre. Ahora FLEE/COMBAT pierden 50 mientras el scheduler aconseja otra cosa y deben ganarse el sitio con evidencia.
- Una respuesta cuyo disparador desaparecía se cortaba de golpe en vez de respetar `responseHoldTicks` (ahora compite como candidato "en espera").
- `NPCInstance.getHome()` sin fijar devolvía la posición actual (la patrulla y las rutinas derivaban): el scheduler fija el hogar la primera vez que ve al NPC.
- (Detectado en revisión, no por una prueba) un candidato con la misma clave en dos capas (p. ej. dormir por reloj y por necesidad) podía hacer que la histéresis comparase con el equivocado: se elige el de mayor capa/puntuación.
- Con sonidos repetidos de explosión el NPC pasó (correctamente) de investigar a asistir/huir: la prueba usa un ruido inofensivo.

## 5. Rendimiento

Ver [PHASE2_PERFORMANCE](PHASE2_PERFORMANCE.md). Scheduler, 1000 NPC simulados: tick medio ≈ 0.69 ms, evaluación ≈ 53 µs, coste acotado por la cuota (no crece linealmente con la población).

## 6. Riesgos

- **Foundation PARTIAL**: el gate de la Fase 2 sigue sin certificarse.
- **Cifras de rendimiento simuladas** (scheduler) y sin prueba de carga con cientos de NPC físicos en servidor.
- **Overlays de partículas** de los tres motores sin verificación visual.
- **Un solo reloj** (Overworld) para todas las dimensiones.
- Comportamientos que dependen de cuerpos reales solo se ejercitan con CustomNPCs; otros controladores necesitarían implementar `movementBody`.
- Cambio de comportamiento en el motor de decisión heredado (penalización de FLEE/COMBAT mientras hay consejo): documentado y acotado a cuando el scheduler está activo.

## 7. Pendientes y límites conocidos (honestos)

**Navigation:** escaleras de mano y de piedra solo en simulador; natación vertical; salto de huecos; cuerpos de ancho ≥ 1 en puertas.
**Perception:** `BlockSensor` por proximidad (no línea de visión); `VoiceSensor` sin publicador; olfato sin viento ni rastros persistentes; un ruido muy breve puede no oírse.
**Scheduler:**
- `Temperament.speedFactor` calculado y expuesto, aún no consumido por Navigation.
- `GREET_NEARBY` e `IDLE_FIDGET` se anuncian (advice, inspector) pero no producen acción visible; `STAY_ALERT`/`SCAN_SURROUNDINGS` sí afectan a la percepción y `KEEP_FORMATION` a la velocidad.
- Energía, personalidad (deriva) y grupos fijados **no se persisten** entre reinicios (las zonas sí).
- La separación social solo ajusta el punto de espera al **empezar** una rutina.
- Los grupos automáticos se forman por estilo de vida y cercanía; no hay editor de rutas de patrulla más allá de zonas con waypoints.
- Sin jugadores en línea todos los NPC hibernan (intencionado).

## 8. Cambios en código existente (resumen)

`NPCController` (+`movementBody`, +`ownedEntityIds`), `CustomNPCsController`, `MoveToTask` (delega en navegación), `PatrolBehavior`, `FleeBehavior`, `ProtectBehavior`, `InvestigateBehavior` (con consejo), `SamuraiDefinition` (+INVESTIGATE), `GoalType` (+10), `UtilityDecisionEngine` (evidencia y consejo), `DefaultBrain` (candidato aconsejado, estados), `BehaviorRegistry`, `WorldContext` (+snapshot, +advice), `NPCInstance` (+home), `NPCTickService`, `NPCSpawnService`, `SamuraiCommand`, `Samuraiai`, `SamuraiLogger`, `build.gradle` (`-PnavTrace`), `event/EventSink` (compartido).
