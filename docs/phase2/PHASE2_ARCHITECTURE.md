# Fase 2 — Arquitectura global

Tres motores nuevos sobre el Brain existente, cada uno con **una** responsabilidad y un contrato de datos explícito.

```
                     ┌──────────────────────────── Brain (DefaultBrain, UtilityDecisionEngine) ───────────────────────────┐
                     │  decide el objetivo (Goal) · planifica con el Behavior · ejecuta Tasks                              │
                     └───────▲───────────────────────▲──────────────────────────────────────────┬──────────────────────────┘
        PerceptionSnapshot   │                       │ SchedulerAdvice                          │ Task (NavigateTask / RoutineTask)
     (WorldContext.snapshot) │                       │ (WorldContext.advice)                    ▼
              ┌──────────────┴─────────┐   ┌─────────┴────────────┐               ┌─────────────────────────────┐
              │  Perception Engine 2.0 │   │  Behavior Scheduler  │──Temperament─▶│  Navigation Engine AAA      │
              │  (qué concluye el NPC) │   │  (qué quiere hacer)  │  (SenseProfile,│  (cómo llegar; MovementBody) │
              └───────────▲────────────┘   └───────────▲──────────┘   preferencias)└──────────────┬──────────────┘
                          │  sonidos, daño, entidades  │ reloj, zonas, grupos                   ▼
                          └──────────── Mundo Minecraft / CustomNPCs (solo detrás de adaptadores) ◀── MovementController
```

## Reglas de separación (ejecutables: `ArchitectureRulesTest`, 7 reglas)

| Regla | Comprobación |
| --- | --- |
| Perception no decide ni mueve | no importa brain, behavior, task, action, decision, goal, controller, emotion, navegación ni scheduler |
| Navigation no elige metas | no importa perception, scheduler, brain, behavior, decision, goal |
| El scheduler no mueve entidades | no importa `net.minecraft.world.entity` ni `navigation.movement/world` |
| Los tres núcleos son Java puro | ningún `net.minecraft`, `net.minecraftforge` ni `noppes` fuera de `world/` |
| El scheduler no conoce Brain, percepción ni navegación | ni `context`, `npc`, `emotion`, `goal`… (solo datos propios) |
| CustomNPCs solo como adaptador | `noppes.*` únicamente en `integration/customnpcs` |
| Motores internos sin acoplarse | `scheduler/engine` y `group` sin percepción/navegación |

Las dependencias entre **subsistemas** son acíclicas (Brain → {Perception, Scheduler, Navigation}; Scheduler·adaptador → {Perception, Navigation}; Perception ↔ Navigation solo por el `DangerSource` que Perception registra en Navigation desde su adaptador). Dentro del paquete `scheduler` hay acoplamiento entre subpaquetes por los tipos de valor compartidos (`Candidate`, `Intent`, `SchedulerSettings` en `engine`); no afecta a la regla anterior.

## Patrones comunes

- **Contexto/snapshot inmutable** entre motores: `PerceptionSnapshot`, `SchedulerAdvice`, `SchedulerInput`, `Perceived`, `EmotionInput`.
- **EventBus 2.0** mediante `EventSink` (interfaz común); los tests capturan eventos sin bus.
- **Configuración = record**: cada motor tiene `*Settings` (record que se limita a sí mismo, `Builder` reflexivo) enlazado 1:1 con su `.toml` por `RecordConfigBinder`; recarga atómica.
- **Presupuestos y cachés con invalidación**: nodos A* por tick, raycasts por tick, evaluaciones por tick, caché de rutas por chunk, memoria acotada.
- **Fail-safe**: sensor fallido → FAILED + reintento; evaluación fallida → contada y aislada; ruta sin salida → recuperación escalonada; config inválida → se conservan los valores anteriores.
- **Nada hardcodeado**: costes de terreno (tabla + overrides), personalidades y horarios (estilos de vida), calendario, perfiles de rutina, radios de sonido, umbrales.

## Mapa de paquetes

`ai.navigation` (93 ficheros) · `ai.perception` (91 ficheros, ≈ 3 800 líneas) · `ai.scheduler` (92 ficheros, ≈ 4 300 líneas) · adaptadores en `*/world` · integración en `task/` (`NavigateTask`, `RoutineTask`), `behavior/`, `decision/`, `brain/`, `context/`, `runtime/`, `spawn/`, `command/`, `Samuraiai`.

Documentación por motor: [navigation_engine](navigation_engine/NAVIGATION_ENGINE.md), [perception_engine](perception_engine/PERCEPTION_ENGINE.md), [scheduler](scheduler/BEHAVIOR_SCHEDULER.md).
