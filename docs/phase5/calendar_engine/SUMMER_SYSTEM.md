# Verano

**Datos:** línea integrada `SUMMER` de `living/calendar/seasons/SeasonTable.java` (sustituible en `samuraiai-calendar.toml`, clave `seasons`).

**Meses por defecto:** Uzuki, Satsuki, Minazuki (meses 4–6).

## Valores integrados

- Temperatura base **26 °C**, oscilación diaria ±7 °C (mínimo hacia las 04:00, máximo hacia las 16:00), más el microclima, la altitud y el clima (`../calendar_engine/TEMPERATURE_ENGINE.md`).
- Pesos de clima: SUNNY 46, CLOUDY 18, RAIN 16, STORM 12, FOG 3, STRONG_WIND 5.
- Multiplicadores: vegetación 1,0; cultivos 1,0; comida 0,95; combustible 0,5; comercio 1,15; vida social 1,2; viajes 1,15; animales 1,0.

## Qué pasa en el mundo

- **Natsu Matsuri** (Festival de Verano, 20 de Satsuki, 3 días): `SOCIAL +45`, `MERCHANT +20`, algo más de patrulla nocturna; sube la demanda de pescado, arroz y tela.

- El **arroz crece** (meses 4–6); el **trigo** se cosecha en los meses 4–5; las **hierbas** se recogen de los meses 4 a 7.

- Las tormentas de verano (peso 12) son la principal amenaza para las cosechas y los caminos: cuentan como día de tormenta en la agricultura y, sobre un río, pueden provocar una **inundación** (`floodChanceInStorm` en `samuraiai-world.toml`).
