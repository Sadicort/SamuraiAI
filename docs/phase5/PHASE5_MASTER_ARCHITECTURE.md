# Fase 5 — Arquitectura maestra del mundo vivo

Seis motores, **una** arquitectura: Living World (5.0), Living Villages (5.1), Economy & Trade (5.2), Dynamic Quests (5.3), Calendar & Seasons (5.4) y Family, Lineage & Legacy (5.5), unidos por un hub y un adaptador.

```
                    Minecraft / Forge (servidor)
                               │
        living/server  ── adaptador: LivingService, LivingEvents, LivingCommand, LivingConfig,
                               │     CognitionOutside, BiomeClassifier, ResourceItems, ZoneBridge,
                               │     InteractionBridge, LivingDebugRenderer
                               │
        living/sim     ── hub: LivingWorld (tick, día por etapas, llegadas, fundaciones),
                               │     LivingBridges (puertos), LivingReactions (reacciones),
                               │     ConditionScanner (condiciones de estado), Outside
        ┌──────────┬───────────┼───────────┬───────────┬───────────┐
   calendar      world      village     economy      quest       family      (motores puros: sin Minecraft,
        └──────────┴───────────┴───────────┴───────────┴───────────┘            sin importarse entre sí)
                               │
        living/core    ── vocabulario común: WorldClock, CalendarDate, Season, WeatherKind, MoonPhase,
                               DayPhase, Dice, Provenance, LivingEvent, Skill, TickBudget, LivingStorage
```

## Reglas (comprobadas por `LivingArchitectureTest`)

1. Los seis motores **no se importan entre sí**: cada uno declara lo que necesita como **puertos** (interfaces propias: `WorldPorts`, `VillagePorts`, `EconomyPorts`, `QuestPorts`, `FamilyPorts`, más `CalendarEngine` como `WorldClock`) y el hub los implementa (`LivingBridges`).
2. Ningún motor ni el hub tocan Minecraft, Forge ni el runtime de NPCs: solo `living/server`.
3. `living/core` no depende de ningún motor.
4. Todo nombre de experiencia que el mundo vivo pide a la capa cognitiva existe en su catálogo.

## Reutilización del resto de SamuraiAI (sin duplicar)

| Sistema existente | Uso |
| --- | --- |
| `NPCEventBus` / `EventSink` / `NpcEvent` | todos los eventos de la Fase 5 son `LivingEvent` que viajan por el mismo bus |
| `VersionedStore` (Fase 3) | persistencia con formato `samuraiai-living` |
| `RecordConfigBinder` / `*Settings` | 7 ficheros `.toml` |
| Behavior Scheduler | recibe el día de la aldea como `RoutineBiasSource` (sigue decidiendo) y relee el hogar (`rehome`) |
| Capa cognitiva (Fase 3) | comunidades (= aldeas), historia recordada, reputación (`standing`), relaciones, emociones, personalidad, aprendizaje: a través de `CognitionOutside` |
| Perception | `ThreatDetectedEvent` → amenaza de aldea |
| Zonas del scheduler | → edificios de aldea |
| `NPCInstance.home` | → cama de la aldea |
| Diálogo / prompt | sección «TU MUNDO» (`AIContext.world`) y fecha de Deiliora |
| Combate (`CombatStartedEvent`, muertes) | amenaza de aldea, muertes reales, objetivos de combate |

No existe un «Battlefield Engine» de una fase de combate anterior; la guerra entra como eventos de mundo (`PHASE5_FINAL_REPORT.md`).

## Documentos

`PHASE5_DATA_OWNERSHIP.md`, `PHASE5_WORLD_SIMULATION.md`, `PHASE5_OFFLINE_SIMULATION.md`, `PHASE5_EVENT_FLOW.md`, `PHASE5_PERSISTENCE.md`, `PHASE5_PERFORMANCE.md`, `PHASE5_TESTING.md`, `PHASE5_DEBUGGING.md`, `PHASE5_FINAL_REPORT.md`, `PHASE5_GAP_ANALYSIS.md` y las seis carpetas de motor.
