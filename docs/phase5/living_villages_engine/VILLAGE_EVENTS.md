# Eventos de aldea y del bus

## Eventos de aldea (`living/village/life/VillageEventKind.java`, `VillageEventRecord`)

| Tipo | Sesgo para todos | Sesgo extra guardias | Mercado | Civiles en casa |
| --- | --- | --- | --- | --- |
| SPECIAL_MARKET | MERCHANT +40, SOCIAL +20 | — | lo alarga | no |
| RAIN | WORK −15, REST +10, SOCIAL +5, PATROL −10 | GUARD +10 | — | no |
| ATTACK | REST +60, SOCIAL −40, WORK −40, MERCHANT −60, PRAYER −20 | GUARD +90, PATROL +70, REST −80 | lo cierra | sí |
| CELEBRATION | SOCIAL +40, EAT +15, WORK −20 | — | lo alarga | no |
| TRAINING | — | TRAINING +40 | — | no |
| PROCESSION | PRAYER +30, SOCIAL +15, WORK −10 | PATROL +10 | — | no |
| EMERGENCY | WORK −30, REST +10, SOCIAL −10 | GUARD +40, PATROL +30 | — | no |
| FESTIVAL | el del festival del calendario × `festivalBiasScale` | — | lo alarga | no |
| CEREMONY | PRAYER +35, MEDITATE +10, WORK −10 | — | — | no |

Origen (`source`): `world-event:<id>`, `festival:<id>`, `holiday:<id>`, `weather`, `schedule`. Terminar el origen termina el evento (`endEvents`).

## Eventos del bus (`living/village/events`, dominio `village`)

`VillageCreatedEvent`, `CitizenJoinedEvent`, `CitizenLeftEvent`, `ProfessionAssignedEvent`, `HomeAssignedEvent`, `HousingShortageEvent`, `BuildingRegisteredEvent`, `BuildingStateChangedEvent`, `VillagePopulationChangedEvent`, `VillageSecurityChangedEvent`, `VillageEventStartedEvent`, `VillageEventEndedEvent`, `MarketOpenedEvent`, `MarketClosedEvent`, `TempleRitualEvent`, `GuardShiftChangedEvent`, `VisitorArrivedEvent`, `VisitorLeftEvent`.

Reacciones del hub: población → mundo; ciudadano nuevo → familia, puesto, registro; ciudadano que se va → familia y registro; oficio → familia y puesto; falta de casas → misión. El adaptador anuncia a los jugadores cercanos los eventos de aldea y los cambios de seguridad (`announceToPlayers`).
