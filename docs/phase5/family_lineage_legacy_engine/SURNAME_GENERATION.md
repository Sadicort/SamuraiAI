# Generación de apellidos

**Código:** `living/family/naming/generator/SurnameGenerator.java`, `SurnameOrigin.java`, `GivenNameGenerator.java`.

## Estrategias (`SurnameOrigin`)

```
CURATED, COMPOUND, TOPONYMIC, OCCUPATIONAL, ANCESTRAL, SYMBOLIC, HONORIFIC, HISTORICAL, GENERATED
```

De estas, tres tienen código que las produce hoy; el resto son categorías del vocabulario del motor para clasificar un apellido según cómo llegó a existir (a mano, por comando, o en una extensión futura), no generadores separados — un único punto (`SurnameGenerator.roll`) decide entre las estrategias con código.

| Estrategia | Método | Cuándo se usa |
| --- | --- | --- |
| `CURATED` | `curated(perfil, dice, key)` | recorre `curatedSurnames` de la cultura; siempre válido, es el respaldo seguro |
| `COMPOUND` | dentro de `roll(...)`: un prefijo + un sufijo de la cultura | el 40% de las tiradas lo intentan primero |
| `TOPONYMIC` | `toponymic(nombreDeLugar)` | apellido derivado de un topónimo (p. ej. el nombre de una aldea); expuesto y probado, pero **sin llamador todavía** desde `FamilyEngine` |
| `ANCESTRAL` | `ancestral(perfil, nombreDelFundador)` | apellido derivado del nombre de pila de un fundador (se le añade un sufijo de la cultura en minúscula: «Aldren» + «borne» → «Aldrenborne»); igual de expuesto y probado, igual de sin llamador todavía |

Importante: hoy `FamilyEngine.branch(...)` crea la rama **con el mismo nombre de familia que el padre** (así ya funcionaba en la Fase 5; esta extensión solo le añadió que herede cultura y clan). `ancestral(...)` está listo para el día en que una rama deba llevar su propio apellido derivado de quien la fundó, pero esa decisión de diseño no se tomó aquí — cambiar el nombre de una familia existente no es gratis (afecta memoria, reputación con nombre, y a quien ya conoce ese apellido).

## El camino normal: `roll(perfil, dice, key)`

1. Con un 40% de probabilidad (tirada determinista por `key`), intenta un **compuesto**: elige un prefijo y un sufijo de la cultura, los valida (`NameQualityValidator`) y, si no pasa, **reintenta hasta 5 veces** con una clave distinta cada vez.
2. Si el 40% no sale, o los 5 intentos de compuesto fallan la validación, cae a un apellido **curado** — nunca falla, porque las listas curadas ya están garantizadas por `everyCuratedWordInEveryCulturePassesQualityOnItsOwn`.

Este diseño de reintento-y-respaldo es la razón de que el validador pueda ser estricto sin arriesgarse a que una familia se quede sin apellido: `NamingEngineTest.everyCuratedWordInEveryCulturePassesQualityOnItsOwn` prueba **todas** las combinaciones prefijo×sufijo posibles de cada cultura y confirma que menos del 10% fallaría la validación por su cuenta — el resto pasa directamente, y ese 10% es exactamente lo que el reintento existe para absorber.

## Nombre de pila (`GivenNameGenerator`)

Mucho más simple: `generate(perfil, dice, key, género)` elige de `masculineGiven`, `feminineGiven` o `neutralGiven` según `KinGender` (un género no especificado usa la lista neutra). Las listas curadas de nombres de pila ya están garantizadas por el mismo test exhaustivo que las de apellido.

## Determinismo

Tanto `roll` como `generate` son puros en función de `(semilla del Dice, key, paso)`; la `key` que usa `FamilyEngine` es normalmente el UUID del NPC o de la familia, así que repetir la simulación con la misma semilla de mundo nombra siempre igual a la misma gente (`NamingEngineTest.theSameSeedAndKeyAlwaysNameTheSamePerson`).

## Límites conocidos

- `HONORIFIC`, `SYMBOLIC`, `OCCUPATIONAL` y `HISTORICAL` son categorías reservadas de `SurnameOrigin` sin generador propio todavía. Un apellido que una familia recibe hoy es siempre `CURATED` o `COMPOUND`; `TOPONYMIC` y `ANCESTRAL` tienen código y prueba pero ningún punto de `FamilyEngine` los invoca aún.
- El origen de un apellido (`SurnameGenerator.Result.origin()`) no se persiste junto a la persona: es información del momento de generarlo, no un campo permanente de `NameRecord`. El generador de muestras del depurador (`FAMILY_NAME_DEBUGGER.md`) sí lo muestra, porque genera y descarta en el momento.
