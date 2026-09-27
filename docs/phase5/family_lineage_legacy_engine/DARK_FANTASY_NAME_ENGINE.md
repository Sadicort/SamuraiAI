# Motor de nombres de fantasía oscura

**Código:** `living/family/naming/NamingEngine.java` (orquestador), `NameRecord.java` (nombre en partes), `naming/cultures/` (culturas), `naming/generator/` (nombre de pila y apellido), `naming/validation/NameQualityValidator.java` (calidad), `naming/epithets/` (epítetos, ver `EPITHET_ENGINE.md`).

Esta es una **extensión** del motor de nombres de la Fase 5 (`NAMING_ENGINE.md`), no un motor nuevo: la clase `NamingEngine` sigue siendo el único punto de entrada; lo que cambia es que ahora reparte la generación entre cinco **culturas de nombre** (`CULTURAL_NAMING_PROFILES.md`) en vez de nombrar siempre en yamato.

## Qué resuelve

- Un nombre de pila y un apellido para una persona nueva, deterministas: `NamingEngine.givenName(dice, key, gender, cultura)` y `NamingEngine.familyName(dice, key, cultura)` — la misma `key` en el mismo mundo da siempre el mismo resultado.
- Variedad cultural real, no una sola lista con otra etiqueta (`NamingEngineTest.culturesSoundDifferentFromEachOther`: 0 nombres compartidos entre Ceniza del Norte y Marca Occidental en 100 tiradas).
- Un validador de calidad (`NameQualityValidator`) que se aplica a **todo** nombre generado, cultura incluida: nunca deja pasar algo impronunciable.
- Un orden de lectura por cultura (`NameRecord.Order`): apellido primero en yamato («Takeda Hiro»), nombre primero en las cuatro culturas de fantasía oscura («Aldren Ashborne»), resuelto por `NamingEngine.orderOf(cultura)`.

## Por qué no es solo japonés

El calendario, los festivales y los oficios de Deiliora ya hablan yamato; esta extensión debía añadir variedad de fantasía oscura **sin borrar eso**. La solución fue tratar yamato como una cultura más entre cinco, con un reparto configurable (`FamilySettings.nameCultureWeights`, por defecto `yamato:35, ashen:20, western_march:20, old_flame:15, hollow:10`) que dejó a yamato como la cultura individual más grande pero a las cuatro nuevas juntas con la mayoría — así ninguna familia nueva está obligada a sonar japonesa, y las que ya sonaban así lo siguen haciendo.

## Validador de calidad

`NameQualityValidator.acceptable(nombre, prohibidas)` rechaza:

| Regla | Motivo |
| --- | --- |
| longitud fuera de 2–18 | ni una letra ni un poema |
| caracteres que no son letra, `'` o `-` | nada de dígitos o símbolos |
| 6 o más consonantes seguidas (`CONSONANT_RUN`) | ni un compuesto en inglés llega tan lejos |
| 4 o más vocales seguidas (`VOWEL_RUN`, la «y» cuenta como vocal) | impronunciable |
| la misma letra 3 veces seguidas (`TRIPLED_LETTER`) | un nombre real dobla una letra («Anna», «Sasaki») pero nunca la triplica |
| una palabra prohibida de la cultura, o de la lista base (`mierda`, `puta`, `shit`, `fuck`) | nunca un nombre ofensivo, en español o en inglés |

`acceptablePair(nombre, apellido, prohibidas)` añade que ambos sean distintos entre sí. Las palabras **curadas** (las listas escritas a mano en `NameCultureCatalog`) están garantizadas por diseño — `NamingEngineTest.everyCuratedWordInEveryCulturePassesQualityOnItsOwn` las pasa una a una, más de 700 en total, y también prueba cada combinación prefijo+sufijo posible: menos del 10% falla por cultura (el generador reintenta y cae a una palabra curada cuando eso ocurre — ver `SURNAME_GENERATION.md`).

Este proceso de prueba exhaustiva encontró y corrigió tres problemas reales antes del lanzamiento: dos falsos positivos del validador (una regla de repetición demasiado estricta, la «y» tratada como consonante) y una colisión genuina de datos («Takashita» yamato contenía «shit» en inglés; se cambió el sufijo).

## Determinismo

Todo pasa por `Dice(seed)`: la misma semilla de mundo, la misma clave (normalmente el UUID del NPC o de la familia) y el mismo paso dan siempre el mismo nombre — `NamingEngineTest.theSameSeedAndKeyAlwaysNameTheSamePerson`. Nada aquí usa `Math.random()` ni el reloj del sistema.

## Pruebas

`src/test/java/.../naming/NamingEngineTest.java` (9 pruebas): determinismo, calidad en las cinco culturas (muestreada y exhaustiva sobre las listas curadas), distinción cultural, reparto ponderado y su valor por defecto, orden de lectura, `formal()` con epíteto, detección de nombre genérico, y los bordes del validador.
