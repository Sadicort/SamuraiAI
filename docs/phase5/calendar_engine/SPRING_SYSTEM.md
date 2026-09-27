# Primavera

**Datos:** línea integrada `SPRING` de `living/calendar/seasons/SeasonTable.java` (sustituible en `samuraiai-calendar.toml`, clave `seasons`).

**Meses por defecto:** Mutsuki, Kisaragi, Yayoi (meses 1–3; el año empieza en primavera).

## Valores integrados

- Temperatura base **14 °C**, oscilación diaria ±6 °C (mínimo hacia las 04:00, máximo hacia las 16:00), más el microclima, la altitud y el clima (`../calendar_engine/TEMPERATURE_ENGINE.md`).
- Pesos de clima: SUNNY 34, CLOUDY 24, RAIN 26, STORM 3, FOG 9, STRONG_WIND 4.
- Multiplicadores: vegetación 0,85; cultivos 1,0; comida 1,0; combustible 1,0; comercio 1,05; vida social 1,15; viajes 1,0; animales 1,2 (época de cría: la fauna crece más rápido).

## Qué pasa en el mundo

- **Shōgatsu** (Año Nuevo, 1 de Mutsuki, 3 días, festivo) y **Hanami** (10 de Kisaragi, 4 días): los aldeanos socializan más (`SOCIAL +40`), trabajan y entrenan menos; sube la demanda de arroz, tela y hierbas.

- **Siembra del arroz** en los meses 2–3; el **trigo** sembrado en otoño sigue creciendo y se cosecha al empezar el verano; las **hierbas** se siembran en el mes 1.

- La lluvia de primavera cuenta como «día bueno» para el arroz (quiere un 35 % de días de lluvia); una helada tardía cuenta como día de helada (`frost=1.5` para el arroz).
