# Emotion Engine

`ai.emotion`. Responde **¿cómo me siento y cómo me ha afectado lo vivido?**. `EmotionEngine` posee un `EmotionRuntime` por NPC: emociones activas (clave `tipo|origen`), ánimo y sus puntuaciones, traumas, historial, técnica de regulación, mezcla actual.
API: `trigger(EmotionTrigger)`, `tick(npc, now, Activity)`, `recover(npc, RecoverySource)`, `regulate`, `flashbacks`, `contagion`, y vistas `mood/blend/expression/physiology/tone/traumas/explain`.
Regla: **emoción nunca mueve entidades ni elige Behaviors**; devuelve consejos (`Expression`, `Physiology`, `RegulationAdvice`). El Brain consulta `CognitiveAdvice`, no vive dentro.
