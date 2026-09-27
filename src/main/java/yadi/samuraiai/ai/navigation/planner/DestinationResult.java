package yadi.samuraiai.ai.navigation.planner;

import yadi.samuraiai.ai.navigation.graph.NavPos;

public record DestinationResult(Status status, NavPos resolved, String detail) {
    public enum Status { OK, ADJUSTED, OUT_OF_WORLD, CHUNK_UNLOADED, LAVA, VOID, BLOCKED, FORBIDDEN, DANGEROUS }
    public boolean usable() { return status == Status.OK || status == Status.ADJUSTED; }
    public static DestinationResult reject(Status status, String detail) { return new DestinationResult(status, null, detail); }
}
