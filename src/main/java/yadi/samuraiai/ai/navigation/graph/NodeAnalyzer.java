package yadi.samuraiai.ai.navigation.graph;

import yadi.samuraiai.ai.navigation.terrain.*;

/** Computes {@link NavNode} facts from raw block profiles: standing rules, terrain class and danger. */
final class NodeAnalyzer {
    private static final int[][] CARDINAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private final NavWorldView view;
    private final HazardAnalyzer hazards;
    private final TerrainOverrides overrides;

    NodeAnalyzer(NavWorldView view, HazardAnalyzer hazards, TerrainOverrides overrides) {
        this.view = view; this.hazards = hazards; this.overrides = overrides;
    }

    /** Cheap live check of the standing rules only (three block reads), used to spot world changes along a walked path. */
    boolean quickStandable(NavPos pos) {
        if (pos.y() < view.minY() + 1 || pos.y() >= view.maxY() - 2) return false;
        BlockProfile feet = view.profile(pos), head = view.profile(pos.x(), pos.y() + 1, pos.z());
        BlockProfile below = view.profile(pos.x(), pos.y() - 1, pos.z());
        if (!feet.loaded() || !head.loaded()) return false;
        boolean lowStep = feet.isLowStep();
        if (!feet.isOccupiable() && !lowStep) return false;
        if (!head.isOccupiable()) return false;
        if (lowStep && !view.profile(pos.x(), pos.y() + 2, pos.z()).isOccupiable()) return false;
        return lowStep || feet.climbable() || feet.isWater() || below.canSupport();
    }

    NavNode analyze(NavPos pos) {
        if (pos.y() < view.minY() + 1 || pos.y() >= view.maxY() - 2) return NavNode.unstandable(pos);
        BlockProfile feet = view.profile(pos), head = view.profile(pos.x(), pos.y() + 1, pos.z());
        BlockProfile below = view.profile(pos.x(), pos.y() - 1, pos.z());
        if (!feet.loaded() || !head.loaded()) return NavNode.unstandable(pos);
        boolean lowStep = feet.isLowStep();
        if (!feet.isOccupiable() && !lowStep) return NavNode.unstandable(pos);
        if (!head.isOccupiable()) return NavNode.unstandable(pos);
        if (lowStep && !view.profile(pos.x(), pos.y() + 2, pos.z()).isOccupiable()) return NavNode.unstandable(pos);
        boolean deep = false, supported;
        if (lowStep || feet.climbable()) supported = true;
        else if (feet.isWater()) { supported = below.canSupport(); deep = !supported; supported = true; }
        else supported = below.canSupport();
        if (!supported) return NavNode.unstandable(pos);
        Material floor = lowStep ? feet.material() : below.material();
        double danger = hazards.staticDanger(pos);
        boolean door = feet.door() || head.door();
        boolean iron = feet.isIronDoor() || head.isIronDoor();
        return new NavNode(pos, true, deep, door, iron, feet.climbable(), feet.isWater(),
                classify(pos, feet, head, floor, door, danger), floor, danger);
    }

    private TerrainType classify(NavPos pos, BlockProfile feet, BlockProfile head, Material floor, boolean door, double danger) {
        var override = overrides.terrainAt(view.dimension(), pos);
        if (override.isPresent()) return override.get();
        if (door) return TerrainType.DOOR;
        if (feet.climbable()) return TerrainType.LADDER;
        if (feet.isWater()) return TerrainType.WATER;
        if (feet.isLowStep() || floor == Material.STAIRS || floor == Material.SLAB) return TerrainType.STAIRS;
        int voidSides = 0, cliffSides = 0, uneven = 0;
        for (int[] d : CARDINAL) {
            int x = pos.x() + d[0], z = pos.z() + d[1];
            int drop = hazards.dropBelow(x, pos.y(), z);
            if (view.profile(x, pos.y(), z).isFullBlock() ) uneven++;
            if (drop >= 2) voidSides++;
            if (drop >= 3) cliffSides++;
        }
        boolean opposite = (hazards.dropBelow(pos.x() + 1, pos.y(), pos.z()) >= 2 && hazards.dropBelow(pos.x() - 1, pos.y(), pos.z()) >= 2)
                || (hazards.dropBelow(pos.x(), pos.y(), pos.z() + 1) >= 2 && hazards.dropBelow(pos.x(), pos.y(), pos.z() - 1) >= 2);
        if (opposite) return TerrainType.BRIDGE;
        if (cliffSides > 0) return TerrainType.CLIFF;
        if (leavesOverhead(pos)) return TerrainType.FOREST;
        if (uneven >= 2) return TerrainType.ROUGH;
        if (floor == Material.ROAD && danger == 0) return TerrainType.SAFE;
        return TerrainType.NORMAL;
    }

    private boolean leavesOverhead(NavPos pos) {
        int leaves = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            for (int dy = 2; dy <= 4; dy++)
                if (view.profile(pos.x() + dx, pos.y() + dy, pos.z() + dz).material() == Material.LEAVES) leaves++;
        return leaves >= 4;
    }
}
