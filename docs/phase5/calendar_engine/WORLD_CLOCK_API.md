# World Clock API

**Código:** `living/core/WorldClock.java` (interfaz), implementada por `living/calendar/engine/CalendarEngine`.

```java
public interface WorldClock {
    long now();                          // minuto absoluto de Deiliora, nunca retrocede
    CalendarDate date(long minute);      // fecha desglosada
    int minutesPerDay();                 // 1440
    default CalendarDate today();        // date(now())
    default long dayIndex(long minute);  // día absoluto
    default long currentDay();
    default double daysBetween(long from, long to);
    static WorldClock fixed(long minute, int minutesPerDay, LongFunction<CalendarDate> dates);   // reloj congelado para pruebas
}
```

## Reglas

1. Todos los motores reciben el reloj en el constructor (`new VillageEngine(settings, bus, calendar, seed)`…) y **solo** guardan minutos de este reloj.
2. Ningún motor lee `Level.getGameTime()`/`getDayTime()` para nada histórico. El único lector es el adaptador (`LivingService.tick`), que se los pasa al calendario.
3. Las edades se calculan con fechas de este reloj (`AgeEngine.years(nacimiento, ahora)`), **nunca con ticks de una entidad**.
4. Duraciones en minutos de Deiliora (`perDay() = minutesPerDay()`), no en ticks: un comando que adelanta el tiempo o el tiempo offline las respeta.

`LivingArchitectureTest.theCoreDependsOnNoEngine` comprueba que `living/core` no depende de ningún motor, y `onlyTheAdapterTouchesMinecraftAndTheNpcRuntime` que ningún motor toca Minecraft.
