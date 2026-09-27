# Suspicion System

Clase `awareness.SuspicionMeter` (0–100) con `SuspicionSource`: `HIDDEN_MOVEMENT, SOUND, PLAYER_VANISHED, DOOR_OPENED, BLOCK_BROKEN, UNKNOWN_PRESENCE, THREAT, CUSTOM`.

- **Sube** con lo que inquieta: movimiento tras cobertura, un ruido sin causa, un jugador que desaparece, una puerta que se abre, un bloque roto. La ganancia se escala por el `SenseProfile` (`suspicionGain`: un NPC cauteloso se alarma antes).
- **Baja** despacio: `suspicionDecayPerTick`=0.2.
- **Histéresis:** "levantada" al cruzar 30 hacia arriba, "despejada" al bajar de 10; así no parpadea.
- Eventos: `SuspicionRaisedEvent(value, source)` y `SuspicionClearedEvent`. `PerceptionEmotionBridge` los convierte en ansiedad.

Uso: `PerceptionSnapshot.suspicious()`; `EventResponsePlanner` genera `WATCH` (vigilar) si hay sospecha sin objetivo que investigar. Prueba física: `aBlockBrokenNearbyIsHeardAndInvestigated` exige objetivo de investigación **o** sospecha > 0.
