# Age Engine

**Código:** `living/family/aging/AgeEngine.java`, `LifeStage.java`.

- **Edad = fecha actual de Deiliora − fecha de nacimiento**, con el año del calendario (`minutesPerYear`). **Nunca a partir de ticks de entidad.**
- Etapas (años, configurables): bebé < 3, niño < 12, adolescente < 16, joven adulto < 25, adulto < 45, maduro < 60, anciano ≥ 60. Las tres primeras son `_FUTURE`: el mundo vivo aún no tiene niños con cuerpo, pero las edades son reales.
- Un NPC que llega recibe una edad adulta entre `minAdultAge` (18) y `maxAdultAge` (55), determinista.
- Cambio de etapa (comprobado cada día) → `LifeStageChangedEvent`; al llegar a joven adulto se le cuentan las historias familiares.
- La etapa sesga el día (niños no trabajan, ancianos menos) y el consumo (un niño come 0,6).
