# Changelog 3.1 — Living Memory Engine

Añadido `ai.memory` completo (≈ 80 clases): modelo estructurado, pipeline, consolidación, olvido, evolución, compresión, índices, recuperación por contexto, caché, memoria espacial/procedimental/semántica/temporal, almacenamiento versionado, eventos, métricas, diagnóstico y depuración.
Integración: `CognitionEngine`, `CognitionService`. Tests: `MemoryEngineTest` (18).
Bugs encontrados por los tests: CME en reinterpretación; contadores de compresión con descarte por rareza; olvido perezoso.
**No** se ha implementado: embeddings; SQLite (sólo la costura).
