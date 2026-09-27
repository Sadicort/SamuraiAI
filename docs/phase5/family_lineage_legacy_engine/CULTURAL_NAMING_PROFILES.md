# Perfiles culturales de nombre

**Código:** `living/family/naming/cultures/NameCulture.java` (identidad), `NameCultureProfile.java` (forma de los datos), `NameCultureCatalog.java` (el único lugar donde viven las listas).

## Las cinco culturas

| Cultura | Etiqueta (`label()`) | Orden | Inspiración |
| --- | --- | --- | --- |
| `YAMATO` | Yamato | apellido primero («Takeda Hiro») | la onomástica japonesa que ya usa el calendario y los festivales |
| `ASHEN` | Ceniza del Norte | nombre primero («Aldren Ashborne») | fortalezas caídas del norte, frío |
| `WESTERN_MARCH` | Marca Occidental | nombre primero | un reino feudal occidental |
| `OLD_FLAME` | Vieja Llama | nombre primero | un culto ceremonial del fuego, antiguo |
| `HOLLOW` | Los Huecos | nombre primero | una cultura hueca y decadente de los caídos |

Inspiradas en la fantasía oscura medieval y gótica en general — **no** copiadas de ninguna obra o franquicia concreta. `NameCulture.parse(texto)` admite el nombre en cualquier mayúscula/minúscula y lo usa la persistencia, los comandos y `FamilyRecord.cultureId`.

## Qué guarda un perfil (`NameCultureProfile`)

```
record NameCultureProfile(NameCulture id, NameRecord.Order order, String houseWord,
    List<String> masculineGiven, List<String> feminineGiven, List<String> neutralGiven,
    List<String> curatedSurnames, List<String> surnamePrefixes, List<String> surnameSuffixes,
    List<String> forbiddenSubstrings)
```

- **Nombres de pila** por género (masculino, femenino y neutro — este último para quien no tiene género de parentesco registrado).
- **Apellidos curados**: una lista completa de apellidos ya válidos, la estrategia más segura (ver `SURNAME_GENERATION.md`).
- **Prefijos y sufijos**: raíces con las que `SurnameGenerator` compone apellidos nuevos por cultura, para que dos familias de la misma cultura no repitan siempre las mismas 30 palabras.
- **`houseWord`**: la palabra con la que se llama a una familia importante (ver `HOUSE_NAMING.md`). Por ahora las cinco culturas usan **"Casa"** — es una simplificación consciente; el campo existe por cultura precisamente para poder darle a alguna una palabra propia más adelante sin tocar el resto del motor.
- **`forbiddenSubstrings`**: secuencias que esa cultura en particular no quiere ver combinadas (vacía en las cinco por ahora; el validador de calidad ya cubre longitud, repetición y pronunciabilidad para todas — ver `DARK_FANTASY_NAME_ENGINE.md`).

## Tamaño de las listas

Cada cultura de fantasía oscura tiene ~24 nombres masculinos, ~24 femeninos, 10 neutros, ~32 apellidos curados y 14 prefijos + 14 sufijos (yamato conserva sus listas originales de la Fase 5, copiadas literalmente para no cambiar el comportamiento existente). Con los compuestos que permiten prefijo × sufijo, cada cultura puede producir cientos de apellidos distintos sin repetir siempre las mismas 30 palabras curadas.

## Qué cultura recibe una familia nueva

`NamingEngine.cultureFor(dice, key, pesos)` hace una tirada ponderada, determinista por `key` (normalmente la región de origen): la misma región siempre da la misma cultura. Los pesos son configurables en `samuraiai-family.toml` (`nameCultureWeights`, por defecto `yamato:35, ashen:20, western_march:20, old_flame:15, hollow:10`); una lista vacía o con entradas irreconocibles no falla nunca — cae a ese mismo reparto por defecto. Una vez asignada, la cultura de una familia (`FamilyRecord.cultureId`) es estable: todos sus miembros la heredan (nacimiento, ramas) hasta que la familia se extingue.

## Pruebas

`NamingEngineTest`: variedad cultural real (`culturesSoundDifferentFromEachOther`), calidad de cada palabra curada y de las combinaciones prefijo+sufijo (`everyCuratedWordInEveryCulturePassesQualityOnItsOwn`), reparto ponderado y su respaldo (`theWeightedPickRespectsItsWeightsAndFallsBackWhenTheyAreEmpty`). `FamilyIdentityExtensionTest.twoFoundersInTheSameRegionCarryTheSameCultureThroughoutTheFamily` confirma que padres e hijos comparten cultura.
