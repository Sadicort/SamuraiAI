# Modelo emocional

`EmotionKind` (20): JOY, CALM, CURIOSITY, HOPE, PRIDE, GRATITUDE, SADNESS, FEAR, ANGER, SHAME, GUILT, ANXIETY, COMPASSION, DETERMINATION, RESPECT, DISTRUST, LONELINESS, INSPIRATION, EMOTIONAL_FATIGUE, SURPRISE (valencia y activación por defecto; extensible).
`EmotionRecord`: id, tipo, `EmotionOrigin` (fuente, recuerdo, ref, traza), intensidad/inicial/pico (0-100), fase (ACTIVE/FADING/RECOVERED/SUPPRESSED), curva de decaimiento, **dimensiones** (estabilidad, control, valencia, activación, persistencia), influencias (causas), eventos relacionados, versión, trauma enlazado. Nunca sólo `"sad"`.
