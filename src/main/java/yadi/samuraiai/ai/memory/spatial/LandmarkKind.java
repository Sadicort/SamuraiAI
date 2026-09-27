package yadi.samuraiai.ai.memory.spatial;

import java.util.Locale;

/** What sort of place a spatial node is. */
public enum LandmarkKind {
    HOME, TEMPLE, MARKET, FOREST, BRIDGE, RIVER, CAMP, CAVE, MOUNTAIN, LANDMARK, GUARD_POST, TRAINING, OTHER;

    public static LandmarkKind parse(String name) {
        if (name == null) return OTHER;
        try { return valueOf(name.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return OTHER; }
    }
}
