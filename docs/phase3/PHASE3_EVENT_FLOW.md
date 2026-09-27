# Flujo de eventos

Familias: `MemoryEvent`, `RelationshipEvent`, `EmotionEvent`, `KnowledgeEvent` (+ `PersonalityEvolvedEvent`). Todas extienden `NpcEvent` y se publican por `EventSink` (producción: `NPCEventBus`). Llevan `traceId`.

Entradas al hub (bus y Forge):
| Origen | Experiencia |
| --- | --- |
| `LivingHurtEvent` en el avatar | `ATTACKED_ME`; vecinos → `WITNESSED_ATTACK` (públicos) |
| `LivingDeathEvent` | NPC muerto → `WITNESSED_DEATH` (traumático si es aliado); mob muerto por jugador mientras atacaba a un NPC → `HELPED_ME` (rescate) |
| `ExplosionEvent.Detonate` | `WITNESSED_EXPLOSION` |
| `VisionDetectedEvent` | `MET_PERSON` (una vez cada 3 días) |
| `ThreatDetectedEvent` (≥DANGER) | `THREAT_SEEN` (lugar peligroso) |
| `RoutineCompletedEvent` | `PATROLLED`/`MEDITATED`/`SLEPT` (+ `sleep()`) |
| `WorldScheduleEvent` (festival) | `CELEBRATED` |
| Diálogo completado | `CONVERSATION` |
| Escaneo de zonas/exploración | `VISITED_PLACE`/`DISCOVERED_PLACE`; rituales: `ATTENDED_RITUAL` |
| NPCActivated/Deactivated | cargar / guardar-descargar / borrar |

Trazabilidad: `mind trace <npc>` y `CognitiveTrace.steps(traceId)` (EXPERIENCE→MEMORY→EMOTION→RELATIONSHIP→KNOWLEDGE→SOCIETY).
