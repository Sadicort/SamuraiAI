package yadi.samuraiai.ai.scheduler.conflict;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import yadi.samuraiai.ai.scheduler.engine.Candidate;

/**
 * Settles competing demands on one NPC (its own routine, a group order, a zone rule, an event response, an emotional pull).
 * The rule is fixed and explainable: a higher priority layer wins outright; within a layer the higher score wins unless the
 * scores are within {@code margin}, when the more authoritative source wins; a final tie breaks by name so the result is
 * deterministic. Losers are reported with the reason they lost.
 */
public final class ConflictResolver {
    private final double margin;
    private final Comparator<Candidate> order;

    public ConflictResolver(double margin) {
        this.margin = margin;
        this.order = this::compare;
    }

    private int compare(Candidate a, Candidate b) {
        int layer = b.layer().compareTo(a.layer());
        if (layer != 0) return layer;
        if (Math.abs(a.score() - b.score()) > margin) return Double.compare(b.score(), a.score());
        int authority = Integer.compare(b.source().authority(), a.source().authority());
        if (authority != 0) return authority;
        int score = Double.compare(b.score(), a.score());
        return score != 0 ? score : a.key().compareTo(b.key());
    }

    /** Picks the winner among the candidates; null when there are none. */
    public Resolution resolve(List<Candidate> candidates) {
        if (candidates.isEmpty()) return null;
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(order);
        Candidate winner = sorted.get(0);
        List<Resolution.Loss> losers = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            Candidate loser = sorted.get(i);
            if (loser.key().equals(winner.key())) continue;
            losers.add(new Resolution.Loss(loser, why(winner, loser)));
        }
        return new Resolution(winner, losers);
    }

    private String why(Candidate winner, Candidate loser) {
        if (winner.layer() != loser.layer()) return "layer " + winner.layer() + " outranks " + loser.layer();
        if (Math.abs(winner.score() - loser.score()) > margin) return String.format("score %.0f beats %.0f", winner.score(), loser.score());
        if (winner.source() != loser.source()) return winner.source() + " is more authoritative than " + loser.source();
        return "tie broken by name";
    }
}
