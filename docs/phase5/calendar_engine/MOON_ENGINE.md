# Moon Engine

**Código:** `living/calendar/moon/MoonEngine.java`, `living/core/MoonPhase.java`.

- Ciclo fijo de `moonCycleDays` (30) días desplazado `moonOffsetDays` (1): con los valores por defecto la **luna llena cae el día 15 de cada mes**.
- Ocho fases (`MoonPhase`: nueva, creciente, cuarto creciente, gibosa creciente, llena, gibosa menguante, cuarto menguante, menguante) agrupadas en cinco familias para los rituales (`NEW, WAXING, QUARTER, FULL, WANING`) y con `light()` 0..1 (brillo de la noche).
- `phase(día)`, `position(día)`, `daysToFull(día)`.
- `MoonPhaseChangedEvent` al cambiar de fase.

## Quién la usa

- **Festivales ligados a la luna:** Tsukimi espera a la luna llena dentro de su ventana (`FESTIVAL_ENGINE.md`).
- **Templos:** el escáner de condiciones del hub (`living/sim/ConditionScanner`) abre la misión «El rito de la luna llena» cuando una aldea con templo está a `ritualWarningDays` (2) días de la luna llena.
- Prompts y HUD muestran la fase («esta noche habrá luna llena»).
