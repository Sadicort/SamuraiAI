# Depuración de familias

**Código:** `living/family/debug/FamilyInspector.java`; comandos `/samuraiai living family …` (op).

| Comando | Muestra / hace |
| --- | --- |
| `person <npc>` | familia, generación, edad y etapa, padres, hijos, hermanos, hogar, jefe, oficio, maestro, discípulos, linaje, reputación, honor, casa y clan (si los tiene), reliquias, legado |
| `info <npc>` | registro de su familia |
| `tree <npc>` | árbol de su familia desde los fundadores |
| `lineage <nombre>` | árbol de maestros y discípulos |
| `heirloom show <nombre>` | historia de una reliquia |
| `succession <npc>` | candidatos a jefe de su familia con puntuación y razones |
| `suggest <npc>` | oficios sugeridos con razones |
| `legacy <npc>` | legado con sus causas |
| `audit` | problemas de la genealogía |
| `birth <a> <b> <nombre>` | registra un nacimiento |
| `partners <a> <b>` | registra una pareja (validada) |
| `mentor <maestro> <discípulo> <tipo>` | empieza un aprendizaje |
| `technique <npc> <clave> <nombre>` / `teach <clave> <de> <a>` | técnicas |
| `lineage create <tipo> <fundador> <nombre>` | funda un linaje |
| `heirloom create/give/lost/found …` | reliquias |
| `designate <npc>` / `branch <npc>` | heredero designado / nueva rama |
| `identity <npc>` | nombre en partes, casa, clan, linaje, título, epíteto, cultura y orden — ver `FAMILY_NAME_DEBUGGER.md` |
| `clan list` / `clan info <nombre>` | resumen de todos los clanes / detalle de uno |
| `clan create <npc-fundador> <nombre>` / `clan join <clan> <npc>` / `clan leave <clan> <npc>` | ciclo de vida de un clan |
| `names <cultura> <cantidad>` | tanda de nombres de muestra de esa cultura, sin persistir nada — ver `FAMILY_NAME_DEBUGGER.md` |

`debugLogging = true` en `samuraiai-family.toml` registra sus eventos.
