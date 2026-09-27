# Fase 5 — Depuración

## Comandos (`/samuraiai living …`)

| Grupo | Documento |
| --- | --- |
| `status`, `metrics`, `save`, `debug`, `hud` | aquí |
| `calendar …` | `calendar_engine/DEBUG_CALENDAR_OVERLAY.md` |
| `world …` | `living_world_engine/DEBUG_WORLD_OVERLAY.md` |
| `village …` | `living_villages_engine/DEBUG_VILLAGE_OVERLAY.md` |
| `economy …`, `trade …`, `coins`, `contract …` | `economy_trade_engine/DEBUG_ECONOMY_OVERLAY.md`, `MARKET_ENGINE.md`, `CONTRACT_ENGINE.md` |
| `quest …` | `dynamic_quest_engine/DEBUG_QUEST_OVERLAY.md` |
| `family …` | `family_lineage_legacy_engine/DEBUG_FAMILY_ENGINE.md` |

Lista completa en `../COMANDOS.md`.

- `status`: fecha, clima y luna de tu celda, recuento del mundo y último error del tick.
- `metrics` (op): hub, interacciones físicas y métricas de los seis motores.
- `save` (op): guarda todo ahora.
- `debug` (op, jugador): overlay de partículas (aldeas, edificios, camas, caravanas).
- `hud` (jugador): calendario en la barra de acción.

## Log

`debugLogging = true` en el `.toml` de un motor registra sus eventos en `SamuraiAI/EVENTS`; en `samuraiai-living.toml`, los de todos. Los errores del tick del mundo vivo van a `SamuraiAI/BRAIN` («Living world tick failed») y quedan en `status`.

## Explicaciones

- ¿Por qué hace eso un NPC? `/samuraiai living village citizen <npc>` (plan del día y sesgo con razones) + `/samuraiai scheduler inspect <npc>` (candidatos con `life +N`).
- ¿Por qué cuesta eso? `/samuraiai living economy prices <aldea>` (factores).
- ¿De dónde salió este arroz? `/samuraiai living economy provenance <aldea> rice`.
- ¿Por qué existe esta misión? `/samuraiai living quest info <id>` (causa, condición, variables).
- ¿Quién heredará? `/samuraiai living family succession <npc>`.
