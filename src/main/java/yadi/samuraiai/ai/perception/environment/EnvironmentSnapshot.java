package yadi.samuraiai.ai.perception.environment;

/**
 * Where the NPC is, environmentally: biome, height, temperature, weather, time, light and nearby hazards. Built by the
 * environment/light/weather sensors; a value, so change detection is plain {@code equals} on the parts that matter.
 */
public record EnvironmentSnapshot(String dimension, String biome, int y, double temperature, WeatherState weather, long dayTime,
                                  int lightLevel, boolean inWater, boolean nearLava, boolean skyExposed) {
    public static final EnvironmentSnapshot UNKNOWN = new EnvironmentSnapshot("unknown", "unknown", 64, 0.8D, WeatherState.CLEAR, 6000L, 15, false, false, true);

    public boolean night() { long t = Math.floorMod(dayTime, 24000L); return t >= 13000L && t < 23000L; }
    public boolean dark() { return lightLevel < 4; }
    public boolean cave() { return !skyExposed && y < 60; }
    public boolean precipitating() { return weather != WeatherState.CLEAR; }
}
