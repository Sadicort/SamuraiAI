# Calendar Engine (Fase 5.4)

**Código:** `living/calendar/engine/CalendarEngine.java` (implementa `living/core/WorldClock`), ajustes `CalendarSettings` (`samuraiai-calendar.toml`), instantánea `CalendarRuntime`, inspector `debug/CalendarInspector`, persistencia `persistence/CalendarStorage`.

## Qué es

La **fuente oficial del tiempo** de Deiliora. Todos los motores del mundo vivo (World, Village, Economy, Quest, Family) reciben el calendario como `WorldClock` y guardan fechas como *minuto absoluto de Deiliora* (`long`). Ningún motor lee el reloj vanilla para nada histórico: el adaptador Forge solo le pasa al calendario `gameTime`, `dayTime` y si el ciclo de luz está activo, y el calendario decide.

El motor posee:

| Parte | Clase | Documento |
| --- | --- | --- |
| Reloj monótono | `clock/DeiliClock` | `TIME_ENGINE.md` |
| Forma del calendario (meses, días, fases) | `clock/CalendarSpec`, `MonthDef` | `TIME_ENGINE.md` |
| Estaciones | `seasons/SeasonTable`, `SeasonProfile` | `SEASON_ENGINE.md` y los cuatro `*_SYSTEM.md` |
| Clima regional | `weather/WeatherEngine`, `WeatherRuntime`, `ClimateTable` | `WEATHER_ENGINE.md` |
| Temperatura | `temperature/TemperatureModel` | `TEMPERATURE_ENGINE.md` |
| Luna | `moon/MoonEngine` | `MOON_ENGINE.md` |
| Sol (duración del día) | `astronomy/SunModel` | `SEASON_ENGINE.md` |
| Festivales | `festivals/*` | `FESTIVAL_ENGINE.md` |
| Festivos fijos | `holidays/HolidayEngine` | `HOLIDAY_ENGINE.md` |
| Aniversarios | `holidays/AnniversaryEngine` | `ANNIVERSARY_ENGINE.md` |
| Calendario agrícola | `agriculture/*` | `AGRICULTURE_CALENDAR.md` |
| Cronología mundial | `timeline/WorldTimeline` | `WORLD_TIMELINE_ENGINE.md` |

## Ciclo de trabajo

1. **Cada tick del servidor** (`tick(gameTime, dayTime, sunMoves)`): el reloj avanza (aritmética barata). Si no se cruzó ningún límite de día, no se hace nada más que comprobar la fase del día y avanzar las celdas de clima *cuyo cambio ya toca* (cola de prioridad, no un barrido).
2. **Al cruzar un día** (`processDay`): se publica `DayChangedEvent` y, si procede, `YearChangedEvent`, `MonthChangedEvent`, `SeasonChangedEvent`, `MoonPhaseChangedEvent`; empiezan/terminan festivales; empiezan festivos; se anuncian aniversarios; cada celda de clima resume el día anterior para la agricultura, que puede fijar y anunciar el rendimiento de una cosecha (`HarvestOutlookEvent`).
3. **Saltos largos** (dormir, `/time add`, tiempo offline, `/samuraiai living calendar advance`): como máximo `maxDaysPerAdvance` (90) días se procesan uno a uno; los anteriores se resumen en `DayChangedEvent.skippedDays` para que un salto de un año no congele el servidor.

## Integración

- El hub `living/sim/LivingWorld` llama `calendar.tick(...)` en cada tick y reacciona a `DayChangedEvent` con el trabajo diario del mundo, las familias, el comercio y el escáner de misiones (`PHASE5_EVENT_FLOW.md`).
- Aldeas: la fase del día, el amanecer (`SunModel`), la estación, los festivales y el clima de su región deciden el horario (`../living_villages_engine/DAILY_SCHEDULE_ENGINE.md`).
- Economía: `AgricultureCalendar.productionFactor` multiplica la producción de las granjas; la estación cambia consumo de comida y combustible; los festivales cambian la demanda.
- Diálogo: `LivingService.timeOfDay/promptLines` pone la fecha, estación, clima, temperatura y luna en la sección **TU MUNDO** del prompt.

## Persistencia

Cinco secciones versionadas (`calendar/clock.json`, `weather.json`, `timeline.json`, `anniversaries.json`, `agriculture.json`); ver `../PHASE5_PERSISTENCE.md`.

## Pruebas

`src/test/java/yadi/samuraiai/living/calendar/CalendarEngineTest.java` (16 pruebas) y el GameTest `theCalendarFollowsTheServerAndEverythingIsWrittenToTheWorld`.

## Límites

- El calendario **no** cambia el clima visible de Minecraft salvo que se active `driveVanillaWeather` (por defecto `false`); ver `WEATHER_ENGINE.md`.
- No hay canal de red propio: el cliente ve la fecha en el chat/barra de acción (`/samuraiai living hud`), no en una GUI.
