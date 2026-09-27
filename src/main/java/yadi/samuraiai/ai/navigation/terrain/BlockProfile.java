package yadi.samuraiai.ai.navigation.terrain;

/**
 * What navigation needs to know about one block. Built by a world adapter; the core never sees
 * a Minecraft BlockState.
 *
 * @param topHeight collision height in blocks (0 = no collision, 1 = full block)
 */
public record BlockProfile(Material material, boolean blocksMovement, double topHeight, boolean liquid,
                           boolean climbable, boolean door, boolean loaded) {
    public static final BlockProfile AIR = new BlockProfile(Material.AIR, false, 0, false, false, false, true);
    public static final BlockProfile SOLID = new BlockProfile(Material.GROUND, true, 1, false, false, false, true);
    public static final BlockProfile ROAD = new BlockProfile(Material.ROAD, true, 1, false, false, false, true);
    public static final BlockProfile WATER = new BlockProfile(Material.WATER, false, 0, true, false, false, true);
    public static final BlockProfile LAVA = new BlockProfile(Material.LAVA, false, 0, true, false, false, true);
    public static final BlockProfile LADDER = new BlockProfile(Material.LADDER, false, 0, false, true, false, true);
    public static final BlockProfile UNLOADED = new BlockProfile(Material.UNLOADED, true, 1, false, false, false, false);
    public static final BlockProfile VOID = new BlockProfile(Material.VOID, false, 0, false, false, false, true);
    public static final BlockProfile FIRE = new BlockProfile(Material.FIRE, false, 0, false, false, false, true);
    public static final BlockProfile CACTUS = new BlockProfile(Material.DAMAGING, true, 1, false, false, false, true);
    public static final BlockProfile DOOR_WOOD = new BlockProfile(Material.DOOR_WOOD, false, 0, false, false, true, true);
    public static final BlockProfile DOOR_IRON = new BlockProfile(Material.DOOR_IRON, false, 0, false, false, true, true);
    public static final BlockProfile SLAB = new BlockProfile(Material.SLAB, true, 0.5, false, false, false, true);
    public static final BlockProfile STAIRS = new BlockProfile(Material.STAIRS, true, 1, false, false, false, true);
    public static final BlockProfile FENCE = new BlockProfile(Material.FENCE, true, 1.5, false, false, false, true);
    public static final BlockProfile LEAVES = new BlockProfile(Material.LEAVES, true, 1, false, false, false, true);
    public static final BlockProfile MUD = new BlockProfile(Material.MUD, true, 1, false, false, false, true);
    public static final BlockProfile SAND = new BlockProfile(Material.SAND, true, 1, false, false, false, true);

    /** Doors are treated as openable barriers, so their collision state is irrelevant to standing. */
    public boolean isLava() { return material == Material.LAVA; }
    public boolean isWater() { return material == Material.WATER; }
    public boolean isDamaging() { return material == Material.FIRE || material == Material.DAMAGING; }
    public boolean isIronDoor() { return material == Material.DOOR_IRON; }
    /** Blocks that shrink the walkable box without being a full obstacle: slabs, snow layers, carpets. */
    public boolean isLowStep() { return blocksMovement && topHeight > 0.05D && topHeight <= 0.6D && !liquid; }
    /** A block a body can occupy: no collision, not lava, not damaging. Water is occupiable (wading). */
    public boolean isOccupiable() { return loaded && !blocksMovement && !isLava() && !isDamaging(); }
    /** A block whose top can carry a standing body. */
    public boolean canSupport() {
        return loaded && blocksMovement && topHeight >= 0.99D && !door && !isDamaging() && material != Material.FENCE;
    }
    public boolean isFullBlock() { return blocksMovement && topHeight >= 0.99D; }
}
