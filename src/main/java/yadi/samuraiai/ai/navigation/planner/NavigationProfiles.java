package yadi.samuraiai.ai.navigation.planner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import yadi.samuraiai.ai.navigation.terrain.Material;
import yadi.samuraiai.ai.navigation.terrain.TerrainType;

/**
 * Routing preferences per kind of NPC. Data, not code: adding an archetype is one {@link #register} call and
 * no navigation class knows any NPC type by name. The scheduler's personality engine (phase 2.4) will derive
 * per-NPC preferences on top of these.
 */
public final class NavigationProfiles {
    private static final Map<String, PathPreferences> PROFILES = new ConcurrentHashMap<>();

    static {
        // Disciplined: dislikes mud and rough ground, values safety.
        register("samurai", PathPreferences.defaults().withMaterialBias(Material.MUD, 2.5D).withBias(TerrainType.ROUGH, 1.3D)
                .withDanger(0.15D, 70.0D));
        // A guard keeps to roads and never risks a bad drop.
        register("guard", PathPreferences.defaults().withBias(TerrainType.FOREST, 1.3D).withMaxDrop(2).withDanger(0.15D, 75.0D));
        // A merchant prefers open, safe roads and avoids anything hazardous.
        register("merchant", PathPreferences.defaults().withBias(TerrainType.FOREST, 1.8D).withBias(TerrainType.ROUGH, 1.5D)
                .withMaterialBias(Material.MUD, 2.0D).withMaxDrop(2).withDanger(0.25D, 55.0D));
    }

    private NavigationProfiles() { }

    public static void register(String typeId, PathPreferences preferences) {
        PROFILES.put(typeId.toLowerCase(java.util.Locale.ROOT), java.util.Objects.requireNonNull(preferences));
    }

    public static PathPreferences forType(String typeId) {
        return typeId == null ? PathPreferences.defaults() : PROFILES.getOrDefault(typeId.toLowerCase(java.util.Locale.ROOT), PathPreferences.defaults());
    }
}
