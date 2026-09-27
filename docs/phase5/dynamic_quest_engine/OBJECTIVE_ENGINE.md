# Objective Engine

**Código:** `living/quest/objectives/ObjectiveType.java`, `ObjectiveSpec.java`, `QuestObjective.java`.

| Tipo | Se cumple con | Señal real |
| --- | --- | --- |
| TALK | hablar con el NPC (id, oficio, `giver`, `citizen`) | respuesta de diálogo → `LivingService.conversation` |
| INVESTIGATE, FOLLOW | llegar al lugar (radio) | posición del jugador cada `playerSignalTicks` (40) |
| ESCORT | la caravana llega con el jugador cerca | `CaravanArrivedEvent` + jugadores a 64 bloques |
| COMBAT | matar hostiles o un tipo de criatura (en el lugar si lo tiene) | `LivingDeathEvent` con asesino jugador |
| DEFEND | estar en el lugar mientras dura el ataque | posición; se decide al terminar el evento |
| MEDITATE | minutos quieto y agachado en el lugar | posición + postura |
| GATHER, DELIVER | entregar recursos en el asentamiento | `/samuraiai living quest deliver` |
| WAIT | que pase el tiempo | reloj |
| BUILD | preparado: el edificio lo registra la aldea al entregarse los materiales (consecuencia `BUILD_HOUSE`) | — |

Un objetivo vivo sabe qué pide, sobre qué o quién (`target`), dónde (dimensión, x, y, z, radio; NaN = en cualquier sitio), cuánto se requiere y cuánto va hecho, su estado, y si pertenece a un camino. Los lugares (`place` en la plantilla) se resuelven al crear: `settlement`, `region`, `event`, `route`, `temple`, `place`…
