package yadi.samuraiai.ai.navigation.recovery;

/**
 * Chooses the next recovery step from how many attempts have already failed. Pure and deterministic, so the
 * escalation order is testable: nudge, recalculate, alternative route, backtrack, wait, (teleport), cancel.
 */
public final class RecoveryEngine {
    private final int maxAttempts;
    private final boolean allowTeleport;

    public RecoveryEngine(int maxAttempts, boolean allowTeleport) { this.maxAttempts = maxAttempts; this.allowTeleport = allowTeleport; }

    public RecoveryAction next(int attempt) {
        if (attempt >= maxAttempts) return allowTeleport && attempt == maxAttempts ? RecoveryAction.SAFE_TELEPORT : RecoveryAction.CANCEL;
        return switch (attempt) {
            case 0 -> RecoveryAction.JUMP_NUDGE;
            case 1 -> RecoveryAction.RECALCULATE;
            case 2 -> RecoveryAction.ALTERNATIVE_ROUTE;
            case 3 -> RecoveryAction.BACKTRACK;
            case 4 -> RecoveryAction.WAIT;
            default -> RecoveryAction.ALTERNATIVE_ROUTE;
        };
    }
}
