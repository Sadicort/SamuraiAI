# Prediction Engine

Clase `prediction.TrajectoryPredictor`, `PredictedPosition`. Estima dónde estará un objetivo seguido y cuándo saldrá de la vista, desde su última posición y velocidad: **velocidad constante amortiguada**; la confianza baja cuanto más lejos se mira.

- `predict(x, y, z, vx, vz, ticksAhead, confidence)` y `predict(VisualTrack, now, ticksAhead)`.
- `ticksUntilLostFromView(eye, settings, track, horizon)`: ticks hasta que el objetivo abandona el campo visual.

Prepara seguir, buscar y (más adelante) combatir sin que ninguno sepa cómo se calcula. Hoy lo consume la investigación de "objetivo perdido" (a dónde ir a buscar). Cubierto por `AwarenessTest` (predicción con velocidad, confianza que baja con el horizonte, ticks hasta perder de vista, objetivo estático).
