# Relaciones con facciones

Una relación cuyo destino es `EntityKind.FACTION` se interpreta con `FactionRelations.standing`: puntuación = 0,5·confianza + 0,3·afinidad + 0,2·lealtad − 0,2·miedo − 0,6·rivalidad → ALLIED ≥ `factionAllied` (55), HOSTILE ≤ `factionHostile` (20), si no NEUTRAL. `SocialGraph.alliedTo` responde "¿quién es aliado de esta facción?".
