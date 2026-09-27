# Grafo de conocimiento (vista desde Memory)

El grafo pertenece al motor de conocimiento (`knowledge/graph/KnowledgeGraph`; ver `docs/phase3/knowledge_society_engine/KNOWLEDGE_GRAPH.md`). Memory se relaciona con él **sólo por ids**: `MemoryRecord.knowledgeLinks` ↔ `KnowledgeRecord.memoryLinks`. Así un hecho sabe de qué recuerdo salió y un recuerdo sabe qué hechos produjo, sin duplicar objetos.
