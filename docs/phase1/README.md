# SamuraiAI — Fase 1

Documentación de la reparación del núcleo realizada el 15 de septiembre de 2026.

## Alcance

Esta fase estabiliza el Brain, Runtime, lifecycle, cola de IA, diálogo, memoria,
eventos, configuración, diagnóstico de Ollama y la integración opcional con
CustomNPCs. No añade patrulla, movimiento físico, seguimiento, huida, comercio
ni combate.

## Documentos

- [THREADING](THREADING.md): frontera del hilo principal y scheduler.
- [OLLAMA_QUEUE](OLLAMA_QUEUE.md): cola FIFO, estados, cancelación y métricas.
- [CUSTOMNPCS_FIXES](CUSTOMNPCS_FIXES.md): carga condicional, dependencia y Mixin.
- [LIFECYCLE](LIFECYCLE.md): transiciones, spawn/remove y prevención de fantasmas.
- [NPC_MANAGER](NPC_MANAGER.md): registros, índices y nombres únicos.
- [MEMORY_RELATIONSHIP_EMOTION](MEMORY_RELATIONSHIP_EMOTION.md): servicios de estado.
- [EVENTS_LOGGING_CONFIG](EVENTS_LOGGING_CONFIG.md): EventBus, logging y configuración.
- [OLLAMA_DIAGNOSTICS](OLLAMA_DIAGNOSTICS.md): verificación de proveedor y fallbacks.
- [PERSISTENCE_BOOTSTRAP](PERSISTENCE_BOOTSTRAP.md): DTO versionado y hooks.
- [TESTING](TESTING.md): JUnit, GameTests y comandos de verificación.
- [VERIFICATION](VERIFICATION.md): matriz y evidencia de ejecución real.
- [PHASE1_CHANGELOG](PHASE1_CHANGELOG.md): inventario de cambios y riesgos abiertos.

La inspección inicial, incluyendo las imágenes de `visualizarimganes`, está en
[ANALISIS_COMPLETO_PROYECTO.md](../../ANALISIS_COMPLETO_PROYECTO.md).
