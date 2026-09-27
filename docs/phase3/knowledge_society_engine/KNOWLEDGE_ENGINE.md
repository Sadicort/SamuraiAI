# Knowledge Engine

`ai.knowledge`. Responde **¿qué creo saber del mundo?** (hechos con fuente; no experiencias). `KnowledgeEngine` posee un `KnowledgeRuntime` por NPC (índices por sujeto, objeto, nombre, tag, celda, tipo, predicado, estado). `learn(KnowledgeEvidence)`, `validate`, `teach`, `discover`, `tick` (olvido), `believes`, `about`, `graph`, `encyclopedia`, `explain`.
`KnowledgeRecord`: id, tipo, categoría, sujeto, predicado, objeto, atributos, **origen** (`LearnMethod`), **fuente**, confianza, importancia, fecha, lugar, tags, enlaces, apoyos/contradictores independientes, estado de validación, acceso, revisiones, versión, evidencia directa/pública/fiable, id de rumor.
Creencia ≠ verdad: puede ser falsa; siempre tiene procedencia.
