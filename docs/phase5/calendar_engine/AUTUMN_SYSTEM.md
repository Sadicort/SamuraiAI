# Otoño

**Datos:** línea integrada `AUTUMN` de `living/calendar/seasons/SeasonTable.java` (sustituible en `samuraiai-calendar.toml`, clave `seasons`).

**Meses por defecto:** Fumizuki, Hazuki, Nagatsuki (meses 7–9).

## Valores integrados

- Temperatura base **15 °C**, oscilación diaria ±6 °C (mínimo hacia las 04:00, máximo hacia las 16:00), más el microclima, la altitud y el clima (`../calendar_engine/TEMPERATURE_ENGINE.md`).
- Pesos de clima: SUNNY 32, CLOUDY 28, RAIN 22, STORM 5, FOG 9, STRONG_WIND 4.
- Multiplicadores: vegetación 0,6; cultivos 1,0; comida 1,0; combustible 1,1; comercio 1,1; vida social 1,05; viajes 1,0; animales 0,9.

## Qué pasa en el mundo

- **Obon** (13 de Fumizuki, 3 días, festivo de los ancestros: la Family Engine honra a los ancestros al recibir el `HolidayEvent` con etiqueta `ancestors`), **Tsukimi** (Festival de la Luna: empieza el primer día de luna llena dentro de 6 días a partir del 12 de Hazuki; con la luna por defecto cae el 15) y **Aki Matsuri** (Festival de la Cosecha, 5 de Nagatsuki, 3 días).

- **Cosecha del arroz** (meses 7–8): al empezar, `AgricultureCalendar` fija el rendimiento a partir del tiempo que hizo en la temporada y publica `HarvestOutlookEvent` (GREAT/NORMAL/BAD). Una gran cosecha puede hacer que las aldeas lo celebren (`celebrateGreatHarvests` en `samuraiai-living.toml`) y una mala abre misiones de escasez a través de la economía. El **trigo** se siembra en los meses 8–9.

- El otoño es la estación del comercio de excedentes: con cosechas buenas aparecen caravanas de arroz hacia las aldeas escasas.
