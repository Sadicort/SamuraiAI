# Home Engine

**Código:** `living/village/homes/HomeEngine.java`, `HomeRecord.java`; adaptador `living/server/LivingService.pushHome/bedMoved`, `InteractionBridge.placeBeds`; `npc/NPCInstance.homeAssigned()`; `ai/scheduler/world/SchedulerService.rehome`.

## Un solo sistema de casas

No hay segundo sistema: el hogar de un NPC sigue siendo `NPCInstance.getHome()`, que el Behavior Scheduler ya lee para SLEEP/WAKE. El Village Engine decide **qué casa, qué habitación y qué cama**, y el adaptador escribe esa cama en `NPCInstance.setHome`.

## Asignación (`HomeEngine.assign`)

Al admitir un ciudadano se elige la casa (`HOUSE` construida o planificada) menos ocupada con plazas libres (`capacity`, 4 por defecto); la cama se coloca en un punto de la casa; se añaden objetos personales según la profesión y el radio de zona privada. Sin casa libre → sin cama y `HousingShortageEvent` (que abre la misión de construir casas).

## Del registro al mundo

1. **Activación:** si el NPC no tiene un hogar *asignado* (`homeAssigned()` es falso: su hogar es el punto donde apareció, valor por defecto del constructor), la cama de la aldea pasa a ser su hogar. Si alguien ya le dio uno (una instantánea restaurada, un comando), se respeta.
2. **Camas reales:** cuando `InteractionBridge` escanea una casa y encuentra camas (bloque `BedBlock`, parte cabecera), los residentes de esa casa se asignan en orden a esas camas; si el hogar del NPC seguía siendo la cama que puso el mundo vivo, se mueve a la real y el scheduler relee el hogar (`rehome`).
3. **Persistencia:** los `HomeRecord` se guardan con la aldea; tras reiniciar, el hogar se vuelve a empujar al activarse el NPC.

## Pruebas

`VillageEngineTest.citizensGetProfessionsAndHomesAndAVillageRunsOutOfBeds`, GameTests `anNpcBecomesACitizenWithABedAFamilyAndATimetable` y `aHousesRealBedBecomesItsResidentsHomeAndItsCampfireIsKnown`.
