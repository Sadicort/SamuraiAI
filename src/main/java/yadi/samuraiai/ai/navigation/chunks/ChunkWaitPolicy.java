package yadi.samuraiai.ai.navigation.chunks;

/** What to do when the next stretch of ground is not loaded: wait a bounded time, then give up. Also covers dormant NPCs. */
public record ChunkWaitPolicy(int maxWaitTicks) {
    public enum Decision { WAIT, RECALCULATE, FAIL }

    public Decision decide(long waitedTicks, boolean alternativeMightExist) {
        if (waitedTicks < maxWaitTicks / 2) return Decision.WAIT;
        if (waitedTicks < maxWaitTicks) return alternativeMightExist ? Decision.RECALCULATE : Decision.WAIT;
        return Decision.FAIL;
    }
}
