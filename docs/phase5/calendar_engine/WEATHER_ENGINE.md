# Weather Engine — clima regional persistente

**Código:** `living/calendar/weather/WeatherEngine.java`, `WeatherRuntime.java`, `ClimateTable.java`, `ClimateProfile.java`; `living/core/WeatherKind.java`.

## Modelo

- El mundo se divide en **celdas de clima**: `world` (siempre existe) y una por región (`region:<uuid>`) que la World Engine registra con el microclima de su tipo de terreno (`RegionType.climate()`) y su altitud.
- Cada celda (`WeatherRuntime`) guarda el clima actual, desde cuándo, la intensidad, el próximo cambio, un historial corto y los minutos de cada clima del día actual y del anterior (para la agricultura).
- Tipos (`WeatherKind`): despejado, nublado, niebla, lluvia, tormenta, nieve, viento fuerte, granizo (el granizo solo aparece si un clima lo pide). Cada tipo tiene `outdoorFactor` (cuánto trabajo exterior y viaje sigue ocurriendo), `wet` (riega cultivos) y `severe` (peligro para viajes y cosechas).

## Cambio de clima

- Solo se mira una celda cuando **le toca cambiar**: cola de prioridad por `nextChange`; miles de celdas no cuestan nada entre cambios.
- El siguiente clima se sortea con los pesos de la estación × el factor del microclima (`mountain`: más nieve y viento; `swamp`: más niebla; `coastal`: más lluvia, niebla y tormenta…), con probabilidad `weatherPersistence` (0,35) de **mantenerse**. Dura entre `weatherMinHours` (3) y `weatherMaxHours` (16) horas.
- Lluvia con menos de 1 °C → nieve; nieve con más de 3 °C → lluvia.
- **Determinista:** cada sorteo es `Dice(seed, "weather:"+celda, nº de cambio)`, así que un mundo que se pone al día tras un reinicio tiene el mismo clima que habría tenido.
- Una celda muy atrasada (región dormida semanas) no se avanza cambio a cambio: tras `weatherMaxSteps` (200) cambios salta al presente con un único sorteo (`fastForwards`).
- `forceWeather(celda, tipo, intensidad, minutos)` (comando `/samuraiai living calendar weather <tipo> <horas>`) fija el clima y publica el cambio.

## Microclimas integrados (`climates`)

`temperate` (fallback), `forest`, `mountain` (−4 °C, 0,04 °C/bloque), `coastal` (+1 °C), `river`, `swamp` (+2 °C), `fields`.

## Efectos

`WeatherChangedEvent` → aldeas (penalización de rutinas exteriores `weatherPenalty`, `outdoorFactor` en el trabajo), economía (producción exterior), mundo (tormentas sobre ríos → inundaciones), agricultura (días buenos/secos/tormenta), prompts (clima en «TU MUNDO») y HUD.

## Cielo de Minecraft

Si `driveVanillaWeather = true` (por defecto **false**), el adaptador `living/server/LivingService` traduce el clima de la celda `world` al cielo del Overworld (`setWeatherParameters`) hasta el próximo cambio: lluvia/nieve → lluvia vanilla (la nieve la decide el bioma), tormenta/granizo → tormenta eléctrica. Un `/weather` del jugador se mantiene hasta el siguiente cambio de Deiliora.

## Pruebas

`weatherChangesDeterministicallyAndSeasonsChangeTemperature`, `forcedWeatherIsPublishedAndLasts`.
