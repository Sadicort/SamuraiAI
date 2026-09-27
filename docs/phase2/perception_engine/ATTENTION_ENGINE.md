# Attention Engine

Clases: `attention.AttentionManager`, `AttentionFocus`, `AttentionLevel`, `AttentionSource`.

## Puntuación

Cada estímulo tiene una prioridad base por categoría (`StimulusCategory`: DAMAGE_TAKEN 95, EXPLOSION 90, FIRE 75, HOSTILE 70…) ajustada por: movimiento, sonido, daño, **quién es** (jugador, nombre conocido), **relación** con el NPC, **emociones** y **objetivo actual**. `AttentionSource` explica el porqué: `MOVEMENT, SOUND, DAMAGE, PLAYER, KNOWN_NAME, RELATION, EMOTION, GOAL, THREAT, NOVELTY`.

## Niveles

`NONE, LOW, MEDIUM, HIGH, FOCUSED, CRITICAL`.

## Estabilidad

- **Histéresis:** cambiar de foco exige un candidato `attentionSwitchRatio`=1.25× mejor (no parpadea entre dos jugadores).
- **Recuperación:** un foco que deja de percibirse se conserva `attentionRecoverTicks`=100; si reaparece se reengancha (`focusRecovered`).
- **Caducidad por duración del estímulo:** el foco expira según el intervalo de escaneo efectivo (regresión probada con `tierMultiplier` 4).
- Puntuación mínima para atender: 5.

## Salida

`AttentionFocus(label, category, type, score, reasons, since, expiresTick, lost)` dentro del snapshot; `PerceptionInspector` lo muestra (`focus=…`).

Pruebas: `AwarenessTest` (atención prefiere jugador sobre animal, histéresis, recuperación, expiración) y física `beingHurtIsCriticalEvidence` (la atención va al daño).
