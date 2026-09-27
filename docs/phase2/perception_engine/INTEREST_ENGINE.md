# Interest Engine

Clase `awareness.InterestEngine`, `InterestItem`. Curiosidad: puntúa lo que merece una mirada sin ser amenaza.

- Fuentes: entidad nueva, jugador desconocido, animal, ítem, suceso raro (puerta, bloque), ruido sin causa conocida.
- **Novedad:** se puntúa antes de recordar (regresión: al escribir la memoria antes, nada era nuevo). Lo ya recordado interesa menos.
- **Curiosidad del NPC:** `SenseProfile.curiosity` amplifica.
- Decaimiento 0.1/tick; umbral de detección 40 (`interestThreshold`).
- Las amenazas **no** son interesantes: las trata `ThreatEngine`.

Salida: lista ordenada `InterestItem` en el snapshot y `InterestDetectedEvent(key, label, score, x, y, z)`. `UtilityDecisionEngine` da +10 a INVESTIGATE si el interés principal ≥ 40; `PerceptionEmotionBridge` produce sorpresa.
