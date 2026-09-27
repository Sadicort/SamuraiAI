# Emotion Scheduler

Clases `emotion.{EmotionInput, Mood, EmotionInfluence, EmotionScheduler}`.

El adaptador convierte las emociones del NPC (miedo, ira, tristeza, alegría, calma, ansiedad, confianza) en `EmotionInput`. `EmotionScheduler` las traduce en **presión sobre la agenda**:

| Ánimo | Se activa con | Efecto en rutinas |
| --- | --- | --- |
| PANICKED | miedo dominante ≥ `fearEmergency`=75 | −REST/SOCIAL/WORK/SLEEP; **emergencia** |
| ANXIOUS | miedo/ansiedad dominante | +MEDITATE/PRAYER/GUARD, −SLEEP/SOCIAL |
| GRIEVING | tristeza dominante | +PRAYER/MEDITATE, +SOCIAL según sociabilidad, −WORK |
| ANGRY | ira ≥ `angerAggression`/2 | +TRAINING/PATROL, −REST/MEDITATE/SOCIAL |
| CONTENT | alegría | +SOCIAL/WORK |
| CALM | calma | +MEDITATE leve |

- **Histéresis:** el ánimo solo cambia si otro supera al actual en `emotionShiftThreshold`=15 → `EmotionPriorityChangedEvent`.
- El miedo/ansiedad **añade estrés** a la condición del NPC.
- Un ánimo con sesgo ≥ 20 crea un candidato PERSONAL (`Source.EMOTION`).
- El coraje sube el umbral de pánico.

Pruebas: `moodOnlyChangesWhenAnotherClearlyOvertakesIt`, `griefPullsTowardsPrayerAndAngerTowardsTraining`, `panicFromFearAloneIsAnEmergencyAndShowsInTheMood`; físico `fearOverridesTheNightsSleep`.
