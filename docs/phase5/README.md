# Fase 5 — Living World, Society, Economy, Quests, Time & Generations

Índice de la fase maestra 5.0 → 5.5. Todo el código está en `src/main/java/yadi/samuraiai/living/**`.

| Documento | Contenido |
| --- | --- |
| `PHASE5_MASTER_ARCHITECTURE.md` | motores, hub, adaptador, reglas, reutilización |
| `PHASE5_DATA_OWNERSHIP.md` | una sola fuente de verdad por dato |
| `PHASE5_WORLD_SIMULATION.md` | qué pasa cada tick, cerca y lejos de jugadores |
| `PHASE5_OFFLINE_SIMULATION.md` | servidor apagado y puesta al día |
| `PHASE5_EVENT_FLOW.md` | eventos, reacciones, entradas y salidas del juego |
| `PHASE5_PERSISTENCE.md` | ficheros, formato, reinicio |
| `PHASE5_PERFORMANCE.md` | diseño y cifras medidas |
| `PHASE5_TESTING.md` | pruebas y resultados |
| `PHASE5_DEBUGGING.md` | comandos, overlay, log |
| `PHASE5_FINAL_REPORT.md` | estado, definición de hecho, límites |
| `PHASE5_GAP_ANALYSIS.md` | inventario previo (EXISTS/PARTIAL/MISSING) |
| `living_world_engine/` | 5.0 (21 docs) |
| `living_villages_engine/` | 5.1 (20 docs) |
| `economy_trade_engine/` | 5.2 (20 docs) |
| `dynamic_quest_engine/` | 5.3 (20 docs) |
| `calendar_engine/` | 5.4 (20 docs) |
| `family_lineage_legacy_engine/` | 5.5 (51 docs, incluida la extensión de identidad/clanes/ancianos) |

Configuración: `samuraiai-living.toml`, `-calendar`, `-world`, `-village`, `-economy`, `-quest`, `-family`. Comandos: `../COMANDOS.md` (sección «Mundo vivo»).

---

## Nota histórica — «Fase 5 — Producto y operación» (texto original, conservado)

> Antes de la fase maestra 5.0–5.5, este README describía otra «Fase 5» (producto y operación). Se conserva íntegro a continuación.

# Fase 5 — Producto y operación

## Implementado

- Perfil Gradle seguro por defecto sin CustomNPCs y perfil opt-in
  `-PwithCustomNpcs=true`.
- Dependencia CustomNPCs opcional, versión exacta declarada y refmap remapeado.
- Logging por categorías y contexto estructurado para spawn/remove.
- Configuración validada con límites y recarga atómica.
- Diagnóstico de Ollama al arranque y comando `/samurai probe`.
- `gradle check` ejecuta JUnit y Forge GameTests.
- Artefacto local no utilizado eliminado y logs de ejecución ignorados.
- Matriz de verificación y compatibilidad documentada.

## Compatibilidad conocida

Minecraft 1.19.2 + Forge 43.5.2 + Java 17 está verificado sin CustomNPCs.
Ollama se verifica contra `/api/tags`. El jar CustomNPCs
1.19.2.20250701 incluido tiene un fallo propio en servidor dedicado y debe
reemplazarse antes de publicar un release físico.

## Pendiente de release

No se puede declarar una matriz con CustomNPCs instalado ni publicar un jar
server-safe hasta disponer de una build externa corregida. Antes de producción
se recomienda ejecutar pruebas de carga con 1/10/50 NPCs, revisar privacidad de
prompts y generar CI para ambos perfiles.
