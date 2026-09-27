# Population Engine

**Código:** `living/world/population/PopulationLedger.java`; `WorldEngine.reportPopulation`; eventos `WorldPopulationChangedEvent`.

- La **población de cada asentamiento** la informa el Village Engine (que es dueño de *quién* vive allí) con su reparto por profesión; el total de una región es la suma de sus asentamientos.
- **Registro acotado** (`populationLog`, 2000 entradas) de cambios: llegada, partida, migración, nacimiento, muerte, desaparición — con minuto, asentamiento, persona, nombre y detalle. Los totales por tipo se conservan aunque el registro se recorte.
- Fuentes (todas en el hub `LivingReactions`): `CitizenJoinedEvent` (llegada), `CitizenLeftEvent` (muerte/migración/desaparición), `BirthRegisteredEvent` (nacimiento).
- `WorldPopulationChangedEvent(total, regiones, asentamientos)` cuando cambia el total.

La población **no** es un contador inventado: cada unidad es un ciudadano real del Village Engine (con cuerpo o sin él).
