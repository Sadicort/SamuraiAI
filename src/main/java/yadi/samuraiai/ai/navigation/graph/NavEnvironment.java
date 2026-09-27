package yadi.samuraiai.ai.navigation.graph;

/** Weather, time and biome at a position, captured once per path request. */
public record NavEnvironment(long dayTime, boolean raining, boolean thundering, String biome) {
    public static final NavEnvironment CLEAR_DAY = new NavEnvironment(6000L, false, false, "unknown");
    public boolean night() { long t = Math.floorMod(dayTime, 24000L); return t >= 13000L && t < 23000L; }
}
