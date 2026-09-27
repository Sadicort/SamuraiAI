# Multi-behavior Stack

Clases `stack.{BehaviorStack, BackgroundBehavior, Channel}`.

Además de la rutina principal, un NPC lleva **comportamientos de fondo** compatibles. Canales: `MOVE, LOOK, TALK, POSTURE, PACE`. La rutina principal ocupa los suyos (caminando: MOVE; dormir/meditar/rezar/descansar: POSTURE+LOOK; socializar/comerciar: TALK+POSTURE…). Cada fondo se ofrece por prioridad y solo se conserva si su canal sigue libre.

| Fondo | Canal | Cuándo |
| --- | --- | --- |
| STAY_ALERT | LOOK | sospecha, ánimo ansioso/pánico o rutina GUARD |
| SCAN_SURROUNDINGS | LOOK | patrullando o de camino |
| KEEP_FORMATION | PACE | sigue una orden de grupo |
| GREET_NEARBY | TALK | hay gente cerca y el NPC es sociable |
| IDLE_FIDGET | POSTURE | trabajando o descansando en su sitio |

Dormido no hay fondo.

## Qué consume hoy

`STAY_ALERT`/`SCAN_SURROUNDINGS` suben la visión, el oído y la atención del `SenseProfile` del NPC (efecto real en percepción); `KEEP_FORMATION` hace que un seguidor lejos de su puesto corra. `GREET_NEARBY` e `IDLE_FIDGET` se **anuncian** en el advice, el inspector y el conflicto de canales, pero no producen aún una acción visible (límite conocido).
