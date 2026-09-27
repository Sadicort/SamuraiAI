# Changelog — Fase 5.4 Calendar & Seasons

## Añadido

- `living/core`: `WorldClock`, `CalendarDate`, `Season`, `DayPhase`, `WeatherKind`, `MoonPhase`, `Dice`, `Provenance`, `LivingEvent`, `TickBudget`, persistencia `LivingStorage`/`StoreSection` sobre `VersionedStore` (formato `samuraiai-living`).
- `living/calendar`: reloj monótono `DeiliClock` (sol o `gameTime`, nunca retrocede), `CalendarSpec` configurable (12 × 30 días, meses japoneses), estaciones (`SeasonTable`), sol (`SunModel`), clima regional persistente y determinista (`WeatherEngine`, 7 microclimas), `TemperatureModel`, `MoonEngine` (luna llena el 15), festivales (5), festivos (Año Nuevo, Obon), aniversarios, calendario agrícola (arroz, trigo, hierbas) con rendimiento según el tiempo de la temporada, cronología mundial indexada por ámbito, tiempo offline opcional, 14 eventos, métricas, inspector, 5 secciones persistentes.
- `samuraiai-calendar.toml` (34 claves).
- Etiquetas en español para clima, luna y fases del día (`label()`), usadas en misiones, prompts y HUD.

## Integración

- El adaptador Forge alimenta el reloj cada tick y, opcionalmente, el cielo vanilla (`driveVanillaWeather`).
- Fecha, estación, clima, temperatura y luna en el prompt de diálogo (sección «TU MUNDO») y en `/samuraiai living hud`.

## Pruebas

16 pruebas unitarias (`CalendarEngineTest`) + GameTest de reloj y persistencia.

## Decisiones

- La cronología objetiva vive aquí; Knowledge conserva la historia recordada (ver `../PHASE5_DATA_OWNERSHIP.md`).
- El `Timeline` del Behavior Scheduler (periodos de luz) no se sustituye: describe la luz física; el calendario vivo le llega como sesgo de rutinas.
