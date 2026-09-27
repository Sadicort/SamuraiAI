# Fase 3 — Arquitectura

```
WORLD → PERCEPTION → EXPERIENCE → MEMORY → EMOTION → RELATIONSHIP → KNOWLEDGE → SOCIETY → BRAIN → SCHEDULER → BEHAVIOR → ACTION → WORLD
```

## Paquetes (todo Java puro salvo `cognition/world`)
| Paquete | Rol |
| --- | --- |
| `ai.cognition.model` | **Núcleo compartido**: `EntityRef`, `PlaceRef`, `Stamp`, `Trait`, `EmotionKind`, `ExperienceKind`, `Cause/CauseLog`, `PersonalityView`. |
| `ai.cognition.storage` | `VersionedStore` (sobre, checksum, escritura segura, migraciones), `Json`, `CognitionStorage` (política por NPC). |
| `ai.cognition.engine` | **Hub** `CognitionEngine`: traduce entre motores. También `CognitiveAdvice`, `WorldFacts`, `ExperienceInput`. |
| `ai.cognition.catalog` | `ExperienceCatalog`: significado de cada experiencia **como datos**. |
| `ai.cognition.personality` | `PersonalityLedger` (base + evolución) y `TemporaryModifiers`. |
| `ai.cognition.trace`, `.debug` | Traza mundo→comportamiento e inspector unificado. |
| `ai.memory`, `ai.relationship`, `ai.emotion`, `ai.knowledge` | Los cuatro motores. **No se importan entre sí.** |
| `ai.cognition.world` | Adaptador Forge: `CognitionService`, `CognitionEvents`, `CognitionConfig`, `CognitionCommand`, `PlaceClassifier`, overlay. |

## Contrato de separación (test `CognitionArchitectureTest`)
- Los motores y el hub no importan Minecraft, Brain, Behaviors, Tasks, Decision, Goal, controller, ni Perception/Navigation/Scheduler.
- Memory, Relationship, Emotion y Knowledge **no dependen unos de otros ni del hub**: se comunican por contratos (`SocialEvidence`, `EmotionTrigger`, `KnowledgeEvidence`, `ReputationHearsay`, `SourceView`, `PersonalityView`), lo que evita ciclos.
- Perception/Navigation/Scheduler (núcleos) no importan los motores; el Brain sólo lee `CognitiveAdvice` por `WorldContext.cognition()` y `Brain/Decision` no importan memoria.

## Runtimes por NPC
`MemoryRuntime`, `RelationshipRuntime`, `EmotionRuntime`, `KnowledgeRuntime`, `PersonalityLedger`: uno por UUID, jamás compartidos (test `oneNpcsMutableStateIsNeverSharedWithAnother`).

## Reutilización (sin duplicar)
Se conservan `MemoryManager/ConversationMemory` (diálogo), `EmotionState/EmotionService` (10 valores) y `RelationshipService` (5 ejes): reciben una **proyección** de los motores nuevos. `SchedulerService` recibe la evolución de personalidad (`replaceTraits`) y Navigation un `DangerSource`.
