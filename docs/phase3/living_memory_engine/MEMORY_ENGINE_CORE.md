# Memory Engine — núcleo

Paquete `yadi.samuraiai.ai.memory` (Java puro, sin Minecraft). Responde a **¿qué me ocurrió?**.

## Piezas
| Clase | Responsabilidad |
| --- | --- |
| `engine/MemoryEngine` | Coordinador. Posee un `MemoryRuntime` por NPC y delega en los servicios; **nunca toca un archivo**. |
| `engine/MemoryRuntime` | Todo lo que recuerda *un* NPC: recuerdos, índices, cachés, mapa espacial, habilidades, creencias semánticas, snapshot emocional, flags dirty/urgent. Jamás se comparte. |
| `pipeline/*` | Evaluación, importancia, clasificación, protección. |
| `consolidation/`, `forgetting/`, `evolution/`, `compression/` | Vida del recuerdo. |
| `indexing/`, `retrieval/`, `cache/` | Acceso rápido. |
| `spatial/`, `procedural/`, `semantic/`, `temporal/` | Tipos de memoria. |
| `storage/`, `metrics/`, `diagnostics/`, `debug/`, `events/` | Persistencia y observabilidad. |

## API
`observe(Experience, EvaluationContext, now)` · `retrieve(npc, RetrievalQuery, now)` · `relevant(npc, RetrievalContext, limit, now)` · `echoes(...)` · `tick(npc, now)` · `sleep(npc, now)` · `reinforceByEmotion(...)` · `linkSocial/linkKnowledge` · `remove/protect` (administración).

## Reglas
- Memory no es dueño de Relationship ni de Knowledge: sólo guarda **ids** (`socialLinks`, `knowledgeLinks`).
- No importa ningún otro motor (test `CognitionArchitectureTest`).
- Configuración: `MemorySettings` (≈95 campos, `samuraiai-memory.toml`), todos con *clamp* en el constructor del record.
