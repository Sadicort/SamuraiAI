package yadi.samuraiai.living.world.regions;

import java.util.Locale;
import java.util.Optional;

/**
 * The identity of a stretch of land. Each type has a default microclimate (read by the calendar's weather) and its own natural
 * resources and wildlife (data lines in the world settings). RUINS is prepared for future content: it can be assigned by
 * command but nothing generates it yet.
 */
public enum RegionType {
    MOUNTAINS("mountain"), FOREST("forest"), VILLAGE("temperate"), TEMPLE("temperate"), FIELDS("fields"), RIVER("river"), SWAMP("swamp"), COAST("coastal"), RUINS("temperate");

    private final String climate;
    RegionType(String climate) { this.climate = climate; }

    public String climate() { return climate; }

    public static Optional<RegionType> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
