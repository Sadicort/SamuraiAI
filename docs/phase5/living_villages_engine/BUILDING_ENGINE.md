# Building Engine

**Código:** `living/village/buildings/Building.java`, `BuildingKind.java`, `OwnerRef.java`; `VillageEngine.registerBuilding/setBuildingState`; adaptador `living/server/ZoneBridge.java`, `InteractionBridge.java`.

## Edificio

Tipo, lugar y radio, estado (`PLANNED, BUILT, DAMAGED, DESTROYED, ABANDONED`) y condición 0..1, propietario (`OwnerRef`), ocupantes, referencia al almacén que la economía guarda para él, horario de apertura por fase del día, notas de lo que le pasó y la **zona del Behavior Scheduler** que lo hace destino de rutinas.

| Tipo | Distrito | Zona del scheduler | Lugar de oficio | Capacidad |
| --- | --- | --- | --- | --- |
| HOUSE | residencial | HOME | HOUSE | 4 camas |
| PLAZA | mercado | PLAZA | PLAZA | 40 |
| MARKET | mercado | MARKET | MARKET | 12 |
| TEMPLE | espiritual | TEMPLE | TEMPLE | 20 |
| SMITHY, CARPENTRY, WORKSHOP, WAREHOUSE | artesanal/mercado | WORK | SMITHY… | 3–4 |
| KITCHEN | residencial | DINING | KITCHEN | 12 |
| DOJO | militar | TRAINING | DOJO | 16 |
| GUARD_POST, GATE | militar | GUARD_POST | GUARD_POST/GATE | 4–6 |
| FARM, DOCK, STABLE | agrícola | WORK | FARM/DOCK/STABLE | 6 |
| WELL | residencial | PLAZA | WELL | 6 |
| MINE, LUMBER_CAMP | bosque exterior | WORK | MINE/OUTSKIRTS | 6–8 |

`STABLE` está preparado (ningún oficio lo usa aún). Un edificio es **funcional** cuando está `BUILT` o `DAMAGED` (`usable()`).

## De dónde salen los edificios

1. **Plano** de una aldea nueva (`VillagePlanner`, estado PLANNED).
2. **Zonas del scheduler** (`ZoneBridge`, cada `zoneImportTicks` = 600 ticks): una zona marcada en el mundo dentro de una aldea se convierte en su edificio (HOME→casa, WORK→taller, MARKET→mercado, TEMPLE→templo, TRAINING→dojo, GUARD_POST→puesto, DINING→cocina, PLAZA→plaza; rutas y áreas de descanso no son edificios), enlazado por id de zona. La zona es la fuente de verdad de la posición: si se mueve, el edificio se mueve; si se borra, el edificio queda **abandonado** (nunca se borra: su historia se conserva); si se vuelve a marcar, vuelve a estar construido.
3. **Comando** `/samuraiai living village building <tipo> [radio]` (op) en la posición del jugador.
4. **Misiones** de construcción (`QuestPorts.Villages.buildHouse`: la misión de escasez de vivienda termina registrando una casa).

`setBuildingState` publica `BuildingStateChangedEvent`; construir anota en la memoria comunitaria; destruir una casa libera las camas de sus residentes. Los incendios dañan o destruyen casas (`../living_world_engine/WORLD_EVENT_ENGINE.md`).
