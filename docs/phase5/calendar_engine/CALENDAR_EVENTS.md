# Eventos del calendario

Todos implementan `living/calendar/events/CalendarEngineEvent` (→ `living/core/LivingEvent` → `NpcEvent`), con `domain() = "calendar"` y `minute()`. Viajan por el `NPCEventBus` existente a través del hub (`PHASE5_EVENT_FLOW.md`).

| Evento | Cuándo | Quién reacciona |
| --- | --- | --- |
| `DayChangedEvent(dayIndex, year, month, day, weekday, skippedDays)` | empieza un día | hub: trabajo diario del mundo, familias, planificación de comercio, escáner de misiones |
| `DayPhaseChangedEvent(from, to)` | cambia la fase del día | aldeas (turnos, mercado) |
| `MonthChangedEvent`, `YearChangedEvent` | nuevo mes / año | cronología, familias (edades) |
| `SeasonChangedEvent(from, to, year)` | nueva estación | economía (consumo), aldeas (horarios) |
| `WeatherChangedEvent(cell, from, to, intensity)` | cambia el clima de una celda | aldeas, economía, mundo (inundaciones), adaptador (cielo vanilla si `driveVanillaWeather`) |
| `MoonPhaseChangedEvent(from, to)` | nueva fase lunar | templos |
| `FestivalStartedEvent`, `FestivalEndedEvent` | festival | aldeas (eventos de aldea), economía (demanda), adaptador (anuncio) |
| `HolidayEvent(id, name, kind, tags)` | festivo | familias (`ancestors`) |
| `AnniversaryEvent(id, kind, subject, title, years)` | aniversario | aldeas/familias (memoria) |
| `HarvestOutlookEvent(cell, crop, year, yieldFactor, verdict)` | empieza una cosecha | economía, aldeas (celebración) |
| `TimelineRecordedEvent(entryId, category, title, significance, scopes)` | entrada en la cronología | depuración |
| `TimeRewindRejectedEvent(attempts, detail)` | el reloj del mundo fue hacia atrás | depuración, métricas |
