# Debug del calendario

**Código:** `living/calendar/debug/CalendarInspector.java` (texto puro), comandos en `living/server/LivingCommand.java`, HUD en `LivingService.hud`.

| Comando | Muestra |
| --- | --- |
| `/samuraiai living status` | fecha, hora, estación, clima, temperatura, luna, festivales de la celda del jugador + recuento del mundo |
| `/samuraiai living calendar` | `CalendarInspector.overview` |
| `/samuraiai living calendar weather` | clima de la celda: actual, desde cuándo, próximo cambio, historial, minutos de ayer |
| `/samuraiai living calendar festivals` | festivales activos y próximo |
| `/samuraiai living calendar agriculture` | etapa de cada cultivo y temporadas de crecimiento de la celda |
| `/samuraiai living calendar timeline` | últimas 30 entradas de la cronología |
| `/samuraiai living hud` | (jugador) barra de acción cada `playerSignalTicks`: «12 de Uzuki · verano · 14:05 · lluvia 21 °C · luna creciente» |
| `/samuraiai living calendar advance <días>` | (op) adelanta el tiempo; procesa los días como un salto largo |
| `/samuraiai living calendar weather <tipo> <horas>` | (op) fuerza el clima de la celda del jugador |
| `/samuraiai living metrics` | incluye `CalendarInspector.metrics` |

La «capa de depuración» del calendario es textual (chat y barra de acción); el overlay de partículas (`/samuraiai living debug`) dibuja aldeas, edificios, camas y caravanas (`../living_villages_engine/DEBUG_VILLAGE_OVERLAY.md`). Con `debugLogging = true` en `samuraiai-calendar.toml` cada evento del calendario se registra en el log `SamuraiAI/EVENTS`.
