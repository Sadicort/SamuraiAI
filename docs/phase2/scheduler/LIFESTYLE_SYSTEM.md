# Lifestyle System

Clases `lifestyle.{Lifestyle, LifestyleCatalog}`. Un estilo de vida es **dato**: qué quiere hacer un NPC de ese tipo en cada parte del día, con qué personalidad base, qué turnos puede tener y qué grupo forma.

## Formato de línea (`samuraiai-scheduler.toml → lifestyles`)

`id;types=a,b;group=PATROL;shifts=0,12000;traits=discipline:75,loyalty:80;MORNING=GUARD:60,PATROL:50;NIGHT=SLEEP:60;...`

- `types=*` es el estilo de reserva (ningún NPC queda sin agenda).
- Pesos 0–100; una rutina sin peso en un periodo no se ofrece (salvo sesgo positivo de calendario/ánimo).
- Los errores (`rasgo`/`rutina` desconocidos, números malos) se **reportan** y cuestan solo esa entrada.

## Catálogo incorporado (mismo formato; se puede copiar y editar)

`samurai` (SQUAD; entrenar, patrullar, meditar), `guard` (PATROL; turnos 0/12000), `merchant` (MERCHANT; comerciar, socializar), `monk` (VILLAGE; rezar, meditar), `villager` (`types=*`; trabajar, socializar). El tipo de NPC (`samurai`, `guard`, `merchant`…) elige el estilo; un tipo nuevo cae en `villager`.

Pruebas: `differentKindsOfNpcLiveDifferentDays`, `aVillagerLivesAWholeDayFollowingTheTimeline`.
