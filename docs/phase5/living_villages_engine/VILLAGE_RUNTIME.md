# Village Runtime

**Código:** `living/village/runtime/Village.java`, `VillageLayout.java`, `VillagePlanner.java`.

## `Village`

Una comunidad. Contiene: id (= id del asentamiento), región, nombre, cultura, clave de su comunidad de Knowledge, posición y radio, fecha de fundación, templo y mercado principales, distritos, edificios, ciudadanos, casas (`HomeRecord` por ciudadano), turno de noche, visitantes, eventos de aldea, puestos de mercado, contadores de memoria, seguridad, trazado, estado del mercado y afluencia, rito activo, **renombre** (con sus causas), **malestar** (sube si la comida cubre menos de `unrestFoodDays` días), **prosperidad** (la da la economía) y actividad social.

## Plano de una aldea nueva (`VillagePlanner`)

Determinista a partir del id: plaza en el centro con pozo, templo al norte, mercado y almacén al este, talleres (herrería, carpintería) al oeste, dojo y puesto de guardia, tres campos al sur, `plannedHouses` (8) casas en anillo alrededor de la plaza (un pozo cada cuatro) y puertas norte y sur. El plan crea edificios en estado `PLANNED` (o `BUILT` si la aldea solo existe en la simulación). **Levantar bloques reales no es parte del motor**: un edificio planificado sirve de destino y de capacidad, no de construcción física.

Se planifica al fundar por comando (`/samuraiai living village found <nombre> <radio> plan`) o para aldeas automáticas si `planAutoVillages = true` (por defecto `false`: una aldea que nace alrededor de un NPC no inventa edificios; sus edificios vienen de zonas del scheduler o de comandos).

## Trazado (`VillageLayout`)

Grafo de plaza, puertas, pozos, cruces y edificios con calles entre ellos; cada edificio cuelga del eje más cercano, así que siempre hay camino. `path(a, b)` responde cómo ir de un edificio a otro por las calles.

Prueba: `aPlannedVillageHasItsFunctionalBuildingsDistrictsAndStreets`.
