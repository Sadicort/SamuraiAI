# Changelog 3.3 — Emotion Engine

Añadido `ai.emotion` (≈ 50 clases). El `emotion.EmotionState/EmotionService` legado (10 valores) se **conserva** como superficie que leen el Scheduler y los prompts; el motor nuevo la alimenta (sólo eleva; la legada decae sola). Tests: `EmotionEngineTest` (14). Correcciones: el ánimo no volvía a NEUTRAL sin margen; `lastUpdate` se persiste para que el tiempo apagado cuente.
