package yadi.samuraiai.ai.cognition;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.engine.CognitionSettings;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.cognition.trace.TraceStage;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.emotion.model.RecoverySource;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.society.CommunityKind;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.retrieval.RetrievalContext;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.PromiseRecord;
import yadi.samuraiai.ai.relationship.model.TrustLevel;

/** The specification's integration scenarios, run end to end through the whole cognitive layer. */
class CognitionScenarioTest {
    private final CognitionHarness h = new CognitionHarness();
    private final EntityRef kenji = h.npc("Kenji"), hanako = h.npc("Hanako"), yeremi = h.player("Yeremi");

    private List<TraceStage> stages(UUID trace) { return h.engine.trace().steps(trace).stream().map(s -> s.stage()).toList(); }

    @Test void scenario1_aRescueBecomesMemoryGratitudeTrustAndKnowledgeThatShapeLaterAdvice() {
        var out = h.live(h.input(kenji, ExperienceKind.HELPED_ME).actor(yeremi).place(h.at("road", 10, 10)));
        assertEquals(Importance.HIGH, out.memory().record().importance());
        assertTrue(out.emotion().records().stream().anyMatch(r -> r.kind() == EmotionKind.GRATITUDE));
        assertTrue(out.emotion().records().stream().anyMatch(r -> r.kind() == EmotionKind.JOY));
        assertTrue(out.relationship().delta(Dimension.TRUST) > 5 && out.relationship().delta(Dimension.RESPECT) > 3);
        KnowledgeRecord helped = out.knowledge().stream().map(l -> l.record()).filter(r -> r.predicate() == Predicate.HELPED).findFirst().orElseThrow();
        assertEquals(yeremi.id(), helped.subject().id());
        assertEquals(kenji.id(), helped.object().id());
        var order = stages(out.traceId());
        assertTrue(order.indexOf(TraceStage.EXPERIENCE) < order.indexOf(TraceStage.MEMORY) && order.indexOf(TraceStage.MEMORY) < order.indexOf(TraceStage.EMOTION)
                && order.indexOf(TraceStage.EMOTION) < order.indexOf(TraceStage.RELATIONSHIP) && order.indexOf(TraceStage.RELATIONSHIP) < order.indexOf(TraceStage.KNOWLEDGE), order.toString());
        // Later: the Brain asks what matters now and finds a familiar friend.
        for (int i = 0; i < 3; i++) { h.advance(2000); h.live(kenji, ExperienceKind.HELPED_ME, yeremi, null); }
        var advice = h.engine.advice(kenji.id(), new RetrievalContext(h.at("road", 12, 10), List.of(yeremi.id()), null, Set.of()), h.now, "MORNING", "", 30);
        assertEquals(1, advice.familiars().size());
        assertTrue(advice.familiars().get(0).friendly() && !advice.familiars().get(0).hostile());
        assertFalse(advice.memories().isEmpty());
    }

    @Test void scenario2_aBetrayalIsCriticalAngersDistrustsLowersHonorStartsARumorAndChangesPersonalitySlowly() {
        for (int i = 0; i < 3; i++) { h.live(kenji, ExperienceKind.HELPED_ME, yeremi, null); h.advance(3000); }
        var before = h.engine.relationships().find(kenji.id(), yeremi.id()).orElseThrow();
        double trustBefore = before.trust(), honorBefore = before.honor();
        double loyalty = h.engine.ledger(kenji.id()).evolution(Trait.LOYALTY);
        var out = h.live(h.input(kenji, ExperienceKind.BETRAYED).actor(yeremi).place(h.at("gate", 3, 3)).publicEvent(true));
        assertTrue(out.kept(), h.engine.metrics().lastError);
        assertTrue(out.memory().record().importance().atLeast(Importance.CRITICAL));
        assertTrue(out.memory().record().isProtected());
        for (EmotionKind k : List.of(EmotionKind.ANGER, EmotionKind.SADNESS, EmotionKind.DISTRUST)) assertTrue(h.engine.emotions().intensity(kenji.id(), k) > 20, k.toString());
        var r = out.relationship().record();
        assertEquals(TrustLevel.SUSPICIOUS, out.relationship().trustAfter());
        assertTrue(r.trust() < trustBefore - 25 && r.honor() < honorBefore - 10);
        assertNotNull(out.rumor());
        assertEquals(out.memory().record().id(), out.rumor().originMemory());
        assertNotNull(out.history());
        double moved = h.engine.ledger(kenji.id()).evolution(Trait.LOYALTY) - loyalty;
        assertTrue(moved < 0 && moved > -3.0, "loyalty drifted slowly: " + moved);
        assertEquals(50, h.engine.ledger(kenji.id()).base(Trait.LOYALTY), "the base personality is untouched");
        String why = String.join("\n", h.engine.relationships().explain(kenji.id(), yeremi.id(), Dimension.TRUST));
        assertTrue(why.contains("BETRAYED") && why.contains("HELPED_ME"), why);
        assertTrue(stages(out.traceId()).containsAll(List.of(TraceStage.MEMORY, TraceStage.EMOTION, TraceStage.RELATIONSHIP, TraceStage.SOCIETY)));
    }

    @Test void scenario3_aDiscoveryBecomesPlaceKnowledgeAndIsTaughtToAnotherNpc() {
        var out = h.live(h.input(kenji, ExperienceKind.DISCOVERED_PLACE).place(h.at("cave", 500, 500)).context("placeName", "Cueva del norte").context("category", "CAVE"));
        KnowledgeRecord place = out.knowledge().stream().map(l -> l.record()).filter(r -> r.predicate() == Predicate.LOCATED_AT).findFirst().orElseThrow();
        assertTrue(h.engine.memory().runtime(kenji.id()).spatial().nearest(h.at("x", 505, 505), 30, null).isPresent(), "spatial memory");
        var lesson = h.engine.knowledge().teach(kenji.id(), hanako.id(), List.of(place.id()), 0.8, 0.8, 1.0, null, h.now, out.traceId());
        assertEquals(1, lesson.learnedCount());
        KnowledgeRecord learned = h.engine.knowledge().runtime(hanako.id()).byKey(place.key());
        assertEquals(kenji.id(), learned.source().id());
        assertEquals(KnowledgeCategory.CAVE, learned.category());
        assertTrue(learned.place().distance(place.place()) < 1);
        assertTrue(learned.confidence() <= place.confidence());
    }

    @Test void scenario5_aWitnessedDeathLeavesATraumaAMelancholicMoodAndRecoveryThatLeavesTheMemory() {
        EntityRef ally = h.npc("Ally"), killer = h.player("Killer");
        var out = h.live(h.input(kenji, ExperienceKind.WITNESSED_DEATH).actor(killer).target(ally).traumatic(true).place(h.at("field", 20, 20)));
        assertTrue(out.memory().record().importance().atLeast(Importance.CRITICAL));
        assertNotNull(out.emotion().trauma());
        assertTrue(h.engine.emotions().intensity(kenji.id(), EmotionKind.FEAR) > 20 && h.engine.emotions().intensity(kenji.id(), EmotionKind.SADNESS) > 30);
        h.run(kenji, 4000, 100, Activity.NONE);
        assertTrue(h.engine.emotions().mood(kenji.id()) == MoodKind.MELANCHOLIC || h.engine.emotions().mood(kenji.id()) == MoodKind.FEARFUL, h.engine.emotions().mood(kenji.id()).toString());
        double sadness = h.engine.emotions().intensity(kenji.id(), EmotionKind.SADNESS);
        for (int i = 0; i < 30; i++) { h.engine.emotions().recover(kenji.id(), RecoverySource.FRIENDSHIP, h.now); h.engine.sleep(kenji.id(), h.now); }
        h.run(kenji, 100_000, 1000, Activity.SLEEPING);
        assertTrue(h.engine.emotions().traumas(kenji.id()).get(0).progress() > 0.9);
        assertTrue(h.engine.emotions().intensity(kenji.id(), EmotionKind.SADNESS) < sadness);
        assertNotNull(h.engine.memory().runtime(kenji.id()).get(out.memory().record().id()), "the memory remains");
        assertEquals(EmotionKind.SADNESS, h.engine.memory().runtime(kenji.id()).get(out.memory().record().id()).emotion().primary());
    }

    @Test void scenario6_aTempleNpcAttendsItsTraditionAndTheCulturesHoldOnItGrows() {
        var society = h.engine.society();
        var temple = society.create("templo", "Templo", CommunityKind.TEMPLE, "temple", h.at("templo", 0, 0), 30);
        society.join(kenji.id(), temple.id(), AccessLevel.MEMBERS);
        var tradition = society.dueTradition(kenji.id(), "DAWN", "TEMPLE").orElseThrow();
        double base = tradition.strength();
        for (int i = 0; i < 5; i++) {
            h.advance(24000);
            h.live(h.input(kenji, ExperienceKind.ATTENDED_RITUAL).place(h.at("templo", 1, 1)).tag("tradition"));
            society.observeTradition(kenji.id(), tradition, h.now);
        }
        assertTrue(temple.traditions().get(tradition.id()).strength() > base);
        assertEquals(5, temple.traditions().get(tradition.id()).observed());
        assertTrue(h.engine.memory().runtime(kenji.id()).size() >= 1, "the rituals became (reinforced) memory");
        assertEquals(0.0, h.engine.ledger(kenji.id()).evolution(Trait.SPIRITUALITY), 1e-9, "a routine ritual is too small an event to change a character");
    }

    @Test void personalityHasThreeLayersAndTrivialEventsNeverMoveIt() {
        for (int i = 0; i < 30; i++) { h.live(h.input(kenji, ExperienceKind.CONVERSATION).actor(hanako)); h.advance(3000); }
        assertEquals(0.0, h.engine.ledger(kenji.id()).evolution(Trait.SOCIABILITY), 1e-9);
        // A frightening event: the temporary layer masks courage at once, the long-term layer barely moves.
        double base = h.engine.ledger(kenji.id()).longTerm(Trait.COURAGE);
        h.live(h.input(kenji, ExperienceKind.ATTACKED_ME).actor(yeremi));
        var view = h.engine.personality(kenji.id());
        assertTrue(view.get(Trait.COURAGE) < base - 1.0, "fear masks courage for now: " + view.get(Trait.COURAGE));
        assertTrue(base - h.engine.ledger(kenji.id()).longTerm(Trait.COURAGE) < 1.0, "but courage itself did not vanish");
        h.run(kenji, 300_000, 1000, Activity.NONE);
        assertEquals(h.engine.ledger(kenji.id()).longTerm(Trait.COURAGE), h.engine.personality(kenji.id()).get(Trait.COURAGE), 0.6, "the mask fades with the emotion");
        assertEquals(50, h.engine.ledger(kenji.id()).base(Trait.COURAGE));
    }

    @Test void longTermEvolutionIsRateLimitedPerDayAndCappedInTotal() {
        h.cognitionSettings = yadi.samuraiai.ai.cognition.engine.CognitionSettings.builder().set("evolutionCap", 3.0).build();
        for (int i = 0; i < 12; i++) { h.live(h.input(kenji, ExperienceKind.ATTACKED_ME).actor(yeremi)); h.advance(60); }
        double day1 = h.engine.ledger(kenji.id()).evolution(Trait.CAUTION);
        assertTrue(day1 > 0 && day1 <= h.cognitionSettings.evolutionDailyLimit() + 1e-9, "at most a couple of points a day: " + day1);
        for (int day = 0; day < 10; day++) { h.advance(24000); h.live(h.input(kenji, ExperienceKind.ATTACKED_ME).actor(yeremi)); }
        assertTrue(h.engine.ledger(kenji.id()).evolution(Trait.CAUTION) <= 3.0 + 1e-9, "capped");
        assertTrue(h.engine.ledger(kenji.id()).evolution(Trait.COURAGE) < 0);
        assertFalse(h.events(yadi.samuraiai.ai.cognition.events.PersonalityEvolvedEvent.class).isEmpty());
    }

    @Test void twoNpcsWhoLivedDifferentThingsAreNotMentallyIdentical() {
        h.live(kenji, ExperienceKind.HELPED_ME, yeremi, null);
        h.live(hanako, ExperienceKind.BETRAYED, yeremi, null);
        assertNotEquals(h.engine.relationships().find(kenji.id(), yeremi.id()).orElseThrow().trust(), h.engine.relationships().find(hanako.id(), yeremi.id()).orElseThrow().trust());
        assertNotEquals(h.engine.emotions().mood(kenji.id()), MoodKind.ANGRY);
        assertTrue(h.engine.emotions().intensity(hanako.id(), EmotionKind.DISTRUST) > h.engine.emotions().intensity(kenji.id(), EmotionKind.DISTRUST));
        assertNotEquals(h.engine.memory().runtime(kenji.id()).all().get(0).kind(), h.engine.memory().runtime(hanako.id()).all().get(0).kind());
        assertNull(h.engine.knowledge().runtime(hanako.id()).all().stream().filter(r -> r.predicate() == Predicate.HELPED).findAny().orElse(null));
    }

    @Test void theServerCanRestartAndEveryPartOfTheNpcsHistoryContinues(@org.junit.jupiter.api.io.TempDir Path dir) {
        var storage = new CognitionStorage(dir, true);
        h.engine.useStorage(storage);
        h.engine.ensureLoaded(kenji.id(), h.now);
        var society = h.engine.society();
        var temple = society.create("templo", "Templo", CommunityKind.TEMPLE, "temple", h.at("templo", 0, 0), 30);
        society.join(kenji.id(), temple.id(), AccessLevel.MEMBERS);
        // 1-2. an experience and its memory; 4. a relationship; 5. an emotion; 6. knowledge
        for (int i = 0; i < 3; i++) { h.live(h.input(kenji, ExperienceKind.HELPED_ME).actor(yeremi).place(h.at("road", 10, 10))); h.advance(2500); }
        h.live(h.input(kenji, ExperienceKind.DISCOVERED_PLACE).place(h.at("cave", 500, 500)).context("placeName", "Cueva").context("category", "CAVE"));
        var death = h.live(h.input(kenji, ExperienceKind.WITNESSED_DEATH).actor(hanako).traumatic(true).place(h.at("field", 20, 20)).publicEvent(true));
        var promise = h.engine.relationships().promise(kenji.id(), yeremi, kenji, PromiseRecord.Kind.OATH, "guard the temple", h.now, 500_000, true, 1.0, null);
        h.run(kenji, 3000, 100, Activity.NONE);
        h.engine.sleep(kenji.id(), h.now);
        long now = h.now;
        var memory = h.engine.memory().runtime(kenji.id());
        var relationship = h.engine.relationships().find(kenji.id(), yeremi.id()).orElseThrow();
        var kr = h.engine.knowledge().runtime(kenji.id());
        double trust = relationship.trust(), sadness = h.engine.emotions().intensity(kenji.id(), EmotionKind.SADNESS), cautionEvolution = h.engine.ledger(kenji.id()).evolution(Trait.CAUTION);
        MoodKind mood = h.engine.emotions().mood(kenji.id());
        int memories = memory.size(), knowledge = kr.size(), rumors = society.rumors().size();
        // 7. save; 8. "shut the server down"
        int written = h.engine.saveAll(now);
        assertTrue(written >= 5, "files written: " + written);
        // 9. restart: a brand new world, the same disk
        CognitionHarness restarted = new CognitionHarness();
        restarted.refs.putAll(h.refs);
        restarted.now = now + 5000;
        restarted.engine.useStorage(new CognitionStorage(dir, true));
        assertTrue(restarted.engine.storage().loadSociety(restarted.engine).usable());
        restarted.engine.ensureLoaded(kenji.id(), restarted.now);
        // 10. verify
        var m2 = restarted.engine.memory().runtime(kenji.id());
        assertEquals(memories, m2.size());
        assertTrue(m2.get(death.memory().record().id()).isProtected());
        assertEquals(death.traceId(), m2.get(death.memory().record().id()).origin().traceId());
        var r2 = restarted.engine.relationships().find(kenji.id(), yeremi.id()).orElseThrow();
        assertEquals(trust, r2.trust(), 1e-9);
        assertEquals(relationship.stage(), r2.stage());
        assertEquals(relationship.history().size(), r2.history().size());
        assertEquals(1, restarted.engine.relationships().runtime(kenji.id()).promises().size());
        assertTrue(restarted.engine.relationships().runtime(kenji.id()).promises().containsKey(promise.id()));
        assertTrue(restarted.engine.relationships().graph().whoKnows(yeremi.id()).contains(kenji.id()), "the social graph is rebuilt on load");
        assertEquals(mood, restarted.engine.emotions().runtime(kenji.id()).mood());
        assertEquals(1, restarted.engine.emotions().traumas(kenji.id()).size());
        assertTrue(restarted.engine.emotions().intensity(kenji.id(), EmotionKind.SADNESS) > 0);
        assertEquals(knowledge, restarted.engine.knowledge().runtime(kenji.id()).size());
        assertTrue(restarted.engine.knowledge().runtime(kenji.id()).all().stream().anyMatch(r -> r.category() == KnowledgeCategory.CAVE));
        assertEquals(cautionEvolution, restarted.engine.ledger(kenji.id()).evolution(Trait.CAUTION), 1e-9);
        assertEquals(50.0, restarted.engine.ledger(kenji.id()).base(Trait.COURAGE), 1e-9);
        assertEquals(1, restarted.engine.society().communities().size());
        assertEquals(rumors, restarted.engine.society().rumors().size());
        assertEquals(1.3, restarted.engine.society().honorScale(kenji.id()), 1e-9);
        assertFalse(restarted.engine.society().worldTimeline().isEmpty());
        // Time keeps passing on the restarted server: the mind continues rather than restarting.
        restarted.run(kenji, 60_000, 1000, Activity.NONE);
        assertTrue(restarted.engine.emotions().intensity(kenji.id(), EmotionKind.SADNESS) <= sadness + 1e-6);
        restarted.live(restarted.input(kenji, ExperienceKind.GIFT_RECEIVED).actor(yeremi));
        assertTrue(restarted.engine.relationships().find(kenji.id(), yeremi.id()).orElseThrow().interactions() > relationship.interactions() - 1);
    }

    @Test void erasingAnNpcRemovesItsMindItsFilesAndEveryonesTiesToIt(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var storage = new CognitionStorage(dir, false);
        h.engine.useStorage(storage);
        h.engine.ensureLoaded(kenji.id(), h.now);
        h.engine.ensureLoaded(hanako.id(), h.now);
        h.live(kenji, ExperienceKind.HELPED_ME, hanako, null);
        h.live(hanako, ExperienceKind.HELPED_ME, kenji, null);
        h.engine.saveAll(h.now);
        assertTrue(Files.isDirectory(dir.resolve("npc").resolve(kenji.id().toString())));
        h.engine.erase(kenji.id());
        assertFalse(Files.exists(dir.resolve("npc").resolve(kenji.id().toString())));
        assertTrue(h.engine.relationships().find(hanako.id(), kenji.id()).isEmpty(), "no one keeps a relationship with someone who no longer exists");
        assertTrue(h.engine.memory().peek(kenji.id()).isEmpty() && h.engine.emotions().peek(kenji.id()).isEmpty() && h.engine.knowledge().peek(kenji.id()).isEmpty());
        assertTrue(Files.isDirectory(dir.resolve("npc").resolve(hanako.id().toString())), "others are untouched");
    }

    @Test void oneNpcsMutableStateIsNeverSharedWithAnother() {
        var a = h.engine.memory().runtime(kenji.id());
        var b = h.engine.memory().runtime(hanako.id());
        assertNotSame(a, b);
        assertNotSame(h.engine.emotions().runtime(kenji.id()), h.engine.emotions().runtime(hanako.id()));
        assertNotSame(h.engine.relationships().runtime(kenji.id()), h.engine.relationships().runtime(hanako.id()));
        assertNotSame(h.engine.knowledge().runtime(kenji.id()), h.engine.knowledge().runtime(hanako.id()));
        assertNotSame(h.engine.ledger(kenji.id()), h.engine.ledger(hanako.id()));
        h.live(kenji, ExperienceKind.ATTACKED_ME, yeremi, null);
        assertEquals(0, b.size());
        assertEquals(0, h.engine.emotions().runtime(hanako.id()).active().size());
        assertTrue(h.engine.relationships().relationships(hanako.id()).isEmpty());
    }
}
