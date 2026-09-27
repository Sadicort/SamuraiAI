# Eventos de memoria

Familia `MemoryEvent` (`npcId`, `traceId`) en `ai/memory/events`: `MemoryCreatedEvent`, `MemoryUpdatedEvent`, `MemoryMergedEvent`, `MemoryForgottenEvent`, `MemoryRetrievedEvent`, `MemoryConsolidatedEvent`, `MemoryCompressedEvent`, `EmotionMemoryActivatedEvent`. `KnowledgeUpdatedEvent` pertenece al motor de conocimiento.

Se publican por el `EventSink` (en producción el `NPCEventBus`). Existe además el antiguo `event.npc.MemoryCreatedEvent` (diálogo) que no se ha tocado.
