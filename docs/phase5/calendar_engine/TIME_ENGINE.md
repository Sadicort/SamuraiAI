# Time Engine — reloj oficial y forma del calendario

**Código:** `living/calendar/clock/DeiliClock.java`, `CalendarSpec.java`, `MonthDef.java`; `living/core/CalendarDate.java`, `DayPhase.java`.

## El reloj (`DeiliClock`)

- Estado: un contador `minute` (minutos absolutos de Deiliora desde la época) y una fracción de minuto. **Solo avanza.**
- Un día tiene siempre 24 h × 60 min = 1440 minutos. Con `ticksPerDay = 24000` (por defecto) cada tick son 0,06 minutos: un día de Deiliora dura lo mismo que un día de Minecraft.
- **Fuente:** si `alignToSun = true` y el ciclo de luz está activo, la fuente es el `dayTime` del nivel (así, dormir o `/time add` avanzan la fecha exactamente como se ve en el cielo); si el ciclo está congelado, la fuente es el `gameTime` (el calendario sigue corriendo). `sunOffsetMinutes = 360` alinea el `dayTime` 0 vanilla con las 06:00.
- **Primer tick:** con fuente de luz, el reloj se alinea hacia delante con la hora que muestra el cielo; si el calendario va como mucho `SUN_TOLERANCE_MINUTES` por delante (redondeos al reiniciar) conserva su hora en vez de saltar un día entero.
- **Retrocesos:** si la fuente va hacia atrás (`/time set` a una hora anterior, copia de seguridad restaurada), el reloj **no retrocede**: con fuente de luz salta hacia delante hasta la próxima vez que el cielo muestre esa hora (cuenta `forwardJumps`); con `gameTime` espera a que la fuente lo alcance. Cada intento se cuenta (`rewindAttempts`) y se publica `TimeRewindRejectedEvent`.
- `addMinutes` (comandos, tiempo offline, pruebas) rechaza valores negativos.
- `resumeFrom` tras cargar: el primer tick solo reanuda la cuenta, no realinea.

## La forma del calendario (`CalendarSpec`)

Sin estado: convierte minutos absolutos en `CalendarDate` y viceversa. Por defecto:

| Mes | Nombre | Estación |
| --- | --- | --- |
| 1–3 | Mutsuki, Kisaragi, Yayoi | primavera |
| 4–6 | Uzuki, Satsuki, Minazuki | verano |
| 7–9 | Fumizuki, Hazuki, Nagatsuki | otoño |
| 10–12 | Kannazuki, Shimotsuki, Shiwasu | invierno |

12 meses de 30 días (360 días/año, 90 días por estación), semana de 7 días (Nichiyō … Doyō), año de época `epochYear = 100`. Las líneas de configuración usan el mismo formato que los valores integrados: `months = ["Uzuki:30:SPRING", ...]`, `weekdays = [...]`, `phases = ["DAWN:05:00", ...]`. Un calendario configurado con otros meses o duraciones funciona igual (prueba `aConfiguredCalendarChangesMonthsAndSeasons`).

`CalendarDate` contiene año, mes (con nombre), día, día del año, día de la semana, hora, minuto, estación, día de la estación y **fase del día** (`DayPhase`: madrugada 02:00, amanecer 05:00, mañana 07:00, mediodía 11:30, tarde 13:30, atardecer 17:30, noche 19:30, medianoche 00:00; configurables). `describe()` da el texto que ven jugadores y prompts: «Año 102, 15 de Uzuki (verano), 06:30».

## Configuración (`samuraiai-calendar.toml`)

`ticksPerDay`, `alignToSun`, `sunOffsetMinutes`, `epochYear`, `startDayOfYear`, `startHour` (fecha de un mundo nuevo), `months`, `weekdays`, `phases`.

## Pruebas

`defaultCalendarHasTwelveMonthsOfThirtyDaysAndNinetyDaySeasons`, `datesRoundTripAndHandleTimeBeforeTheEpoch`, `dayPhasesFollowTheConfiguredBoundaries`, `clockFollowsTheSunAndNeverRewinds`, `clockRunsOnGameTimeWhenTheDaylightCycleIsFrozen`, `timeCannotBeMovedBackwards`.
