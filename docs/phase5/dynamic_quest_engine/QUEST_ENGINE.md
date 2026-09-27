# Dynamic Quest Engine (Fase 5.3)

**Código:** `living/quest/**` (motor puro), `QuestEngine`, `QuestSettings` (`samuraiai-quest.toml`), puertos `api/QuestPorts`.

## Principio

«Qué historias y problemas surgen del estado real del mundo». Las misiones son **consecuencias**: el hub informa de condiciones del mundo (escasez, caravana perdida, ataque, festival cercano, NPC agradecido…) y el motor decide si cada una merece una misión, elige una plantilla, la ata a lugares y personas reales y la ofrece. **Ninguna lógica de misión vive en un NPC**: el que la da es un id y un nombre.

## Flujo

```
condición del mundo (hub) → análisis (report) → plantilla (QuestGenerator) → variables → misión ofrecida
   → aceptación (jugador) → objetivos por señales reales → camino elegido → fin → recompensas + consecuencias → historia
```

- **Análisis** (`report`): se ignora por debajo de `minSeverity` (0,2), en enfriamiento (`cooldownMinutes`, 2 días tras resolverse el mismo problema), por encima de `maxOpenQuests` (64) o de `maxOffersPerSettlement` (3) por asentamiento. La misma condición otra vez **evoluciona** la misión (giro si empeoró); un problema igual (mismo tipo y recurso) en el mismo sitio se **fusiona** si la plantilla lo permite; una condición de campaña abre la campaña.
- **Señales** (desde el adaptador y el hub): `talked`, `arrived`, `meditated`, `killed`, `delivered`, `caravanArrived`, `caravanAmbushed`, `worldEventEnded`, `conditionResolved`.
- **Tick** (bucket de misiones del hub, cada 20 ticks): caducan las ofertas no aceptadas (`offeredUntil`) y las activas fuera de plazo (`deadline`); las terminadas se olvidan a los 30 días (la historia queda).

## Documentos

`QUEST_GENERATOR.md`, `QUEST_TEMPLATES.md`, `STORY_ENGINE.md`, `VILLAGE_QUEST_ENGINE.md`, `TEMPLE_QUEST_ENGINE.md`, `ECONOMIC_QUEST_ENGINE.md`, `WAR_QUEST_ENGINE.md`, `RELATIONSHIP_QUEST_ENGINE.md`, `MEMORY_QUEST_ENGINE.md`, `EMOTION_QUEST_ENGINE.md`, `BRANCHING_ENGINE.md`, `CAMPAIGN_ENGINE.md`, `OBJECTIVE_ENGINE.md`, `REWARD_ENGINE.md`, `REPUTATION_ENGINE.md`, `CONSEQUENCE_ENGINE.md`, `QUEST_HISTORY_ENGINE.md`, `DEBUG_QUEST_OVERLAY.md`, `PHASE5_3_CHANGELOG.md`.

Pruebas: `QuestEngineTest` (9), `LivingWorldTest` (extremo a extremo, guerra, disputas y rencillas).
