package yadi.samuraiai.ai.cognition.model;

/** When something happened: the persistent world clock (game ticks), the wall clock and the weather at the time. */
public record Stamp(long gameTime, long realMillis, String weather) {
    public static final long TICKS_PER_DAY = 24000L;

    public Stamp {
        gameTime = Math.max(0L, gameTime);
        weather = weather == null ? "" : weather;
    }

    public static Stamp of(long gameTime) { return new Stamp(gameTime, System.currentTimeMillis(), ""); }
    public static Stamp of(long gameTime, String weather) { return new Stamp(gameTime, System.currentTimeMillis(), weather); }

    public long day() { return gameTime / TICKS_PER_DAY; }
    public long timeOfDay() { return gameTime % TICKS_PER_DAY; }
}
