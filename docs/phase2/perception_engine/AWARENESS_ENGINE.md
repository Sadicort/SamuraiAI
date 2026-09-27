# Awareness Engine

Clases: `awareness.AwarenessEngine`, `AwarenessLevel`, `AwarenessMap`.

## Estados

`UNAWARE → AWARE → ALERT → SEARCHING → TRACKING → FOCUSED`.

## Reglas (`AwarenessEngine.target`)

| Estado | Condición |
| --- | --- |
| FOCUSED | atención ≥ FOCUSED con el foco a la vista, o amenaza CRITICAL, o amenaza DANGER con foco visible |
| TRACKING | foco visible y (sospecha, amenaza ≥ WARNING o atención ≥ HIGH) |
| SEARCHING | foco no visible, recuerda un objetivo perdido o algo oído, **y hay preocupación** (sospecha, amenaza o sospecha residual) |
| ALERT | sospecha, amenaza ≥ WARNING o atención ≥ HIGH |
| AWARE | atención ≥ MEDIUM, cualquier estímulo o sospecha residual |
| UNAWARE | nada |

## Estabilidad

**Subir es inmediato; bajar es de un escalón cada vez y solo tras `awarenessMinDwellTicks`=20.** Perder un foco por sí solo no basta para SEARCHING (regresión: hacía falta preocupación).

## Mapa de conciencia

`AwarenessMap` resume qué conoce el NPC (por tipo de memoria y total) para el inspector y el debug.

Evento: `AwarenessChangedEvent(previous, next, reason)`. Consumo: `UtilityDecisionEngine` (TRACKING+ favorece PROTECT/COMBAT y resta TALK), `EventResponsePlanner` (`Perceived.awareness`).
