# Lifecycle Hooks

**Código:** `living/family/lifecycle/LifeState.java`; `FamilyEngine.lifeState`; adaptador `LivingService.onDeath/deactivated`.

| Estado | Cuándo | Qué dispara |
| --- | --- | --- |
| `ALIVE` | persona viva | — |
| `MISSING` | NPC eliminado por comando o avatar desaparecido (`CitizenLeftEvent MISSING`) | misión de búsqueda si hay un pariente en la aldea; técnicas pueden perderse |
| `MIGRATED` | se muda a otra aldea | memoria («se marcha») |
| `DECEASED_FUTURE` | **muerte real en el mundo** (el cuerpo del NPC muere: `LivingDeathEvent`) | sucesión, herencia, legado, aprendizajes terminados, técnicas perdidas, cronología, extinción |
| `HISTORICAL` | antepasado conocido solo como registro | — |
| `UNBORN_FUTURE` | preparado | — |

**Límites honestos:** la muerte natural por edad no se simula; los nacimientos solo se registran a propósito (`/samuraiai living family birth <a> <b> <nombre>` o API), el mundo aún no los produce solo. Apagar el servidor **no** cambia el estado de nadie.
