# Timeline Engine

Clases `time.Timeline`, `DayPeriod`, `CalendarEvent`, `WorldCalendar`.

## Periodos

`MORNING, AFTERNOON, EVENING, NIGHT, LATE_NIGHT, DAWN`. Inicio de cada uno en ticks del día (configurable): 1000, 6000, 11500, 14000, 18000, 22500; `dayLength`=24000. Las fronteras se **ordenan**, así que una configuración en cualquier orden (o un periodo que cruza medianoche) sigue dando un día coherente (probado). El tiempo negativo se envuelve.

API: `periodAt`, `dayTime`, `dayNumber`, `ticksUntilChange`, `progress`, `next`.

## Fuente del tiempo

`SchedulerService` usa `getDayTime()` del **Overworld** para todas las dimensiones (límite conocido). Cada NPC puede tener un **turno** (`shift`, en ticks) que desplaza su día: un guardia con turno de 12000 trabaja de noche y duerme de día.

## Calendario mundial

Líneas `nombre;every=N;offset=D;periods=A,B;bias=RUTINA:valor,...`. Incorporado: `market_day` (cada 5 días, mañana y tarde: MERCHANT +40, SOCIAL +25, WORK −10) y `festival` (cada 20 días, tarde-noche: SOCIAL +45, PRAYER +20, PATROL −10, TRAINING −20). Al empezar/terminar un evento se publica `WorldScheduleEvent`; al cambiar de periodo, `TimelineChangedEvent`.

Pruebas: `TimelineTest` (7), `theCalendarAnnouncesWhenAWorldEventStartsAndEnds`, físico `theTimelineAnnouncesPeriodsAndCommandsRespond`.
