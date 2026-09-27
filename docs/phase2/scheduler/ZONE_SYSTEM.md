# Zone System

Clases `zone.{Zone, ZoneKind, ZoneRegistry, ZoneScheduler, PlaceResolver, Place}`; `world.ZoneStore`.

`ZoneKind`: `HOME, WORK, MARKET, TEMPLE, TRAINING, GUARD_POST, DINING, PLAZA, PATROL_ROUTE, REST_AREA`.

Una `Zone` tiene centro, radio, **capacidad**, **horas de apertura** (periodos), **propietario** (NPC o grupo) y, para rutas, **waypoints**.

## Reglas de elección (`ZoneScheduler.choose`)

1. Abierta en el periodo actual.
2. Si hay zonas **propias** (del NPC o de su grupo) → la más cercana, ignorando capacidad.
3. Si no, las **sin dueño** con sitio, la más cercana (penaliza la ocupación).
4. Si no hay libres, cualquiera abierta.
La ocupación se registra al **empezar** la rutina y se libera al terminar/interrumpir; un NPC nunca se considera "de más" en su propio sitio.

## Sin zonas: anillo alrededor del hogar (`PlaceResolver`)

Un mundo sin configurar sigue funcionando: dormir/despertar → hogar; patrulla → `patrolPoints` puntos en un anillo (`fallbackRing`=8) alrededor del hogar; el resto → un punto del anillo **en dirección propia de cada rutina**, para que trabajar y comer no coincidan. Sin zona ni hogar no hay lugar y la rutina no se ofrece. El hogar se **fija** la primera vez que el scheduler ve al NPC (`SchedulerService`), porque `getHome()` sin fijar cae a la posición actual.

## Alertas de zona

`ZoneScheduler.alert(zona, hasta)`: una alarma dentro de una zona la pone en alerta y sube +20 a GUARD/PATROL en ella.

## Persistencia y comandos

`data/samuraiai_zones.json` del mundo (`ZoneStore`, carga al arrancar, guarda al cambiar y al parar). `/samuraiai scheduler zone add <id> <kind> <radio> [capacidad]` (en tu posición), `zone remove <id>`, `zone claim <id> <npc|grupo>`, `zones`. Las entradas corruptas se saltan.

Pruebas: `ZoneFormationGroupTest`, `aMarketDayPullsMerchantsToTheirStalls`, `aZoneThatIsFullSendsTheNextNpcToAnotherOne`, `zonesSurviveARoundTripThroughTheirFile`; físico `theClockShapesTheDayOfAMerchant` (el mercader camina de verdad al puesto y luego a casa a dormir).
