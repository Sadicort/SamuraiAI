package yadi.samuraiai.ai.navigation.graph;

/** Integer block position of a standing node (the block holding the NPC's feet). Minecraft-agnostic. */
public record NavPos(int x, int y, int z) {
    public static NavPos ofBlock(double x, double y, double z) {
        return new NavPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
    public static long chunkKey(int chunkX, int chunkZ) { return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32); }
    public NavPos offset(int dx, int dy, int dz) { return new NavPos(x + dx, y + dy, z + dz); }
    public int chunkX() { return x >> 4; }
    public int chunkZ() { return z >> 4; }
    public long chunkKey() { return chunkKey(x >> 4, z >> 4); }
    public double horizontalDistance(NavPos other) { return Math.sqrt(horizontalDistanceSquared(other)); }
    public double horizontalDistanceSquared(NavPos other) {
        double dx = x - other.x, dz = z - other.z;
        return dx * dx + dz * dz;
    }
    public double distance(NavPos other) {
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    /** Center of the block, where steering aims. */
    public double centerX() { return x + 0.5D; }
    public double centerZ() { return z + 0.5D; }
    @Override public String toString() { return x + "," + y + "," + z; }
}
