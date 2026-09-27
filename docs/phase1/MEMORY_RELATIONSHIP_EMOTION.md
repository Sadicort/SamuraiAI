# Memoria, relaciones y emociones

`MemoryAccess` separa `CONVERSATION`, `WORKING`, `LONG_TERM`, `PERSISTENT`,
`SEMANTIC` y `EPISODIC`. Sólo `ConversationMemory` está activa; las demás son
extensiones preparadas. La memoria conversacional usa snapshots defensivos y se
libera en `NPCSpawnService.remove`/`MemoryManager.clearAll`.

Las relaciones sólo se modifican mediante `RelationshipService`, con clamp al
rango permitido y eventos `RelationshipCreatedEvent`, `RelationshipUpdatedEvent`
y `RelationshipRemovedEvent` (además del evento de compatibilidad existente).

Las emociones sólo se mutan mediante `EmotionService`; `EmotionState` ya no
expone mutadores públicos. Incluye estímulos para daño, conversación, combate,
miedo, alegría y descanso, y publica `EmotionChangedEvent` con acceso protegido.

`CoreTest` valida clamp, eventos, snapshots y limpieza. Riesgo futuro: conectar
memoria persistente sin convertirla en una referencia mutable compartida.
