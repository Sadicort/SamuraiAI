package yadi.samuraiai.ai.navigation.planner;

import java.util.EnumMap;
import java.util.Map;
import yadi.samuraiai.ai.navigation.movement.MovementMode;
import yadi.samuraiai.ai.navigation.terrain.Material;
import yadi.samuraiai.ai.navigation.terrain.TerrainType;

/**
 * How one walker weighs the world. This is where personality reaches routing: a disciplined samurai
 * penalises mud, a bandit tolerates danger, a merchant prefers open roads. Bias multipliers are
 * clamped to at least 1 so the A* heuristic stays admissible.
 *
 * @param dangerWeight   cost added per danger point of a node
 * @param maxDanger      nodes above this danger are impassable for this walker
 * @param maxDrop        largest fall, in blocks, the walker accepts
 * @param terrainBias    extra multiplier per terrain type
 * @param materialBias   extra multiplier per floor material
 */
public record PathPreferences(double dangerWeight, double maxDanger, int maxDrop, boolean openWoodDoors,
                              boolean openIronDoors, boolean allowWade, boolean allowSwim,
                              Map<TerrainType, Double> terrainBias, Map<Material, Double> materialBias,
                              MovementMode mode, double bodyRadius) {
    public PathPreferences {
        dangerWeight = Double.isFinite(dangerWeight) ? Math.max(0.0D, dangerWeight) : 0.1D;
        maxDanger = Double.isFinite(maxDanger) ? Math.max(0.0D, maxDanger) : 80.0D;
        maxDrop = Math.max(0, Math.min(20, maxDrop));
        EnumMap<TerrainType, Double> terrains = new EnumMap<>(TerrainType.class);
        if (terrainBias != null) terrainBias.forEach((type, value) ->
                terrains.put(type, Double.isFinite(value) ? Math.max(1.0D, value) : 1.0D));
        terrainBias = Map.copyOf(terrains);
        EnumMap<Material, Double> floors = new EnumMap<>(Material.class);
        if (materialBias != null) materialBias.forEach((type, value) ->
                floors.put(type, Double.isFinite(value) ? Math.max(1.0D, value) : 1.0D));
        materialBias = Map.copyOf(floors);
        mode = mode == null ? MovementMode.WALK : mode;
        bodyRadius = Double.isFinite(bodyRadius) ? Math.max(0.1D, Math.min(1.5D, bodyRadius)) : 0.32D;
    }

    public static PathPreferences defaults() {
        return new PathPreferences(0.1D, 80.0D, 3, true, false, true, false, Map.of(), Map.of(), MovementMode.WALK, 0.32D);
    }
    public double bias(TerrainType type) { return terrainBias.getOrDefault(type, 1.0D); }
    public double bias(Material type) { return materialBias.getOrDefault(type, 1.0D); }

    public PathPreferences withMode(MovementMode value) {
        return new PathPreferences(dangerWeight, maxDanger, maxDrop, openWoodDoors, openIronDoors, allowWade, allowSwim, terrainBias, materialBias, value, bodyRadius);
    }
    /** Half the walker's real width plus a margin; smoothing and validation keep this much clear of walls. */
    public PathPreferences withBodyRadius(double value) {
        return new PathPreferences(dangerWeight, maxDanger, maxDrop, openWoodDoors, openIronDoors, allowWade, allowSwim, terrainBias, materialBias, mode, value);
    }
    public PathPreferences withMaxDrop(int value) {
        return new PathPreferences(dangerWeight, maxDanger, value, openWoodDoors, openIronDoors, allowWade, allowSwim, terrainBias, materialBias, mode, bodyRadius);
    }
    public PathPreferences withDanger(double weight, double max) {
        return new PathPreferences(weight, max, maxDrop, openWoodDoors, openIronDoors, allowWade, allowSwim, terrainBias, materialBias, mode, bodyRadius);
    }
    public PathPreferences withBias(TerrainType type, double value) {
        EnumMap<TerrainType, Double> copy = new EnumMap<>(TerrainType.class);
        copy.putAll(terrainBias); copy.put(type, value);
        return new PathPreferences(dangerWeight, maxDanger, maxDrop, openWoodDoors, openIronDoors, allowWade, allowSwim, copy, materialBias, mode, bodyRadius);
    }
    public PathPreferences withMaterialBias(Material type, double value) {
        EnumMap<Material, Double> copy = new EnumMap<>(Material.class);
        copy.putAll(materialBias); copy.put(type, value);
        return new PathPreferences(dangerWeight, maxDanger, maxDrop, openWoodDoors, openIronDoors, allowWade, allowSwim, terrainBias, copy, mode, bodyRadius);
    }
    public PathPreferences withDoors(boolean wood, boolean iron) {
        return new PathPreferences(dangerWeight, maxDanger, maxDrop, wood, iron, allowWade, allowSwim, terrainBias, materialBias, mode, bodyRadius);
    }
    public PathPreferences withWater(boolean wade, boolean swim) {
        return new PathPreferences(dangerWeight, maxDanger, maxDrop, openWoodDoors, openIronDoors, wade, swim, terrainBias, materialBias, mode, bodyRadius);
    }

    /** Everything that changes routing. Used in cache keys so two walkers only share a path when they would route identically. */
    public int cacheSignature() {
        return java.util.Objects.hash(dangerWeight, maxDanger, maxDrop, openWoodDoors, openIronDoors, allowWade, allowSwim,
                new java.util.TreeMap<>(terrainBias), new java.util.TreeMap<>(materialBias), Math.round(bodyRadius * 20));
    }
}
