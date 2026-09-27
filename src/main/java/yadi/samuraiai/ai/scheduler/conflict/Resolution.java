package yadi.samuraiai.ai.scheduler.conflict;

import java.util.List;
import yadi.samuraiai.ai.scheduler.engine.Candidate;

/** Outcome of a conflict between candidates competing for the same NPC: the winner and, for each loser, why it lost. */
public record Resolution(Candidate winner, List<Loss> losers) {
    public record Loss(Candidate candidate, String reason) { }
    public boolean contested() { return !losers.isEmpty(); }
}
