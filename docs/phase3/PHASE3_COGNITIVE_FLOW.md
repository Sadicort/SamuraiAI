# Flujo cognitivo

`CognitionEngine.experience(ExperienceInput)`:
1. **Experience** (perfil del catálogo: importancia base, emociones, efecto social, hechos a extraer, rumor, historia, deriva de personalidad).
2. **Memory** `observe` → CREATED / REINFORCED / DISCARDED (si se descarta, nada más ocurre: no todo importa).
3. **Emotion** `trigger` (peso = importancia del recuerdo) → emociones, ¿trauma?; si el peso emocional es alto → `reinforceByEmotion` sobre el recuerdo. Snapshot emocional al runtime de memoria.
4. **Relationship** `apply(SocialEvidence)` con `honorScale` cultural y ánimo → enlaces recuerdo↔relación; Relationship→Emotion (subir de etapa alegra; caer en sospecha genera desconfianza; amistad ayuda a recuperar).
5. **Knowledge** por cada `KnowledgeHint` (descubrimientos por el `DiscoveryEngine`); enlaces recuerdo↔conocimiento; las comunidades del NPC anotan lo que creen.
6. **Society**: rumor (si público o presenciado y suficientemente grande) con origen = recuerdo; historia pública y posición de la comunidad.
7. **Personality**: deriva lenta (umbral de magnitud, límite diario, tope).
8. El **Brain** consulta `CognitiveAdvice` (personas conocidas cercanas, peligro conocido, tradición, ánimo) → `UtilityDecisionEngine.cognitionModifier` (tope ±30). Scheduler y Navigation reciben personalidad/peligros.

Cada paso registra `TraceStage` con el mismo `traceId`, que también llevan los eventos.

## Bucles evitados
Eco y contagio se marcan (`fromEcho`/`CONTAGION`): no crean recuerdos, no refuerzan su origen, no se re-propagan. El estado emocional temporal sólo inclina (con tope) la lectura social.
