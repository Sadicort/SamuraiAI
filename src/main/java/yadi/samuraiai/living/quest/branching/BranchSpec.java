package yadi.samuraiai.living.quest.branching;

/**
 * One way through a quest: the path, how it is described, what it takes to be offered ({@code minTrust} of the quest giver in
 * the player, 0..100; {@code minStanding} of the player in the settlement, -1..1), and how it scales the rewards.
 */
public record BranchSpec(Path path, String label, double minTrust, double minStanding, double rewardFactor) {
    public BranchSpec {
        label = label == null ? path.label() : label;
        rewardFactor = Double.isFinite(rewardFactor) ? Math.max(0.0D, rewardFactor) : 1.0D;
    }
}
