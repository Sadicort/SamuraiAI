package yadi.samuraiai.ai.perception.engine;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.ai.perception.attention.AttentionFocus;
import yadi.samuraiai.ai.perception.attention.AttentionLevel;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.awareness.AwarenessMap;
import yadi.samuraiai.ai.perception.awareness.InterestItem;
import yadi.samuraiai.ai.perception.awareness.ThreatLevel;
import yadi.samuraiai.ai.perception.awareness.ThreatSource;
import yadi.samuraiai.ai.perception.environment.EnvironmentSnapshot;
import yadi.samuraiai.ai.perception.hearing.HeardSound;
import yadi.samuraiai.ai.perception.vision.VisualTarget;

/**
 * The immutable result of perception for one NPC at one moment: everything the Brain and scheduler may know about the
 * world through this NPC's senses. This is the perception-to-brain contract; it is what "evidence" means.
 */
public record PerceptionSnapshot(UUID npcId, long tick, AwarenessLevel awareness, AttentionLevel attention, AttentionFocus focus,
                                 List<VisualTarget> targets, List<HeardSound> recentSounds, ThreatLevel threatLevel, double threatScore,
                                 List<ThreatSource> threats, double suspicion, boolean suspicious, List<InterestItem> interests,
                                 EnvironmentSnapshot environment, AwarenessMap map, InvestigationTarget investigation, int stimuliThisPass) {

    public PerceptionSnapshot {
        targets = List.copyOf(targets); recentSounds = List.copyOf(recentSounds); threats = List.copyOf(threats); interests = List.copyOf(interests);
    }

    public static PerceptionSnapshot empty(UUID npcId, long tick) {
        return new PerceptionSnapshot(npcId, tick, AwarenessLevel.UNAWARE, AttentionLevel.NONE, null, List.of(), List.of(), ThreatLevel.SAFE, 0.0D, List.of(),
                0.0D, false, List.of(), EnvironmentSnapshot.UNKNOWN, AwarenessMap.from(new yadi.samuraiai.ai.perception.memory.PerceptionMemory(), tick), null, 0);
    }

    public List<VisualTarget> seenTargets() { return targets.stream().filter(VisualTarget::seen).toList(); }

    public Optional<VisualTarget> nearestSeen(yadi.samuraiai.ai.perception.engine.EntityClass kind) {
        return targets.stream().filter(t -> t.seen() && t.kind() == kind).min(Comparator.comparingDouble(VisualTarget::distance));
    }

    public Optional<ThreatSource> strongestThreat() { return threats.stream().max(Comparator.comparingDouble(ThreatSource::score)); }
    public Optional<InvestigationTarget> investigationTarget() { return Optional.ofNullable(investigation); }
    public boolean threatened() { return threatLevel.atLeast(ThreatLevel.WARNING); }
    public boolean hasEvidence() { return !targets.isEmpty() || !recentSounds.isEmpty() || !threats.isEmpty() || investigation != null; }
}
