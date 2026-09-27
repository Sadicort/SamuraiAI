package yadi.samuraiai.living.world.engine;

import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.world.regions.RegionType;

/**
 * What the World Engine needs from outside, as interfaces the hub implements. The world never imports another engine: the
 * calendar reaches it as {@link Environment} and {@link Chronicle}, the Minecraft map as {@link RegionClassifier}.
 */
public final class WorldPorts {
    private WorldPorts() { }

    /** What kind of land is at a position (the adapter reads the biome and the height; tests answer directly). */
    public interface RegionClassifier {
        record Classification(RegionType type, String biome, double altitude) { }
        Classification classify(String dimension, double x, double z);
    }

    /** The conditions the world's own simulation reads: season, the season's animal activity, weather over a region, food surplus. */
    public interface Environment {
        Season season();
        double seasonAnimals();
        WeatherKind weather(String regionScope);
        /** How much more food the region's settlements hold than they need (1 = balanced). */
        double foodSurplus(UUID region);
    }

    /** Where world history is written (the calendar's world timeline and anniversaries). */
    public interface Chronicle {
        void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source);
        void anniversary(String kind, String subject, String title, long minute);
    }

    public static final RegionClassifier FIELDS_EVERYWHERE = (d, x, z) -> new RegionClassifier.Classification(RegionType.FIELDS, "", 64);

    public static final Environment NEUTRAL = new Environment() {
        @Override public Season season() { return Season.SPRING; }
        @Override public double seasonAnimals() { return 1.0D; }
        @Override public WeatherKind weather(String regionScope) { return WeatherKind.SUNNY; }
        @Override public double foodSurplus(UUID region) { return 1.0D; }
    };

    public static final Chronicle SILENT = new Chronicle() {
        @Override public void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source) { }
        @Override public void anniversary(String kind, String subject, String title, long minute) { }
    };
}
