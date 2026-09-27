package yadi.samuraiai.living.core;

import java.util.Locale;
import java.util.Optional;

/**
 * Weather as the living world understands it. {@link #outdoorFactor()} is how much outdoor work and travel still happens
 * (1 = normal), {@link #wet()} whether it waters crops, {@link #severe()} whether it is a hazard for travel and harvests.
 * HAIL is prepared for later climates and is only produced when a climate lists it.
 */
public enum WeatherKind {
    SUNNY(1.0D, false, false), CLOUDY(1.0D, false, false), FOG(0.8D, false, false), RAIN(0.75D, true, false), STORM(0.35D, true, true),
    SNOW(0.55D, true, false), STRONG_WIND(0.7D, false, false), HAIL(0.4D, true, true);

    private final double outdoorFactor;
    private final boolean wet, severe;

    WeatherKind(double outdoorFactor, boolean wet, boolean severe) { this.outdoorFactor = outdoorFactor; this.wet = wet; this.severe = severe; }

    public double outdoorFactor() { return outdoorFactor; }
    public boolean wet() { return wet; }
    public boolean severe() { return severe; }

    /** The Spanish word players and prompts read. */
    public String label() {
        return switch (this) {
            case SUNNY -> "despejado"; case CLOUDY -> "nublado"; case FOG -> "niebla"; case RAIN -> "lluvia"; case STORM -> "tormenta";
            case SNOW -> "nieve"; case STRONG_WIND -> "viento fuerte"; case HAIL -> "granizo";
        };
    }

    public static Optional<WeatherKind> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
