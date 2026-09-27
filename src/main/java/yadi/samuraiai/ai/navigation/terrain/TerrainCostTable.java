package yadi.samuraiai.ai.navigation.terrain;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configurable cost multipliers per terrain type and per floor material. Every multiplier is clamped
 * to at least 1: the A* heuristic assumes no step is cheaper than its geometric length, and a
 * "preferred" terrain is expressed by penalising the alternatives instead.
 */
public final class TerrainCostTable {
    private final EnumMap<TerrainType, Double> terrain = new EnumMap<>(TerrainType.class);
    private final EnumMap<Material, Double> material = new EnumMap<>(Material.class);

    public TerrainCostTable() { reset(); }

    /** Restores the built-in multipliers, before configuration overrides are applied again. */
    public synchronized TerrainCostTable reset() {
        terrain.clear(); material.clear();
        for (TerrainType type : TerrainType.values()) terrain.put(type, 1.0D);
        for (Material type : Material.values()) material.put(type, 1.0D);
        terrain.put(TerrainType.ROUGH, 1.6D); terrain.put(TerrainType.WATER, 3.0D); terrain.put(TerrainType.CLIFF, 2.2D);
        terrain.put(TerrainType.STAIRS, 1.1D); terrain.put(TerrainType.LADDER, 1.5D); terrain.put(TerrainType.BRIDGE, 1.3D);
        terrain.put(TerrainType.DOOR, 1.2D); terrain.put(TerrainType.FOREST, 1.4D); terrain.put(TerrainType.LAVA, 1000.0D);
        material.put(Material.MUD, 1.8D); material.put(Material.SAND, 1.3D); material.put(Material.SNOW, 1.3D);
        material.put(Material.LEAVES, 1.5D); material.put(Material.ICE, 1.4D);
        return this;
    }

    public static TerrainCostTable defaults() { return new TerrainCostTable(); }
    public double terrain(TerrainType type) { return terrain.getOrDefault(type, 1.0D); }
    public double material(Material type) { return material.getOrDefault(type, 1.0D); }
    public TerrainCostTable terrain(TerrainType type, double multiplier) { terrain.put(type, clamp(multiplier)); return this; }
    public TerrainCostTable material(Material type, double multiplier) { material.put(type, clamp(multiplier)); return this; }

    /** Applies "NAME=value" overrides from configuration; unknown names and bad numbers are ignored. */
    public TerrainCostTable applyOverrides(Iterable<? extends String> entries) {
        if (entries == null) return this;
        for (String entry : entries) {
            if (entry == null) continue;
            int split = entry.indexOf('=');
            if (split < 1) continue;
            String name = entry.substring(0, split).trim().toUpperCase(java.util.Locale.ROOT);
            try {
                double value = Double.parseDouble(entry.substring(split + 1).trim());
                if (!Double.isFinite(value)) continue;
                try { terrain(TerrainType.valueOf(name), value); continue; } catch (IllegalArgumentException ignored) { }
                try { material(Material.valueOf(name), value); } catch (IllegalArgumentException ignored) { }
            } catch (NumberFormatException ignored) { }
        }
        return this;
    }
    public Map<TerrainType, Double> terrainSnapshot() { return Map.copyOf(terrain); }
    public Map<Material, Double> materialSnapshot() { return Map.copyOf(material); }
    private static double clamp(double value) { return Double.isFinite(value) ? Math.max(1.0D, Math.min(10000.0D, value)) : 1.0D; }
}
