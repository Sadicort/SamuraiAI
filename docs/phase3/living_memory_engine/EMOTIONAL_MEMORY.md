# Memoria emocional y eco emocional

`EmotionalSignature(primary, secondary, intensity 0-1, valence, traumatic)`.

- **Emoción → Memoria**: tras el disparador, si el peso emocional ≥ `emotionMemoryThreshold` (0,6) o hay trauma, `reinforceByEmotion` sube la fuerza, la importancia mínima (HIGH si ≥0,85) y, si es trauma, **protege** el recuerdo.
- **Eco** (`MemoryEngine.echoes`): recuerdos con emoción ≥ `echoMinIntensity` ligados a las personas/lugar presentes reactivan su emoción (`EmotionMemoryActivatedEvent`). Cooldown por recuerdo (`echoCooldownTicks`).
- **Sin bucles**: el hub convierte el eco en un `EmotionTrigger` marcado `fromEcho`; ni crea recuerdos, ni refuerza el recuerdo origen, ni se propaga por contagio.
