# Depurador de nombres e identidad

**Código:** `living/family/debug/FamilyInspector.java` (métodos `identity`, `clan`, `sampleNames`); comandos `/samuraiai living family identity|clan|names …` (op).

Esta es una extensión de `DEBUG_FAMILY_ENGINE.md`, no un depurador aparte: sigue siendo texto plano por el chat/consola del operador, sin GUI y sin mutar estado salvo cuando el comando lo pide explícitamente.

## Identidad de una persona

`FamilyInspector.identity(engine, npc)` — comando `/samuraiai living family identity <npc>` — muestra, una línea por campo, exactamente lo que pide la especificación: nombre de pila, apellido, nombre completo (con epíteto si lo tiene), casa, clan, linaje, título/honorífico, epíteto, cultura de nombre (con el orden de lectura) y la semilla de generación (el UUID de la persona, que es la `key` con la que se generó su nombre). Todo se lee de estado ya existente — no vuelve a tirar dados ni recalcula nada.

## Un clan, por dentro

`FamilyInspector.clan(engine, clan)` — comando `/samuraiai living family clan info <nombre>` — muestra estado, fecha de fundación, familias miembro, líder, reputación y honor, tradiciones, historia y sus aliados/rivales (si los tiene). `clan list` da un resumen de una línea por cada clan que existe (recuerda también los `DISPERSED`, nunca se borran). `clan create <npc-fundador> <nombre>`, `clan join <clan> <npc>` y `clan leave <clan> <npc>` operan el ciclo de vida descrito en `CLAN_ENGINE.md` desde consola.

## Generador de nombres de muestra

`FamilyInspector.sampleNames(cultura, semilla, cantidad)` — comando `/samuraiai living family names <cultura> <cantidad>` (hasta 40) — genera una tanda de nombres completos de una cultura para revisar cómo suenan, **sin tocar ninguna familia, persona ni fichero de guardado**. Cada línea es `Nombre Apellido (ORIGEN)`, con `ORIGEN` siendo `CURATED` o `COMPOUND` según de qué estrategia salió ese apellido concreto (ver `SURNAME_GENERATION.md`).

Es intencionadamente una herramienta de desarrollo: usa una semilla nueva en cada llamada (`System.nanoTime()`, no la semilla del mundo), así que llamarlo dos veces seguidas da tandas distintas — a propósito, para poder repasar variedad rápidamente. El nombramiento real de una persona **nunca** pasa por este método; siempre pasa por `FamilyEngine.adopt(...)`, que sí usa la semilla determinista del mundo (ver `NPC_CREATOR_NAMING_INTEGRATION.md`).

## Tabla de comandos añadidos

| Comando | Muestra / hace |
| --- | --- |
| `identity <npc>` | nombre en partes, casa, clan, linaje, título, epíteto, cultura y orden, semilla |
| `clan list` | resumen de todos los clanes, dispersos incluidos |
| `clan info <nombre>` | detalle de un clan |
| `clan create <npc-fundador> <nombre>` | funda un clan sobre la familia del NPC dado |
| `clan join <clan> <npc>` | la familia del NPC se une a ese clan |
| `clan leave <clan> <npc>` | la familia del NPC deja ese clan |
| `names <cultura> <cantidad>` | genera nombres de muestra de esa cultura (1–40), sin persistir nada |

`debugLogging = true` en `samuraiai-family.toml` sigue registrando los eventos nuevos (`HouseTitleGrantedEvent`, `EpithetGrantedEvent`, `ArtifactNamedEvent`, los de clan) igual que el resto.
