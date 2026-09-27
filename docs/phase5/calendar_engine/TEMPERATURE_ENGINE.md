# Temperature Engine

**Código:** `living/calendar/temperature/TemperatureModel.java`; entrada `CalendarEngine.temperature(celda, altitud)` y `temperatureAt(...)`.

## Fórmula

```
T = base_estación (mezclada con la siguiente en los últimos seasonBlendDays)
  − oscilación · cos(2π (hora − 4) / 24)        mínimo ~04:00, máximo ~16:00
  + offset del microclima
  − (altitud − nivel_del_mar) · lapse            solo por encima del nivel del mar
  + offset del clima
```

Offset del clima: despejado +1,5 de día / −1 de noche (las noches despejadas son más frías), nublado −0,5 de día / +0,5 de noche, niebla −1, lluvia −2, tormenta −3, nieve −4, viento −2, granizo −3,5.

`comfort(°C)` da un 0..1 de comodidad al aire libre (1 entre 12 y 26 °C) que leen las rutinas.

Sin estado: es una función pura de estación, hora, microclima, altitud y clima, así que el mismo instante siempre da la misma temperatura.

## Usos

- Nieve frente a lluvia (`WeatherEngine`).
- Heladas en la agricultura: la temperatura a las 04:00 del día anterior de cada celda (`AgricultureCalendar.onDay`).
- Horarios de aldea: por debajo de `coldThreshold` los aldeanos prefieren quedarse dentro.
- Prompts y HUD (`/samuraiai living hud`, «hace lluvia y 12 °C»).
