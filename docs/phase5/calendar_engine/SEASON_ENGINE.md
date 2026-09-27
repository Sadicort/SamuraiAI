# Season Engine

**Código:** `living/calendar/seasons/SeasonTable.java`, `SeasonProfile.java`, `living/calendar/astronomy/SunModel.java`, `living/core/Season.java`.

## Qué decide una estación

Cada estación es un `SeasonProfile` de datos que los demás motores leen como números:

| Campo | Uso |
| --- | --- |
| `baseTemperature`, `diurnalSwing` | `TemperatureModel` |
| `weather` (peso por tipo de clima) | `WeatherEngine` |
| `vegetation`, `crops` | crecimiento vegetal / cultivos |
| `food`, `fuel` | consumo por persona y día (Economy `NeedProfile`) |
| `trade` | volumen de comercio: multiplica la carga de las caravanas |
| `social` | vida social de la aldea |
| `travel` | velocidad/volumen de caravanas |
| `animals` | actividad de fauna (World `WildlifeEngine`) |

La pertenencia de los meses a las estaciones es configuración del calendario (`CalendarSpec`), no código. Las líneas tienen el formato `WINTER;temp=1;swing=4;weather=SUNNY:26,CLOUDY:30,SNOW:20;crops=0.3;food=1.25;fuel=2.0;trade=0.7;social=0.85;travel=0.7`; una línea configurada sustituye solo la estación que nombra.

## Transiciones

- `SeasonChangedEvent` al primer día de cada estación: la agricultura, los precios y las rutinas recalculan con él en lugar de consultar la fecha continuamente.
- La temperatura se **mezcla** con la de la estación siguiente durante los últimos `seasonBlendDays` (15) días, para que no haya un salto de temperatura de un día al siguiente.

## Sol (`SunModel`)

Horas de luz `12 ± daylightAmplitudeHours` (2,5 h), máximo el día `longestDayOfYear` (135) y mínimo medio año después, mediodía solar a las 12:00. `sunriseShift` desplaza el inicio del día de los horarios de aldea (la gente se levanta más tarde en invierno).

Detalle de cada estación en `SPRING_SYSTEM.md`, `SUMMER_SYSTEM.md`, `AUTUMN_SYSTEM.md`, `WINTER_SYSTEM.md`.
