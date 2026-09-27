# Motor de epítetos

**Código:** `living/family/naming/epithets/EpithetCategory.java`, `EpithetCatalog.java`; concesión en `FamilyEngine.considerEpithet` (personas), `considerArtifactEpithet` (reliquias) y `createClan` (fundador); `NameRecord.epithet`/`withEpithet`/`formal()`.

## Un epíteto nunca es al azar

Todo epíteto tiene que venir de una **causa real y rastreable**; `EpithetCategory` enumera exactamente cuáles:

| Categoría | Causa | Quién la concede |
| --- | --- | --- |
| `LEADERSHIP` | ser cabeza de una familia con importancia histórica ≥ 60% del umbral de casa | `considerEpithet` |
| `MASTERY` | liderar una escuela (`Lineage`) de más de una generación de maestros | `considerEpithet` |
| `MENTORSHIP` | completar un aprendizaje (`Mentorship.State.COMPLETED`) | `considerEpithet` |
| `HONOR` / `DISHONOR` | honor acumulado a nombre propio (`CauseLedger.Cause.person()`) por encima de `epithetHonorThreshold` en valor absoluto, positivo o negativo | `considerEpithet` |
| `FOUNDING` | fundar un clan | `createClan` |

Cada persona recibe **como mucho un epíteto** («si ya tiene uno, no se le concede otro» — `considerEpithet` sale de inmediato si `p.name().epithet()` no está vacío); no se le retira nunca ni se recalcula. `considerEpithet` se llama una vez al día para cada adulto vivo desde `simulate`; comprueba las causas en el orden de la tabla y se detiene en la primera que encaje.

## El texto, siempre en español, sea cual sea la cultura del nombre

`EpithetCatalog` no traduce epítetos en inglés: están escritos directamente en el estilo de la crónica real castellana («Alfonso el Sabio», «Pedro el Cruel», «Juana la Loca»), con forma masculina y femenina para cada frase (`KinGender.UNSPECIFIED` lee en masculino, la forma por defecto del español). Esto es intencional: un «Aldren Ashborne» de la cultura Ceniza del Norte que gana honor se convierte en «Aldren Ashborne, el Honorable», no en un calco de "the Honourable" — el resto del texto de Deiliora es en español y el epíteto tiene que sonar igual de natural.

La frase concreta dentro de una categoría se elige con una tirada determinista (`dice.below("epithet:" + categoría + ":" + key, ...)`), así que el mismo mundo repetido concede el mismo epíteto a la misma persona.

## Cómo se ve

`NameRecord.formal()` añade el epíteto tras el nombre completo con una coma: `"Aldren Ashborne, el Honorable"`. `full()` (usado en árboles y listados donde no cabe tanto detalle) no lo incluye — el epíteto es parte del tratamiento formal de una persona, no de su nombre corto.

## Reliquias también ganan un nombre propio

`considerArtifactEpithet(h)` (llamada por familia y por reliquia desde `simulate`) le da a una reliquia que ha pasado por bastantes manos y eventos (`h.symbolicValue() >= artifactEpithetThreshold`) un nombre evocador, compuesto de un prefijo de la cultura de su familia más el tipo de objeto en mayúscula («Ashborne» + "espada" → "Ashborne Espada", por ejemplo) — **no** todas las reliquias lo reciben, solo las que cruzan el umbral de importancia simbólica. `Heirloom.displayName()` devuelve el epíteto si lo tiene, o el nombre original si no.

## Persistencia

El epíteto de una persona viaja en `NameRecord` (campo `epithet`, ver `FamilyStorage`); el de una reliquia, en `Heirloom.epithet`. Ambos sobreviven a un reinicio (`FamilyIdentityExtensionTest.identityExtensionSurvivesARestart`).

## Pruebas

`NamingEngineTest.formalNameShowsAnEarnedEpithetNaturally` (el formato). `FamilyIdentityExtensionTest.aFamilyEarnsAHouseTitleFromRealHistoryAndAPersonEarnsAnEpithetFromRealHonour` y `dishonourEarnsADishonourableEpithetNotAnHonourableOne` (las causas de honor/deshonor, con los umbrales bajados vía `FamilySettings.builder()` para no simular años de juego). `familiesFormAClanItGrowsAndDisbandsWhenTheLastFamilyLeaves` comprueba el epíteto de fundador de clan.
