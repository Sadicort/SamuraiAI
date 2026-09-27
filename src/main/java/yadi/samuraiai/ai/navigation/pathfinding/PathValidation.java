package yadi.samuraiai.ai.navigation.pathfinding;

public record PathValidation(int invalidIndex, Reason reason) {
    public enum Reason { NONE, NODE_UNSTANDABLE, EDGE_ILLEGAL, DANGEROUS, CHUNK_UNLOADED }
    public static final PathValidation VALID = new PathValidation(-1, Reason.NONE);
    public boolean valid() { return invalidIndex < 0; }
}
