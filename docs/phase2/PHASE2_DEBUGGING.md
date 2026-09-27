# Fase 2 — Depuración

## Comandos (operadores)

| Motor | Comandos |
| --- | --- |
| Navigation | `/samuraiai nav status`, `inspect <npc>`, `debug` (overlay), `metrics reset`, … — ver [DEBUG_NAVIGATION](navigation_engine/DEBUG_NAVIGATION.md) |
| Perception | `/samuraiai perception status`, `inspect <npc>`, `sound <x y z> <cat> <vol>`, `debug`, `metrics reset` — [DEBUG_PERCEPTION](perception_engine/DEBUG_PERCEPTION.md) |
| Scheduler | `/samuraiai scheduler status`, `inspect <npc>`, `groups`, `zones`, `zone add/remove/claim`, `group create/add/disband/formation`, `experience`, `debug`, `metrics reset` — [DEBUG_SCHEDULER](scheduler/DEBUG_SCHEDULER.md) |

Todos los `inspect` explican **por qué** (razones de puntuación, motivos de descarte, candidatos, pila de interrupciones).

## Overlays de partículas (solo el operador que los pide)

Navigation: ruta, nodo actual, puertas, peligro. Perception: campo visual, objetivos, sonidos, amenazas, investigación. Scheduler: zonas, destino de la rutina, líder, puestos de formación. **Ninguno se ha verificado visualmente** (no hay cliente en las pruebas).

## Trazas

`-PnavTrace` (en `gradlew runGameTestServer`/`check`) activa `debugLogging` de navegación y las trazas `SCHED_TRACE`/`INV_TRACE`/`PERC_VISION_TRACE` de las pruebas físicas. Así se diagnosticaron los fallos que solo aparecen en servidor real (autocolisión, hitbox 1×1, arenas superpuestas, hibernación sin jugadores).

## Diagnóstico frecuente

- *El NPC no hace lo aconsejado*: `scheduler inspect` (¿hay advice? ¿bucket HIBERNATING?) → objetivo del Brain (`WorldContext.advice`, bonus por capa) → sesión de navegación (`nav inspect`).
- *No oye un ruido*: ventana de 60 ticks del `SoundLog` y tier del NPC.
- *Vaivén de rutinas*: `routinesStarted` en `scheduler status`; ajustar `switchMargin`, `stickiness`, `minRoutineTicks`.
- *Sin jugadores en línea* (o en pruebas): la distancia es infinita y todos los NPC quedan en HIBERNATING (evaluación cada 600 ticks); es intencionado.
