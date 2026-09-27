# Invierno

**Datos:** línea integrada `WINTER` de `living/calendar/seasons/SeasonTable.java` (sustituible en `samuraiai-calendar.toml`, clave `seasons`).

**Meses por defecto:** Kannazuki, Shimotsuki, Shiwasu (meses 10–12).

## Valores integrados

- Temperatura base **1 °C**, oscilación diaria ±4 °C (mínimo hacia las 04:00, máximo hacia las 16:00), más el microclima, la altitud y el clima (`../calendar_engine/TEMPERATURE_ENGINE.md`).
- Pesos de clima: SUNNY 26, CLOUDY 30, RAIN 8, STORM 2, FOG 10, SNOW 20, STRONG_WIND 4.
- Multiplicadores: vegetación 0,2; cultivos 0,3; comida 1,25; combustible 2,0; comercio 0,7; vida social 0,85; viajes 0,7; animales 0,6.

## Qué pasa en el mundo

- **Fuyu Matsuri** (Festival de Invierno, 20 de Shimotsuki, 3 días): `SOCIAL +35`, `PRAYER +25`, descanso; sube la demanda de madera, arroz y tela.

- Casi no hay producción agrícola (cultivos 0,3); el trigo sembrado en otoño aguanta el invierno creciendo despacio. El consumo de **combustible se duplica** y el de comida sube un 25 %: una aldea sin reservas de leña o arroz entra en escasez, lo que dispara caravanas, contratos y misiones.

- La lluvia con temperatura por debajo de 1 °C cae como **nieve** (y la nieve por encima de 3 °C como lluvia). El frío por debajo de `coldThreshold` (5 °C, `samuraiai-village.toml`) empuja a los aldeanos a quedarse dentro; las caravanas viajan más despacio (`travel 0,7`).
