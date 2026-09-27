package yadi.samuraiai.ai.cognition.relationship;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.relationship.engine.RelationshipEngine;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.events.FriendshipLevelChangedEvent;
import yadi.samuraiai.ai.relationship.events.HonorChangedEvent;
import yadi.samuraiai.ai.relationship.events.LoyaltyChangedEvent;
import yadi.samuraiai.ai.relationship.events.PromiseBrokenEvent;
import yadi.samuraiai.ai.relationship.events.PromiseCreatedEvent;
import yadi.samuraiai.ai.relationship.events.PromiseFulfilledEvent;
import yadi.samuraiai.ai.relationship.events.RelationshipCreatedEvent;
import yadi.samuraiai.ai.relationship.events.ReputationUpdatedEvent;
import yadi.samuraiai.ai.relationship.events.TrustChangedEvent;
import yadi.samuraiai.ai.relationship.factions.FactionRelations;
import yadi.samuraiai.ai.relationship.honor.HonorSystem;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.FactionStanding;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.HonorCategory;
import yadi.samuraiai.ai.relationship.model.PromiseRecord;
import yadi.samuraiai.ai.relationship.model.PromiseStatus;
import yadi.samuraiai.ai.relationship.model.RelationState;
import yadi.samuraiai.ai.relationship.model.RelationType;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.ReputationHearsay;
import yadi.samuraiai.ai.relationship.model.ReputationLabel;
import yadi.samuraiai.ai.relationship.model.ReputationScope;
import yadi.samuraiai.ai.relationship.model.RespectLevel;
import yadi.samuraiai.ai.relationship.model.SocialEffect;
import yadi.samuraiai.ai.relationship.model.SocialEvidence;
import yadi.samuraiai.ai.relationship.model.TrustLevel;
import yadi.samuraiai.ai.relationship.respect.RespectEngine;
import yadi.samuraiai.ai.relationship.trust.TrustEngine;

class RelationshipEngineTest {
    private final CognitionHarness h = new CognitionHarness();
    private final EntityRef kenji = h.npc("Kenji"), hanako = h.npc("Hanako"), yeremi = h.player("Yeremi");

    private RelationshipEngine rel() { return h.engine.relationships(); }
    private RelationshipSettings s() { return h.relationshipSettings; }

    private SocialEvidence ev(EntityRef npc, EntityRef other, String kind, SocialEffect effect, double weight) { return SocialEvidence.simple(npc.id(), other, kind, effect, weight, h.now); }
    private void tick(int times) { for (int i = 0; i < times; i++) { h.advance(1000); } }

    @Test void aRelationshipIsAComplexRecordWithAxesHistoryAndLinks() {
        var out = h.live(kenji, ExperienceKind.HELPED_ME, yeremi, null);
        RelationshipRecord r = out.relationship().record();
        assertTrue(out.relationship().created());
        assertEquals(yeremi.id(), r.target().id());
        assertTrue(r.trust() > 45 && r.respect() > 20 && r.affinity() > 40 && r.honor() > 50);
        assertEquals(1, r.history().size());
        assertTrue(r.memories().contains(out.memory().record().id()), "the relationship knows the memory it came from");
        assertTrue(out.memory().record().socialLinks().contains(yeremi.id()), "and the memory knows the relationship's target");
        assertEquals(1, h.events(RelationshipCreatedEvent.class).size());
        assertFalse(r.causes(Dimension.TRUST).list().isEmpty());
    }

    @Test void relationshipsAreDirectionalAndNeverAssumedSymmetric() {
        // Kenji admires Hanako; Hanako distrusts Kenji: perfectly valid.
        rel().apply(ev(kenji, hanako, "ADMIRED", new SocialEffect(30, 70, 0, 0, 0, 0, 0), 1.0));
        rel().apply(ev(hanako, kenji, "DISTRUSTED", new SocialEffect(-30, 20, 0, 5, 0, 0, 0), 1.0));
        RelationshipRecord a = rel().find(kenji.id(), hanako.id()).orElseThrow(), b = rel().find(hanako.id(), kenji.id()).orElseThrow();
        assertTrue(a.respect() > 60 && a.trust() > 50);
        assertTrue(b.trust() < 25 && b.respect() < a.respect());
        assertTrue(rel().graph().whoRespects(hanako.id(), 60).contains(kenji.id()));
        assertFalse(rel().graph().whoRespects(kenji.id(), 60).contains(hanako.id()));
        assertTrue(rel().graph().whoKnows(hanako.id()).contains(kenji.id()));
    }

    @Test void trustIsHardToWinAndEasyToLose() {
        rel().apply(ev(kenji, yeremi, "A", new SocialEffect(20, 0, 0, 0, 0, 0, 0), 1.0));
        double afterGain = rel().find(kenji.id(), yeremi.id()).orElseThrow().trust();
        rel().apply(ev(kenji, yeremi, "B", new SocialEffect(-20, 0, 0, 0, 0, 0, 0), 1.0));
        double afterLoss = rel().find(kenji.id(), yeremi.id()).orElseThrow().trust();
        assertTrue(afterLoss < s().initialTrust(), "a loss of the same size undoes more than the gain built: " + afterLoss);
        assertTrue(afterGain > s().initialTrust());
    }

    @Test void trustLevelsFollowTheConfiguredThresholds() {
        RelationshipRecord r = new RelationshipRecord(UUID.randomUUID(), kenji.id(), yeremi, 0, new double[] {35, 20, 40, 0, 0, 0, 50}, 8);
        assertEquals(TrustLevel.UNKNOWN, TrustEngine.level(r, s()));
        r.interactions(5);
        assertEquals(TrustLevel.NEUTRAL, TrustEngine.level(r, s()));
        r.set(Dimension.TRUST, 10); assertEquals(TrustLevel.SUSPICIOUS, TrustEngine.level(r, s()));
        r.set(Dimension.TRUST, 55); assertEquals(TrustLevel.TRUSTING, TrustEngine.level(r, s()));
        r.set(Dimension.TRUST, 70); assertEquals(TrustLevel.CLOSE, TrustEngine.level(r, s()));
        r.set(Dimension.TRUST, 85); assertEquals(TrustLevel.CLOSE, TrustEngine.level(r, s()));
        r.set(Dimension.TRUST, 97); assertEquals(TrustLevel.ABSOLUTE_TRUST, TrustEngine.level(r, s()));
    }

    @Test void trustChangesAndLevelCrossingsPublishEvents() {
        h.live(kenji, ExperienceKind.BETRAYED, yeremi, null);
        var changes = h.events(TrustChangedEvent.class);
        assertFalse(changes.isEmpty());
        assertEquals("SUSPICIOUS", changes.get(changes.size() - 1).newLevel());
        assertFalse(h.events(HonorChangedEvent.class).isEmpty());
    }

    @Test void respectIsIndependentOfLikingAnEnemyCanBeRespected() {
        h.live(kenji, ExperienceKind.DUEL_HONORED, yeremi, null);
        h.advance(5000);
        h.live(kenji, ExperienceKind.DUEL_HONORED, yeremi, null);
        h.live(kenji, ExperienceKind.ATTACKED_ME, yeremi, null);
        RelationshipRecord r = rel().find(kenji.id(), yeremi.id()).orElseThrow();
        assertTrue(r.respect() >= s().respectModerate(), "respect " + r.respect());
        assertTrue(r.trust() < s().trustNeutral(), "trust " + r.trust());
        assertTrue(r.rivalry() > 15);
        assertTrue(RespectEngine.level(r, s()).ordinal() >= RespectLevel.MODERATE.ordinal());
    }

    @Test void honorIsCulturalAndAnOathBreakerStaysLabelledUntilRedeemed() {
        PromiseRecord oath = rel().promise(kenji.id(), yeremi, kenji, PromiseRecord.Kind.OATH, "protect the village", h.now, 1000, true, 1.0, UUID.randomUUID());
        rel().breakPromise(kenji.id(), oath.id(), h.now + 10, null);
        RelationshipRecord r = rel().find(kenji.id(), yeremi.id()).orElseThrow();
        assertEquals(1, r.oathsBroken());
        assertEquals(HonorCategory.OATH_BREAKER, HonorSystem.category(r, s()));
        assertTrue(r.honor() < 40);
    }

    @Test void loyaltyGrowsSlowlyOnlyOnTrustAndCanBeBroken() {
        RelationshipEngine e = rel();
        e.apply(ev(kenji, yeremi, "LOYAL", new SocialEffect(0, 0, 0, 0, 30, 0, 0), 1.0));
        double withoutTrust = e.find(kenji.id(), yeremi.id()).orElseThrow().loyalty();
        e.apply(ev(kenji, hanako, "TRUST", new SocialEffect(60, 0, 0, 0, 0, 0, 0), 1.0));
        e.apply(ev(kenji, hanako, "TRUST", new SocialEffect(40, 0, 0, 0, 0, 0, 0), 1.0));
        e.apply(ev(kenji, hanako, "LOYAL", new SocialEffect(0, 0, 0, 0, 30, 0, 0), 1.0));
        double withTrust = e.find(kenji.id(), hanako.id()).orElseThrow().loyalty();
        assertTrue(withTrust > withoutTrust * 2, withTrust + " vs " + withoutTrust);
        for (int i = 0; i < 5; i++) e.apply(ev(kenji, hanako, "LOYAL", new SocialEffect(0, 0, 0, 0, 30, 0, 0), 1.0));
        e.apply(ev(kenji, hanako, "ABANDONED", new SocialEffect(0, 0, 0, 0, -80, 0, 0), 1.0));
        assertTrue(e.find(kenji.id(), hanako.id()).orElseThrow().loyaltyBroken());
        assertTrue(h.events(LoyaltyChangedEvent.class).stream().anyMatch(LoyaltyChangedEvent::broken));
    }

    @Test void friendshipAdvancesStageByStageAndNotInAnAfternoon() {
        for (int i = 0; i < 25; i++) rel().apply(ev(kenji, yeremi, "TIME", new SocialEffect(8, 4, 8, 0, 0, 0, 0), 1.0));
        assertTrue(rel().find(kenji.id(), yeremi.id()).orElseThrow().stage().ordinal() <= FriendshipStage.ACQUAINTANCE.ordinal(),
                "with no time together there is no friendship: " + rel().find(kenji.id(), yeremi.id()).orElseThrow().stage());
        // Over days the same evidence builds a real friendship, one stage at a time.
        FriendshipStage previous = rel().find(kenji.id(), yeremi.id()).orElseThrow().stage();
        for (int i = 0; i < 40; i++) {
            h.advance(12000);
            var u = rel().apply(SocialEvidence.simple(kenji.id(), yeremi, "TIME", new SocialEffect(8, 4, 8, 0, 0, 0, 0), 1.0, h.now));
            assertTrue(u.stageAfter().ordinal() - previous.ordinal() <= 1, "no jumps: " + previous + " -> " + u.stageAfter());
            previous = u.stageAfter();
        }
        assertTrue(previous.ordinal() >= FriendshipStage.FRIEND.ordinal(), "reached " + previous);
        assertTrue(previous.ordinal() < FriendshipStage.BROTHER_IN_ARMS.ordinal(), "the last stage is prepared but not enabled");
        assertFalse(h.events(FriendshipLevelChangedEvent.class).isEmpty());
        assertEquals(RelationType.FRIEND, rel().find(kenji.id(), yeremi.id()).orElseThrow().type());
    }

    @Test void aCloseFriendshipBluntsRivalry() {
        h.relationshipSettings = RelationshipSettings.builder().set("friendshipTicksPerStage", 0).build();
        for (int i = 0; i < 40; i++) rel().apply(ev(kenji, yeremi, "T", new SocialEffect(10, 6, 10, 0, 0, 0, 0), 1.0));
        RelationshipRecord friend = rel().find(kenji.id(), yeremi.id()).orElseThrow();
        assertTrue(friend.stage().ordinal() >= FriendshipStage.CLOSE_FRIEND.ordinal(), friend.stage().toString());
        rel().apply(ev(kenji, yeremi, "R", new SocialEffect(0, 0, 0, 0, 0, 30, 0), 1.0));
        rel().apply(ev(kenji, hanako, "R", new SocialEffect(0, 0, 0, 0, 0, 30, 0), 1.0));
        assertTrue(rel().find(kenji.id(), yeremi.id()).orElseThrow().rivalry() < rel().find(kenji.id(), hanako.id()).orElseThrow().rivalry());
    }

    @Test void personalityBendsHowEvidenceIsRead() {
        EntityRef cautious = h.npc("Cautious"), trusting = h.npc("Trusting");
        h.setTrait(cautious, Trait.CAUTION, 95);
        h.setTrait(trusting, Trait.CAUTION, 5);
        rel().apply(ev(cautious, yeremi, "HELP", new SocialEffect(20, 0, 0, 0, 0, 0, 0), 1.0));
        rel().apply(ev(trusting, yeremi, "HELP", new SocialEffect(20, 0, 0, 0, 0, 0, 0), 1.0));
        assertTrue(rel().find(trusting.id(), yeremi.id()).orElseThrow().trust() > rel().find(cautious.id(), yeremi.id()).orElseThrow().trust());
    }

    @Test void aPassingMoodTiltsTheReadingOnlyWithinACapAndNeverDestroysARelationship() {
        h.engine.relationships().useMoods(id -> -1.0);
        rel().apply(ev(kenji, yeremi, "HELP", new SocialEffect(20, 0, 0, 0, 0, 0, 0), 1.0));
        double gloomy = rel().find(kenji.id(), yeremi.id()).orElseThrow().trust();
        h.engine.relationships().useMoods(id -> 1.0);
        rel().apply(ev(kenji, hanako, "HELP", new SocialEffect(20, 0, 0, 0, 0, 0, 0), 1.0));
        double bright = rel().find(kenji.id(), hanako.id()).orElseThrow().trust();
        assertTrue(bright > gloomy);
        assertTrue(bright / gloomy < 1.0 + 2 * s().moodInfluenceCap() + 0.5, "bounded tilt " + bright + "/" + gloomy);
        assertTrue(gloomy > s().initialTrust(), "a good deed still helps in a bad mood");
    }

    @Test void promisesHaveALifeCycleWithConsequencesForTrustAndHonor() {
        PromiseRecord kept = rel().promise(kenji.id(), yeremi, kenji, PromiseRecord.Kind.ESCORT, "walk me home", h.now, 10000, false, 0.8, null);
        assertEquals(PromiseStatus.ACTIVE, kept.status());
        assertEquals(1, h.events(PromiseCreatedEvent.class).size());
        double before = 35;
        var outcome = rel().fulfill(kenji.id(), kept.id(), h.now + 50, null).orElseThrow();
        assertEquals(PromiseStatus.FULFILLED, kept.status());
        assertTrue(outcome.update().record().trust() > before && outcome.update().delta(Dimension.HONOR) > 0);
        assertEquals(1, h.events(PromiseFulfilledEvent.class).size());
        PromiseRecord broken = rel().promise(kenji.id(), hanako, kenji, PromiseRecord.Kind.DELIVER, "the letter", h.now, 10000, true, 0.8, null);
        var bad = rel().breakPromise(kenji.id(), broken.id(), h.now + 60, null).orElseThrow();
        assertTrue(bad.update().record().trust() < 30 && bad.update().delta(Dimension.HONOR) < 0);
        assertEquals(1, h.events(PromiseBrokenEvent.class).size());
        assertTrue(rel().fulfill(kenji.id(), kept.id(), h.now + 70, null).isEmpty(), "a resolved promise cannot be resolved twice");
    }

    @Test void anUnfulfilledPromiseExpiresAndCostsTrustMildly() {
        PromiseRecord p = rel().promise(kenji.id(), yeremi, kenji, PromiseRecord.Kind.MEET, "at dawn", h.now, 2000, false, 0.5, null);
        rel().apply(ev(kenji, yeremi, "HELLO", new SocialEffect(2, 0, 2, 0, 0, 0, 0), 0.2));
        double before = rel().find(kenji.id(), yeremi.id()).orElseThrow().trust();
        h.advance(5000);
        rel().tick(kenji.id(), h.now);
        assertEquals(PromiseStatus.EXPIRED, p.status());
        double after = rel().find(kenji.id(), yeremi.id()).orElseThrow().trust();
        assertTrue(after < before && after > before - 15, before + " -> " + after);
    }

    @Test void relationshipsCoolWithDisuseButNeverVanish() {
        for (int i = 0; i < 6; i++) rel().apply(ev(kenji, yeremi, "FRIEND", new SocialEffect(15, 5, 12, 0, 6, 0, 0), 1.0));
        rel().apply(ev(kenji, hanako, "FEAR", new SocialEffect(0, 0, 0, 60, 0, 30, 0), 1.0));
        RelationshipRecord friend = rel().find(kenji.id(), yeremi.id()).orElseThrow(), feared = rel().find(kenji.id(), hanako.id()).orElseThrow();
        double trust = friend.trust(), fear = feared.fear();
        h.advance(200_000);
        rel().tick(kenji.id(), h.now);
        assertTrue(friend.trust() < trust, "trust cooled");
        assertTrue(feared.fear() < fear, "fear eased");
        assertEquals(RelationState.COOLING, friend.state());
        h.advance(2_000_000);
        rel().tick(kenji.id(), h.now);
        assertEquals(RelationState.DORMANT, friend.state());
        assertTrue(rel().find(kenji.id(), yeremi.id()).isPresent(), "never removed by cooling");
        assertTrue(friend.trust() > 0);
    }

    @Test void reputationTravelsThroughSourcesTheNpcTrustsAndNoSourceCountsTwice() {
        h.live(kenji, ExperienceKind.CONVERSATION, hanako, null);
        for (int i = 0; i < 6; i++) h.live(kenji, ExperienceKind.HELPED_ME, hanako, null);
        EntityRef stranger = h.npc("Stranger");
        long at = h.now;
        // A reliable, trusted source: believed.
        assertTrue(rel().hear(new ReputationHearsay(kenji.id(), yeremi, ReputationLabel.TRAITOR, 0.8, ReputationScope.LOCAL, "", hanako, UUID.randomUUID(), false, false, at, null), 0.85, 0.7, 0.6));
        double first = rel().reputationOf(kenji.id(), yeremi.id()).get(0).score(ReputationLabel.TRAITOR);
        assertTrue(first > 0.3, "score " + first);
        // The same source repeating adds (almost) nothing...
        rel().hear(new ReputationHearsay(kenji.id(), yeremi, ReputationLabel.TRAITOR, 0.8, ReputationScope.LOCAL, "", hanako, UUID.randomUUID(), false, false, at + 10, null), 0.85, 0.7, 0.6);
        double repeated = rel().reputationOf(kenji.id(), yeremi.id()).get(0).score(ReputationLabel.TRAITOR);
        // ...an independent source adds more.
        rel().hear(new ReputationHearsay(kenji.id(), yeremi, ReputationLabel.TRAITOR, 0.8, ReputationScope.LOCAL, "", stranger, UUID.randomUUID(), false, false, at + 20, null), 0.85, 0.7, 0.6);
        double independent = rel().reputationOf(kenji.id(), yeremi.id()).get(0).score(ReputationLabel.TRAITOR);
        assertTrue(repeated - first < independent - repeated, first + " " + repeated + " " + independent);
        // A source nobody trusts is not believed at all.
        assertFalse(rel().hear(new ReputationHearsay(kenji.id(), hanako, ReputationLabel.BANDIT, 0.9, ReputationScope.LOCAL, "", stranger, UUID.randomUUID(), false, false, at + 30, null), 0.0, 0.0, 0.0));
        assertFalse(h.events(ReputationUpdatedEvent.class).isEmpty());
    }

    @Test void anExplanationNamesTheCausesBehindAnAxis() {
        h.live(kenji, ExperienceKind.HELPED_ME, yeremi, null);
        h.advance(3000);
        h.live(kenji, ExperienceKind.LIED_TO, yeremi, null);
        PromiseRecord p = rel().promise(kenji.id(), yeremi, kenji, PromiseRecord.Kind.GENERIC, "meet", h.now, 100, false, 0.8, null);
        rel().breakPromise(kenji.id(), p.id(), h.now + 5, null);
        List<String> why = rel().explain(kenji.id(), yeremi.id(), Dimension.TRUST);
        String text = String.join("\n", why);
        assertTrue(text.contains("PROMISE_BROKEN") && text.contains("LIED_TO") && text.contains("HELPED_ME"), text);
        assertTrue(text.startsWith("TRUST ="), text);
    }

    @Test void factionsAreAlliedNeutralOrHostileFromTheSameAxes() {
        EntityRef temple = EntityRef.of(EntityRef.nameId("faction", "temple"), EntityKind.FACTION, "Templo");
        EntityRef bandits = EntityRef.of(EntityRef.nameId("faction", "bandits"), EntityKind.FACTION, "Bandoleros");
        for (int i = 0; i < 6; i++) rel().apply(ev(kenji, temple, "SERVED", new SocialEffect(20, 10, 15, 0, 15, 0, 5), 1.0));
        for (int i = 0; i < 4; i++) rel().apply(ev(kenji, bandits, "ROBBED", new SocialEffect(-20, -10, -15, 20, -10, 25, -10), 1.0));
        assertEquals(FactionStanding.ALLIED, FactionRelations.standing(rel().find(kenji.id(), temple.id()).orElseThrow(), s()));
        assertEquals(FactionStanding.HOSTILE, FactionRelations.standing(rel().find(kenji.id(), bandits.id()).orElseThrow(), s()));
        assertTrue(rel().graph().alliedTo(temple.id(), s().factionAllied()).contains(kenji.id()));
    }

    @Test void deletingAnNpcErasesTiesToItFromEveryone() {
        rel().apply(ev(kenji, hanako, "X", new SocialEffect(10, 0, 0, 0, 0, 0, 0), 1.0));
        rel().apply(ev(hanako, kenji, "X", new SocialEffect(10, 0, 0, 0, 0, 0, 0), 1.0));
        rel().forgetNpc(hanako.id());
        assertTrue(rel().find(kenji.id(), hanako.id()).isEmpty());
        assertTrue(rel().find(hanako.id(), kenji.id()).isEmpty());
        assertTrue(rel().graph().to(hanako.id()).isEmpty());
    }
}
