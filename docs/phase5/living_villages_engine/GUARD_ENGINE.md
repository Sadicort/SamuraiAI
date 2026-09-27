# Guard Engine

**Código:** `living/village/guards/GuardEngine.java`; evento `GuardShiftChangedEvent`.

- Guardias y samuráis son ciudadanos con **guardia**. Una parte (`nightWatchShare`, 34 %) hace la guardia de noche, equilibrada al cambiar la plantilla.
- La guardia **cambia a las 06:00 y a las 18:00**. De servicio, un guardia quiere `GUARD` y `PATROL`; fuera de servicio, `TRAINING` y descanso.
- En **peligro o ataque** todos están de servicio.
- Los eventos de aldea tienen un sesgo propio para guardias (`guardBias`: ataque GUARD +90, PATROL +70, REST −80; emergencia GUARD +40, PATROL +30…).
- Misión `GUARD_SHORTAGE` cuando faltan guardias en una aldea amenazada.
