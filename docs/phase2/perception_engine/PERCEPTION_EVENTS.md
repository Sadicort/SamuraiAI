# Eventos de percepción

Publicados en el EventBus 2.0 (`NPCEventBus`) mediante `EventSink`; son **hechos**, nunca órdenes.

| Evento | Campos |
| --- | --- |
| `VisionDetectedEvent` | npcId, targetId, kind, name, x/y/z, confidence, state |
| `VisionLostEvent` | npcId, targetId, kind, name, lastX/Y/Z |
| `SoundHeardEvent` | npcId, category, x/y/z, direction, intensity, distance, uncertainty, source |
| `SuspicionRaisedEvent` | npcId, value, source |
| `SuspicionClearedEvent` | npcId |
| `InterestDetectedEvent` | npcId, key, label, score, x/y/z |
| `ThreatDetectedEvent` | npcId, key, level, score, category, label, x/y/z |
| `AwarenessChangedEvent` | npcId, previous, next, reason |

Suscriptores actuales: `PerceptionEmotionBridge` (amenaza → miedo/ansiedad, sospecha → ansiedad, interés → sorpresa) y los tests. Los eventos salen a través de `EventSink` (interfaz común en `yadi.samuraiai.event`), de modo que los tests los capturan sin bus.
