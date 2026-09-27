package yadi.samuraiai.ai.cognition.society;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.events.RumorConfirmedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorCreatedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorSpreadEvent;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.propagation.PropagationTask;
import yadi.samuraiai.ai.knowledge.rumors.RumorClaim;
import yadi.samuraiai.ai.knowledge.rumors.RumorEngine;
import yadi.samuraiai.ai.knowledge.rumors.RumorRecord;
import yadi.samuraiai.ai.knowledge.rumors.RumorState;
import yadi.samuraiai.ai.relationship.model.ReputationLabel;

class RumorPropagationTest {
    private final CognitionHarness h = new CognitionHarness();
    private final EntityRef kenji = h.npc("Kenji"), hanako = h.npc("Hanako"), taro = h.npc("Taro"), mika = h.npc("Mika"), yeremi = h.player("Yeremi");

    private RumorRecord witness() {
        var out = h.live(h.input(kenji, ExperienceKind.WITNESSED_ATTACK).actor(yeremi).target(taro).place(h.at("plaza", 0, 0)));
        assertNotNull(out.rumor(), "a witnessed public attack starts a rumor");
        return out.rumor();
    }

    @Test void aRumorCannotExistWithoutAnOriginOrASubject() {
        var society = h.engine.society();
        var claim = new RumorClaim(yeremi, Predicate.ATTACKED, taro, "BANDIT", 0.6, PlaceRef.unknown(), "X");
        assertThrows(IllegalArgumentException.class, () -> society.createRumor(kenji, null, null, claim, "", h.now));
        assertThrows(IllegalArgumentException.class, () -> society.createRumor(null, UUID.randomUUID(), null, claim, "", h.now));
        assertThrows(IllegalArgumentException.class, () -> society.createRumor(kenji, UUID.randomUUID(), null, new RumorClaim(null, Predicate.ATTACKED, null, "", 0.5, null, ""), "", h.now));
    }

    @Test void aWitnessedEventStartsARumorWhoseProvenanceIsTheWitnessAndTheMemory() {
        RumorRecord rumor = witness();
        assertEquals(kenji.id(), rumor.origin().id());
        assertNotNull(rumor.originMemory());
        assertTrue(h.engine.memory().runtime(kenji.id()).contains(rumor.originMemory()));
        assertEquals(RumorState.ACTIVE, rumor.state());
        assertEquals(yeremi.id(), rumor.claim().subject().id());
        assertEquals(1, h.events(RumorCreatedEvent.class).size());
        KnowledgeRecord own = h.engine.knowledge().runtime(kenji.id()).all().stream().filter(r -> rumor.id().equals(r.rumorId())).findFirst().orElseThrow();
        assertEquals(ValidationState.VERIFIED, own.state(), "the witness itself saw it: for it this is a fact, not a rumor");
    }

    @Test void informationTravelsWithADelayAndArrivesAsARumorNotAsTruth() {
        RumorRecord rumor = witness();
        assertEquals(1, h.engine.society().gossip(kenji.id(), hanako.id(), 5.0, h.engine.sourceView(), h.now));
        assertEquals(1, h.engine.society().queue().size());
        h.engine.society(h.now + 50);
        assertNull(h.engine.knowledge().runtime(hanako.id()).byKey(KnowledgeRecord.key(KnowledgeType.FACT, yeremi.id(), Predicate.ATTACKED, taro.id())), "not there yet");
        h.advance(2000);
        var deliveries = h.engine.society(h.now);
        assertEquals(1, deliveries.size());
        assertTrue(deliveries.get(0).delivered());
        KnowledgeRecord heard = h.engine.knowledge().runtime(hanako.id()).byKey(KnowledgeRecord.key(KnowledgeType.FACT, yeremi.id(), Predicate.ATTACKED, taro.id()));
        assertNotNull(heard);
        assertEquals(ValidationState.RUMOR, heard.state());
        assertEquals(rumor.id(), heard.rumorId());
        assertEquals(kenji.id(), heard.source().id());
        assertTrue(heard.confidence() < 0.4);
        assertEquals(1, rumor.hops().size());
        assertTrue(rumor.holders().contains(hanako.id()));
        assertEquals(1, h.events(RumorSpreadEvent.class).size());
        // What was heard bears on the teller's subject: the listener's reputation book now has it, with its source.
        var reputation = h.engine.relationships().reputationOf(hanako.id(), yeremi.id());
        assertFalse(reputation.isEmpty());
        assertTrue(reputation.get(0).score(ReputationLabel.BANDIT) > 0.05);
        assertTrue(reputation.get(0).sources().contains(kenji.id()));
    }

    @Test void theSamePairDoesNotRepeatItselfAndNoOneHearsTheSameRumorTwice() {
        witness();
        assertEquals(1, h.engine.society().gossip(kenji.id(), hanako.id(), 5.0, h.engine.sourceView(), h.now));
        assertEquals(0, h.engine.society().gossip(kenji.id(), hanako.id(), 5.0, h.engine.sourceView(), h.now + 10), "gossip cooldown");
        h.advance(3000);
        h.engine.society(h.now);
        h.advance(h.knowledgeSettings.gossipCooldownTicks() + 10);
        assertEquals(0, h.engine.society().gossip(kenji.id(), hanako.id(), 5.0, h.engine.sourceView(), h.now), "she already knows it");
    }

    @Test void aRumorPassesAlongAChainWeakeningAndSometimesDistortingButNeverChangingItsSubject() {
        RumorRecord rumor = witness();
        RumorEngine engine = h.engine.society().rumorEngine();
        double initial = rumor.initialMagnitude();
        double strength = rumor.strength();
        UUID from = kenji.id();
        int accepted = 0;
        boolean distorted = false;
        for (int hop = 1; hop <= 10; hop++) {
            UUID to = UUID.randomUUID();
            var transfer = engine.transfer(rumor, from, to, 0.8, h.now + hop, h.knowledgeSettings);
            if (!transfer.accepted()) break;
            accepted++;
            distorted |= transfer.transformed();
            assertTrue(rumor.claim().magnitude() >= initial * (1 - h.knowledgeSettings.distortionMax()) - 1e-9 && rumor.claim().magnitude() <= Math.min(1.0, initial * (1 + h.knowledgeSettings.distortionMax())) + 1e-9);
            assertEquals(yeremi.id(), rumor.claim().subject().id(), "a rumor never invents a subject");
            assertTrue(rumor.strength() < strength);
            strength = rumor.strength();
            from = to;
        }
        assertEquals(h.knowledgeSettings.maxHops(), accepted, "the chain has a length limit");
        assertEquals(accepted, rumor.hops().size());
        assertEquals(rumor.transformations().size() > 0, distorted);
    }

    @Test void distortionIsDeterministicSoATellingAlwaysChangesTheSameWay() {
        var claim = new RumorClaim(yeremi, Predicate.ATTACKED, taro, "BANDIT", 0.6, PlaceRef.unknown(), "X");
        UUID id = UUID.randomUUID();
        RumorRecord a = new RumorRecord(id, kenji, UUID.randomUUID(), null, claim, "", 0, 1.0), b = new RumorRecord(id, kenji, UUID.randomUUID(), null, claim, "", 0, 1.0);
        RumorEngine engine = h.engine.society().rumorEngine();
        for (int hop = 0; hop < 6; hop++) {
            UUID to = UUID.randomUUID();
            engine.transfer(a, kenji.id(), to, 0.9, 10 + hop, h.knowledgeSettings);
            engine.transfer(b, kenji.id(), to, 0.9, 10 + hop, h.knowledgeSettings);
        }
        assertEquals(a.claim().magnitude(), b.claim().magnitude(), 1e-12);
        assertEquals(a.transformations().size(), b.transformations().size());
    }

    @Test void deliveriesAreBudgetedPerTickSoNothingPropagatesGloballyInOneTick() {
        h.knowledgeSettings = KnowledgeSettings.builder().set("propagationBudget", 4).build();
        var society = h.engine.society();
        for (int i = 0; i < 10; i++) society.queue().enqueue(new PropagationTask(UUID.randomUUID(), PropagationTask.Kind.KNOWLEDGE, kenji.id(), UUID.randomUUID(), UUID.randomUUID(), h.now, 1, 0.5, ""), 500);
        assertEquals(4, society.deliverDue(h.now, h.engine.sourceView()).size());
        assertEquals(4, society.deliverDue(h.now, h.engine.sourceView()).size());
        assertEquals(2, society.deliverDue(h.now, h.engine.sourceView()).size());
        assertEquals(0, society.queue().size());
        // The queue itself is bounded too.
        for (int i = 0; i < 20; i++) society.queue().enqueue(new PropagationTask(UUID.randomUUID(), PropagationTask.Kind.KNOWLEDGE, kenji.id(), UUID.randomUUID(), UUID.randomUUID(), h.now + 999, 1, 0.5, ""), 5);
        assertEquals(5, society.queue().size());
        assertEquals(15, society.queue().dropped());
    }

    @Test void arrivalTimeGrowsWithDistanceLowTrustLowRelevanceAndCrowds() {
        var p = h.engine.society().propagation();
        var s = h.knowledgeSettings;
        long near = p.delay(5, 0.8, 0.8, 4, s), far = p.delay(200, 0.8, 0.8, 4, s), distrust = p.delay(5, 0.1, 0.8, 4, s), dull = p.delay(5, 0.8, 0.1, 4, s), crowd = p.delay(5, 0.8, 0.8, 80, s);
        assertTrue(far > near && distrust > near && dull > near && crowd > near, near + " " + far + " " + distrust + " " + dull + " " + crowd);
    }

    @Test void aConfirmedRumorRaisesTheHoldersBeliefButOnlyTheirOwnEvidenceVerifiesIt() {
        RumorRecord rumor = witness();
        h.engine.society().gossip(kenji.id(), hanako.id(), 5, h.engine.sourceView(), h.now);
        h.advance(3000);
        h.engine.society(h.now);
        String key = KnowledgeRecord.key(KnowledgeType.FACT, yeremi.id(), Predicate.ATTACKED, taro.id());
        KnowledgeRecord heard = h.engine.knowledge().runtime(hanako.id()).byKey(key);
        double before = heard.confidence();
        h.engine.society().resolveRumor(rumor.id(), true, mika.id(), h.now);
        assertEquals(RumorState.CONFIRMED, rumor.state());
        assertTrue(heard.confidence() > before);
        assertNotEquals(ValidationState.VERIFIED, heard.state(), "confirmed by someone else is not verified by me");
        assertEquals(1, h.events(RumorConfirmedEvent.class).size());
        assertFalse(rumor.open());
        // Now Hanako sees it happen herself: verified.
        h.engine.knowledge().learn(new yadi.samuraiai.ai.knowledge.model.KnowledgeEvidence(hanako.id(), KnowledgeType.FACT, yadi.samuraiai.ai.knowledge.model.KnowledgeCategory.GENERAL, yeremi, Predicate.ATTACKED, taro,
                java.util.Map.of(), LearnMethod.OBSERVATION, null, -1, PlaceRef.unknown(), Set.of(), null, null, h.now, 1, 0.5, yadi.samuraiai.ai.knowledge.model.AccessLevel.PUBLIC, null));
        assertEquals(ValidationState.VERIFIED, heard.state());
    }

    @Test void aRejectedRumorIsMarkedFalseAndItsHoldersDoubtIt() {
        RumorRecord rumor = witness();
        h.engine.society().gossip(kenji.id(), hanako.id(), 5, h.engine.sourceView(), h.now);
        h.advance(3000);
        h.engine.society(h.now);
        KnowledgeRecord heard = h.engine.knowledge().runtime(hanako.id()).byKey(KnowledgeRecord.key(KnowledgeType.FACT, yeremi.id(), Predicate.ATTACKED, taro.id()));
        double before = heard.confidence();
        h.engine.society().resolveRumor(rumor.id(), false, mika.id(), h.now);
        assertEquals(RumorState.FALSE, rumor.state());
        assertTrue(heard.confidence() < before);
        assertFalse(heard.contradictors().isEmpty());
        assertEquals(1, h.engine.knowledge().metrics().rumorsRejected.get());
    }

    @Test void rumorsNobodyRepeatsAreForgotten() {
        RumorRecord rumor = witness();
        h.advance((long) h.knowledgeSettings.rumorHalfLifeTicks() * 3);
        h.engine.society(h.now);
        assertEquals(RumorState.FORGOTTEN, rumor.state());
        assertEquals(1, h.engine.knowledge().metrics().rumorsForgotten.get());
    }

    @Test void aSourceTheListenerDoesNotTrustIsNotBelieved() {
        RumorRecord rumor = witness();
        // Make Hanako despise Kenji: trust far below the credibility floor.
        for (int i = 0; i < 4; i++) h.live(hanako, ExperienceKind.BETRAYED, kenji, null);
        h.engine.society().gossip(kenji.id(), hanako.id(), 5, h.engine.sourceView(), h.now);
        h.advance(3000);
        var deliveries = h.engine.society(h.now);
        assertEquals(1, deliveries.size());
        assertFalse(deliveries.get(0).delivered(), deliveries.get(0).note());
        assertFalse(rumor.holders().contains(hanako.id()));
    }

    @Test void theWholeChainCanBeFollowedFromTheWorldEventToTheListener() {
        RumorRecord rumor = witness();
        var steps = h.engine.trace().steps(rumor.traceId());
        var stages = steps.stream().map(s -> s.stage()).distinct().toList();
        assertTrue(stages.containsAll(List.of(yadi.samuraiai.ai.cognition.trace.TraceStage.EXPERIENCE, yadi.samuraiai.ai.cognition.trace.TraceStage.MEMORY, yadi.samuraiai.ai.cognition.trace.TraceStage.EMOTION,
                yadi.samuraiai.ai.cognition.trace.TraceStage.KNOWLEDGE, yadi.samuraiai.ai.cognition.trace.TraceStage.SOCIETY)), stages.toString());
        h.engine.society().gossip(kenji.id(), hanako.id(), 5, h.engine.sourceView(), h.now);
        h.advance(3000);
        h.engine.society(h.now);
        assertTrue(h.engine.trace().steps(rumor.traceId()).stream().anyMatch(s -> s.npcId().equals(hanako.id())), "the trace reaches the listener");
    }
}
