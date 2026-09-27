# Nombramiento de casas

**Código:** `FamilyEngine.considerHouseTitle` (privado, llamado desde `simulate`); `FamilyRecord.grantHouse/isHouse/houseTitle/houseGrantedAt`; `NameCultureProfile.houseWord`.

## Qué es una casa

Una familia suficientemente importante y con suficiente historia detrás pasa a ser conocida como «Casa <apellido>» — nunca al revés: el nombre de la familia (`FamilyRecord.name()`) no cambia, `houseTitle` es un título añadido que se muestra junto a él. No hay un campo `boolean isHouse` guardado aparte: `FamilyRecord.isHouse()` es simplemente `!houseTitle.isEmpty()`.

## Cuándo se concede

`considerHouseTitle(f)`, llamada una vez al día por cada familia activa desde `simulate`, concede el título cuando **las dos condiciones** se cumplen a la vez:

| Condición | Umbral | Configurable en |
| --- | --- | --- |
| importancia histórica de la familia | ≥ `houseImportanceThreshold` (0.5 por defecto) | `FamilySettings` / `samuraiai-family.toml` |
| generaciones que lleva existiendo | ≥ `houseMinGenerations` (3 por defecto) | ídem |

Una vez concedido (`f.grantHouse(título, ahora)`), el título **no se retira nunca** — ni si la importancia baja después, ni si la familia decae. Se registra como un recuerdo familiar (`FamilyMemoryEntry.Kind.OTHER`) y se publica `HouseTitleGrantedEvent`.

## La palabra de la casa

El título se compone como `perfil.houseWord() + " " + f.name()` usando la cultura de la propia familia (`NameCultureCatalog.of(cultura).houseWord()`). Hoy las cinco culturas comparten la palabra **"Casa"** — una simplificación consciente documentada también en `CULTURAL_NAMING_PROFILES.md`: el campo existe por cultura para poder diferenciarlas más adelante (por ejemplo "Hogar" para Ceniza del Norte, "Linaje" para los Huecos) sin tocar `considerHouseTitle` ni la persistencia, que ya leen la palabra del perfil en vez de tenerla escrita a fuego.

## Persistencia

`cultureId`, `clan`, `houseTitle` y `houseGrantedAt` viajan en el bloque de familia de `families.json` (`FamilyStorage`); una familia que ya era una casa antes de guardar sigue siéndolo después de cargar — probado en `FamilyIdentityExtensionTest.identityExtensionSurvivesARestart`.

## Pruebas

`FamilyIdentityExtensionTest.aFamilyEarnsAHouseTitleFromRealHistoryAndAPersonEarnsAnEpithetFromRealHonour`: baja los umbrales con `FamilySettings.builder()`, hace crecer la importancia histórica y las generaciones de una familia real, y comprueba que `isHouse()` se vuelve verdadero con un título que empieza por "Casa " y contiene el apellido de la familia.
