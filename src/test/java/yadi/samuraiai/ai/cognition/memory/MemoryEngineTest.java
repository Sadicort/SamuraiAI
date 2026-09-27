package yadi.samuraiai.ai.cognition.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.engine.Observation;
import yadi.samuraiai.ai.memory.events.EmotionMemoryActivatedEvent;
import yadi.samuraiai.ai.memory.events.MemoryCompressedEvent;
import yadi.samuraiai.ai.memory.events.MemoryConsolidatedEvent;
import yadi.samuraiai.ai.memory.events.MemoryCreatedEvent;
import yadi.samuraiai.ai.memory.events.MemoryForgottenEvent;
import yadi.samuraiai.ai.memory.events.MemoryMergedEvent;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;
import yadi.samuraiai.ai.memory.model.MemoryType;
import yadi.samuraiai.ai.memory.retrieval.RetrievalContext;
import yadi.samuraiai.ai.memory.retrieval.RetrievalQuery;
import yadi.samuraiai.ai.memory.temporal.RelativeTime;

class MemoryEngineTest {
    private final CognitionHarness h = new CognitionHarness();
    private final EntityRef me = h.npc("Kenji");
    private final EntityRef yeremi = h.player("Yeremi");

    private MemoryRuntime rt() { return h.engine.memory().runtime(me.id()); }

    @Test void anExperienceBecomesAStructuredMemoryWithItsOriginTypeAndFeeling() {
        var out = h.live(me, ExperienceKind.HELPED_ME, yeremi, null);
        assertTrue(out.kept());
        MemoryRecord r = out.memory().record();
        assertEquals(Observation.Result.CREATED, out.memory().result());
        assertEquals(Importance.HIGH, r.importance());
        assertEquals(EmotionKind.GRATITUDE, r.emotion().primary());
        assertEquals(yeremi.id(), r.actor().id());
        assertEquals(MemoryState.TEMPORARY, r.state());
        assertEquals(out.traceId(), r.origin().traceId());
        assertEquals("HELPED_ME", r.origin().event());
        assertTrue(r.events().contains("HELPED_ME"));
        assertEquals(1, h.events(MemoryCreatedEvent.class).size());
    }

    @Test void aBelowThresholdExperienceIsDiscardedNotEveryThingIsRemembered() {
        h.memorySettings = MemorySettings.builder().set("retainThreshold", 0.4).build();
        var out = h.live(me, ExperienceKind.SLEPT, null, null);
        assertFalse(out.kept());
        assertEquals(0, rt().size());
        assertEquals(1, h.engine.memory().metrics().discarded.get());
    }

    @Test void importanceFollowsWhatHappenedNotOneFixedLevel() {
        var trivial = h.live(me, ExperienceKind.PATROLLED, null, null).memory().record();
        var betrayal = h.live(me, ExperienceKind.BETRAYED, yeremi, null).memory().record();
        assertTrue(betrayal.importance().atLeast(Importance.CRITICAL), "betrayal " + betrayal.importance());
        assertTrue(trivial.importance().ordinal() < Importance.NORMAL.ordinal(), "patrol " + trivial.importance());
        assertTrue(betrayal.isProtected());
    }

    @Test void repeatedRoutineReinforcesOneMemoryInsteadOfManyIdenticalOnes() {
        for (int i = 0; i < 6; i++) { h.live(me, ExperienceKind.PATROLLED, null, null); h.advance(100); }
        assertEquals(1, rt().size());
        MemoryRecord r = rt().all().get(0);
        assertEquals(6, r.repeatCount());
        assertEquals(5, h.engine.memory().metrics().reinforced.get());
        assertTrue(rt().procedural().skill("patrol").orElseThrow().proficiency() > 0.2);
    }

    @Test void consolidationSettlesTemporaryMemoriesAndMergesSimilarOnes() {
        for (int i = 0; i < 3; i++) {
            h.live(me, ExperienceKind.CONVERSATION, yeremi, null);
            h.advance(3000);          // beyond the reinforcement window: separate memories...
            h.engine.tick(me.id(), h.now, Activity.NONE);   // ...settled and merged by consolidation
        }
        assertEquals(1, rt().size());
        MemoryRecord r = rt().all().get(0);
        assertEquals(3, r.repeatCount());
        assertEquals(MemoryState.CONSOLIDATED, r.state());
        assertFalse(h.events(MemoryMergedEvent.class).isEmpty());
        assertFalse(h.events(MemoryConsolidatedEvent.class).isEmpty());
    }

    @Test void sleepingConsolidatesEverythingFreshWithinABatch() {
        for (int i = 0; i < 5; i++) h.live(me, ExperienceKind.CONVERSATION, h.player("P" + i), null);
        assertEquals(5, rt().temporaryCount());
        h.engine.sleep(me.id(), h.now);
        assertEquals(0, rt().temporaryCount());
        assertTrue(rt().all().stream().allMatch(r -> r.state() == MemoryState.CONSOLIDATED));
    }

    @Test void memoriesFadeGraduallyAndAreForgottenNeverInstantly() {
        h.live(me, ExperienceKind.CONVERSATION, yeremi, null);
        h.advance(700); h.engine.tick(me.id(), h.now, Activity.NONE);
        MemoryRecord r = rt().all().get(0);
        double before = r.strength();
        h.advance(60000); h.engine.tick(me.id(), h.now, Activity.NONE);
        assertTrue(r.strength() < before && r.strength() > 0.05, "gradual: " + r.strength());
        assertEquals(1, rt().size());
        h.advance(600000); h.engine.tick(me.id(), h.now, Activity.NONE);
        assertEquals(0, rt().size());
        assertEquals(1, h.events(MemoryForgottenEvent.class).size());
    }

    @Test void protectedMemoriesSurviveTimeThatErasesOrdinaryOnes() {
        h.live(me, ExperienceKind.BETRAYED, yeremi, null);
        h.live(me, ExperienceKind.CONVERSATION, h.player("Other"), null);
        h.advance(700); h.engine.tick(me.id(), h.now, Activity.NONE);
        h.advance(20_000_000); h.engine.tick(me.id(), h.now, Activity.NONE);
        assertEquals(1, rt().size());
        assertEquals(ExperienceKind.BETRAYED, rt().all().get(0).kind());
        assertTrue(rt().all().get(0).strength() >= 0.99);
    }

    @Test void repetitiveEpisodesCompressIntoGistsWithoutLosingTheCount() {
        h.memorySettings = MemorySettings.builder().set("retainThreshold", 0.0).set("reinforceWindowTicks", 0).set("mergeSimilarity", 1.0).set("compressionTrigger", 10).set("compressGroupMin", 3).build();
        for (int i = 0; i < 30; i++) {
            h.live(me, ExperienceKind.PATROLLED, null, null);
            h.advance(2000);
            h.engine.tick(me.id(), h.now, Activity.NONE);
        }
        h.advance(7000); h.engine.tick(me.id(), h.now, Activity.NONE);
        int total = 0;
        for (MemoryRecord r : rt().all()) total += r.repeatCount();
        assertEquals(30, total, "compression keeps the count of what happened");
        assertTrue(rt().size() <= 8, "30 patrols became " + rt().size() + " memories");
        assertTrue(rt().all().stream().anyMatch(r -> r.state() == MemoryState.COMPRESSED && r.type() == MemoryType.SEMANTIC && r.repeatCount() >= 3));
        assertFalse(h.events(MemoryCompressedEvent.class).isEmpty());
        assertTrue(h.engine.memory().metrics().snapshot().compressionRatio() > 1.5);
    }

    @Test void indexesAnswerByPersonPlaceEmotionAndDayWithoutScanning() {
        EntityRef other = h.player("Hanako");
        PlaceRef temple = h.at("temple", 100, 100), market = h.at("market", 400, 400);
        h.live(h.input(me, ExperienceKind.HELPED_ME).actor(yeremi).place(temple));
        h.advance(30000);
        h.live(h.input(me, ExperienceKind.ATTACKED_ME).actor(other).place(market));
        var mem = h.engine.memory();
        assertEquals(1, mem.retrieve(me.id(), RetrievalQuery.create().entity(yeremi.id()), h.now).size());
        assertEquals(ExperienceKind.ATTACKED_ME, mem.retrieve(me.id(), RetrievalQuery.create().zone("market"), h.now).get(0).record().kind());
        assertEquals(ExperienceKind.HELPED_ME, mem.retrieve(me.id(), RetrievalQuery.create().emotion(EmotionKind.GRATITUDE), h.now).get(0).record().kind());
        assertEquals(1, mem.retrieve(me.id(), RetrievalQuery.create().days(h.now / 24000, h.now / 24000).entity(other.id()), h.now).size());
        assertEquals(0, mem.retrieve(me.id(), RetrievalQuery.create().entity(yeremi.id()).zone("market"), h.now).size(), "exact needs every criterion");
        assertEquals(2, mem.retrieve(me.id(), RetrievalQuery.create().entity(yeremi.id()).zone("market").mode(RetrievalQuery.Mode.FUZZY).minStrength(0), h.now).size(), "fuzzy accepts half the criteria");
        assertEquals(2, rt().index().entityCount());
    }

    @Test void contextRetrievalFindsWhatMattersNowAndIsCachedForAMoment() {
        h.live(h.input(me, ExperienceKind.ATTACKED_ME).actor(yeremi).place(h.at("gate", 10, 10)));
        h.live(h.input(me, ExperienceKind.CONVERSATION).actor(h.player("Other")).place(h.at("plaza", 500, 500)));
        var ctx = new RetrievalContext(h.at("gate", 12, 10), List.of(yeremi.id()), EmotionKind.FEAR, java.util.Set.of());
        var results = h.engine.memory().relevant(me.id(), ctx, 3, h.now);
        assertEquals(ExperienceKind.ATTACKED_ME, results.get(0).record().kind());
        assertTrue(results.get(0).matched().contains("person"));
        h.engine.memory().relevant(me.id(), ctx, 3, h.now + 5);
        assertTrue(rt().cache().shortHits() >= 1, "second call served from the short cache");
        h.live(me, ExperienceKind.GIFT_RECEIVED, yeremi, null);
        h.engine.memory().relevant(me.id(), ctx, 3, h.now + 6);
        assertTrue(rt().cache().shortMisses() >= 2, "a new memory invalidates cached answers");
    }

    @Test void aPlaceThatHurtEchoesItsFeelingOnceAndNeverLoopsBackIntoMemory() {
        PlaceRef bridge = h.at("bridge", 50, 50);
        h.live(h.input(me, ExperienceKind.BETRAYED).actor(yeremi).place(bridge));
        int memoriesBefore = rt().size();
        var ctx = new RetrievalContext(bridge, List.of(), null, java.util.Set.of());
        var echoes = h.engine.memory().echoes(me.id(), ctx, h.now + 10);
        assertFalse(echoes.isEmpty());
        assertTrue(echoes.get(0).intensity() > 0.1);
        assertTrue(h.engine.memory().echoes(me.id(), ctx, h.now + 20).isEmpty(), "cooldown: the same memory does not echo again at once");
        assertFalse(h.events(EmotionMemoryActivatedEvent.class).isEmpty());
        h.engine.context(me.id(), ctx, h.now + 5000);
        assertEquals(memoriesBefore, rt().size(), "echoes never create memories");
    }

    @Test void aLaterBetrayalReinterpretsWhatWasRememberedWarmly() {
        h.live(me, ExperienceKind.HELPED_ME, yeremi, null);
        MemoryRecord warm = rt().all().get(0);
        double valence = warm.emotion().valence();
        assertTrue(valence > 0);
        h.advance(5000);
        h.live(me, ExperienceKind.BETRAYED, yeremi, null);
        assertTrue(warm.emotion().valence() < valence, "the friendship is no longer remembered as warmly");
        assertTrue(warm.tags().stream().anyMatch(t -> t.startsWith("reinterpreted")));
    }

    @Test void placesAndPracticedRoutesBecomePartOfTheMentalMap() {
        h.live(h.input(me, ExperienceKind.DISCOVERED_PLACE).place(h.at("cave", 200, 200)).context("placeName", "Cueva del norte"));
        h.live(h.input(me, ExperienceKind.VISITED_PLACE).place(h.at("temple", 220, 220)).context("placeName", "Templo"));
        assertEquals(2, rt().spatial().size());
        var temple = rt().spatial().nearest(h.at("temple", 221, 221), 30, null).orElseThrow();
        assertEquals("Templo", temple.name());
        assertFalse(temple.connections().isEmpty(), "the two places were visited in sequence, so they are connected");
    }

    @Test void semanticBeliefsEmergeFromConsolidatedExperiences() {
        h.live(me, ExperienceKind.HELPED_ME, yeremi, null);
        h.advance(700); h.engine.tick(me.id(), h.now, Activity.NONE);
        assertTrue(rt().semantic().value(yeremi.id(), yadi.samuraiai.ai.memory.semantic.Aspect.TRUSTWORTHY) > 0.05);
        assertTrue(rt().semantic().value(yeremi.id(), yadi.samuraiai.ai.memory.semantic.Aspect.HELPFUL) > 0.05);
    }

    @Test void relativeTimeSpeaksInTermsACharacterWouldUse() {
        assertEquals("ayer", RelativeTime.between(24000 * 5 + 100, 24000 * 6 + 5000).phrase());
        assertEquals("hace tres días", RelativeTime.between(0, 24000 * 3 + 100).phrase());
        assertEquals("hace mucho tiempo", RelativeTime.between(0, 24000 * 90).phrase());
    }

    @Test void memoryIsPersistentAcrossARestartWithEveryFieldIntact(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        h.live(h.input(me, ExperienceKind.HELPED_ME).actor(yeremi).place(h.at("temple", 100, 100)).context("placeName", "Templo").tag("oath"));
        h.live(h.input(me, ExperienceKind.DISCOVERED_PLACE).place(h.at("cave", 300, 300)));
        h.advance(700); h.engine.tick(me.id(), h.now, Activity.NONE);
        CognitionStorage storage = new CognitionStorage(dir, true);
        h.engine.useStorage(storage);
        h.engine.ensureLoaded(me.id(), h.now);
        assertTrue(storage.saveNpc(h.engine, me.id(), h.now, false) >= 1);
        List<MemoryRecord> before = rt().all();

        CognitionHarness second = new CognitionHarness();
        second.engine.useStorage(new CognitionStorage(dir, true));
        second.engine.ensureLoaded(me.id(), second.now);
        MemoryRuntime restored = second.engine.memory().runtime(me.id());
        assertEquals(before.size(), restored.size());
        for (MemoryRecord r : before) {
            MemoryRecord q = restored.get(r.id());
            assertNotNull(q);
            assertEquals(r.kind(), q.kind()); assertEquals(r.importance(), q.importance()); assertEquals(r.state(), q.state()); assertEquals(r.emotion().primary(), q.emotion().primary());
            assertEquals(r.origin().traceId(), q.origin().traceId()); assertEquals(r.place().zone(), q.place().zone()); assertEquals(r.tags(), q.tags()); assertEquals(r.isProtected(), q.isProtected());
            assertEquals(r.strength(), q.strength(), 1e-9);
        }
        assertEquals(before.size(), restored.index().size());
        assertEquals(rt().spatial().size(), restored.spatial().size());
        assertEquals(1, restored.index().entity(yeremi.id()).size());
    }

    @Test void thousandsOfMemoriesStayCheapToQuery() {
        h.memorySettings = MemorySettings.builder().set("maxMemories", 5000).build();
        for (int i = 0; i < 3000; i++) {
            h.live(h.input(me, ExperienceKind.CONVERSATION).actor(i % 50 == 0 ? yeremi : h.player("P" + (i % 400))).place(h.at("z" + (i % 20), i % 500, i % 300)));
            h.advance(50);
        }
        assertTrue(rt().size() > 1000);
        long started = System.nanoTime();
        for (int i = 0; i < 200; i++) h.engine.memory().retrieve(me.id(), RetrievalQuery.create().entity(yeremi.id()).limit(5), h.now);
        double micros = (System.nanoTime() - started) / 1000.0 / 200;
        assertTrue(micros < 2000, "per-query micros " + micros);
    }
}
