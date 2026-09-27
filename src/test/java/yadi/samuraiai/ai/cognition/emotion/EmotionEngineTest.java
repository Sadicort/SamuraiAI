package yadi.samuraiai.ai.cognition.emotion;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.emotion.engine.EmotionEngine;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.events.EmotionContagionEvent;
import yadi.samuraiai.ai.emotion.events.EmotionCreatedEvent;
import yadi.samuraiai.ai.emotion.events.EmotionUpdatedEvent;
import yadi.samuraiai.ai.emotion.events.MoodChangedEvent;
import yadi.samuraiai.ai.emotion.events.TraumaCreatedEvent;
import yadi.samuraiai.ai.emotion.events.TraumaRecoveredEvent;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.emotion.model.DecayCurve;
import yadi.samuraiai.ai.emotion.model.EmotionEffect;
import yadi.samuraiai.ai.emotion.model.EmotionOrigin;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.EmotionTrigger;
import yadi.samuraiai.ai.emotion.model.Expression;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.emotion.model.RecoverySource;
import yadi.samuraiai.ai.emotion.model.Technique;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;
import yadi.samuraiai.ai.emotion.model.TriggerSource;
import yadi.samuraiai.ai.emotion.propagation.EmotionNeighbor;
import yadi.samuraiai.ai.emotion.recovery.DecayEngine;
import yadi.samuraiai.ai.memory.model.MemoryRecord;

class EmotionEngineTest {
    private final CognitionHarness h = new CognitionHarness();
    private final EntityRef kenji = h.npc("Kenji"), yeremi = h.player("Yeremi");

    private EmotionEngine em() { return h.engine.emotions(); }
    private EmotionTrigger fear(EntityRef npc, double intensity) { return EmotionTrigger.simple(npc.id(), TriggerSource.PERCEPTION, EmotionKind.FEAR, intensity, h.now); }

    @Test void aTriggerCreatesAnEmotionRecordWithOriginDimensionsAndAPersonalityScaledIntensity() {
        var result = em().trigger(new EmotionTrigger(kenji.id(), TriggerSource.PERCEPTION, List.of(new EmotionEffect(EmotionKind.FEAR, 60)), null, "wolf", UUID.randomUUID(), h.now, null, null, false, 0.5, false, "", "a wolf"));
        EmotionRecord r = result.records().get(0);
        assertEquals(EmotionKind.FEAR, r.kind());
        assertEquals(45.0, r.intensity(), 0.5);
        assertEquals(TriggerSource.PERCEPTION, r.origin().source());
        assertTrue(r.valence() < 0 && r.arousal() > 0.5);
        assertTrue(r.control() > 0 && r.control() < 1 && r.stability() > 0 && r.persistence() > 0);
        assertFalse(r.influences().list().isEmpty());
        assertEquals(1, h.events(EmotionCreatedEvent.class).size());
    }

    @Test void severalEmotionsCoexistAndBlendIntoAMixture() {
        em().trigger(new EmotionTrigger(kenji.id(), TriggerSource.MEMORY, List.of(new EmotionEffect(EmotionKind.FEAR, 70), new EmotionEffect(EmotionKind.HOPE, 60)), null, "x", null, h.now, null, null, false, 0.6, false, "", ""));
        h.run(kenji, 400, 100, Activity.NONE);
        var blend = em().blend(kenji.id());
        assertTrue(blend.mixed(), blend.label());
        assertTrue(blend.label().contains("FEAR") && blend.label().contains("HOPE"), blend.label());
        assertTrue(em().runtime(kenji.id()).active().size() >= 2);
    }

    @Test void sameOriginEmotionsFuseInsteadOfMultiplying() {
        UUID memory = UUID.randomUUID();
        for (int i = 0; i < 3; i++) em().trigger(new EmotionTrigger(kenji.id(), TriggerSource.MEMORY, List.of(new EmotionEffect(EmotionKind.ANGER, 40)), memory, "", null, h.now, null, null, false, 0.5, false, "", ""));
        assertEquals(1, em().runtime(kenji.id()).active().size());
        EmotionRecord r = em().runtime(kenji.id()).activeRecords().get(0);
        assertEquals(2, r.reinforcements());
        assertTrue(r.intensity() > 30 && r.intensity() <= 100);
        assertEquals(2, h.events(EmotionUpdatedEvent.class).size());
    }

    @Test void moodMovesGraduallyAndLastsLongerThanTheEmotionThatCausedIt() {
        em().trigger(new EmotionTrigger(kenji.id(), TriggerSource.MEMORY, List.of(new EmotionEffect(EmotionKind.SADNESS, 75)), UUID.randomUUID(), "", null, h.now, null, null, false, 1.0, false, "", ""));
        h.run(kenji, 100, 100, Activity.NONE);
        assertEquals(MoodKind.NEUTRAL, em().mood(kenji.id()), "one step is not enough to change mood");
        h.run(kenji, 2500, 100, Activity.NONE);
        assertEquals(MoodKind.MELANCHOLIC, em().mood(kenji.id()));
        assertFalse(h.events(MoodChangedEvent.class).isEmpty());
        long changes = h.events(MoodChangedEvent.class).size();
        // The sadness itself decays long before the mood is gone, and the mood does not flicker on the way out.
        h.run(kenji, 6000, 100, Activity.NONE);
        assertTrue(em().intensity(kenji.id(), EmotionKind.SADNESS) < 30, "sadness " + em().intensity(kenji.id(), EmotionKind.SADNESS));
        assertTrue(h.events(MoodChangedEvent.class).size() <= changes + 1, "mood changes " + h.events(MoodChangedEvent.class));
        h.run(kenji, 60000, 200, Activity.NONE);
        assertEquals(MoodKind.NEUTRAL, em().mood(kenji.id()));
    }

    @Test void decayCurvesDifferInShapeAndTraumaLeavesAResidue() {
        EmotionSettings s = EmotionSettings.defaults();
        DecayEngine decay = new DecayEngine();
        EmotionRecord exp = rec(EmotionKind.FEAR, DecayCurve.EXPONENTIAL), lin = rec(EmotionKind.FEAR, DecayCurve.LINEAR), hope = rec(EmotionKind.HOPE, DecayCurve.HOPE),
                trauma = rec(EmotionKind.FEAR, DecayCurve.TRAUMA), custom = rec(EmotionKind.FEAR, DecayCurve.CUSTOM);
        custom.customHalfLife(600);
        for (EmotionRecord r : List.of(exp, lin, hope, trauma, custom)) decay.decay(r, 2400, 1.0, 1.0, s);
        assertTrue(custom.intensity() < exp.intensity(), "a short custom half-life fades faster");
        assertTrue(trauma.intensity() > exp.intensity() * 2, "trauma fades far more slowly");
        assertTrue(lin.intensity() < 100 && lin.intensity() > 0);
        assertTrue(hope.intensity() > 40, "hope lingers");
        for (int i = 0; i < 40; i++) decay.decay(trauma, 24000, 1.0, 1.0, s);
        assertTrue(trauma.intensity() >= 100 * s.traumaFloorFraction() * 0.99, "an unrecovered trauma keeps a floor: " + trauma.intensity());
        for (int i = 0; i < 40; i++) decay.decay(trauma, 24000, 1.0, 0.0, s);
        assertTrue(trauma.intensity() < 5, "once recovered the residue is gone");
    }

    private EmotionRecord rec(EmotionKind kind, DecayCurve curve) {
        EmotionRecord r = new EmotionRecord(UUID.randomUUID(), kenji.id(), kind, new EmotionOrigin(TriggerSource.ADMIN, null, "", null), kind + "|" + UUID.randomUUID(), 100, 0, 8);
        r.curve(curve);
        return r;
    }

    @Test void sleepingAndMeditatingCalmFasterThanJustWaiting() {
        EntityRef sleeper = h.npc("Sleeper"), waiter = h.npc("Waiter");
        for (EntityRef e : List.of(sleeper, waiter)) em().trigger(EmotionTrigger.simple(e.id(), TriggerSource.PERCEPTION, EmotionKind.ANGER, 100, h.now));
        for (int i = 0; i < 20; i++) { h.advance(100); em().tick(sleeper.id(), h.now, Activity.SLEEPING); em().tick(waiter.id(), h.now, Activity.NONE); }
        assertTrue(em().intensity(sleeper.id(), EmotionKind.ANGER) < em().intensity(waiter.id(), EmotionKind.ANGER));
    }

    @Test void personalityRulesShapeWhatIsFeltAndForHowLong() {
        EntityRef brave = h.npc("Brave"), timid = h.npc("Timid"), patient = h.npc("Patient"), hothead = h.npc("Hothead");
        h.setTrait(brave, Trait.COURAGE, 95); h.setTrait(timid, Trait.COURAGE, 5);
        h.setTrait(patient, Trait.PATIENCE, 95); h.setTrait(hothead, Trait.PATIENCE, 5);
        for (EntityRef e : List.of(brave, timid)) em().trigger(EmotionTrigger.simple(e.id(), TriggerSource.PERCEPTION, EmotionKind.FEAR, 80, h.now));
        assertTrue(em().intensity(brave.id(), EmotionKind.FEAR) < em().intensity(timid.id(), EmotionKind.FEAR) * 0.8);
        for (EntityRef e : List.of(patient, hothead)) em().trigger(EmotionTrigger.simple(e.id(), TriggerSource.PERCEPTION, EmotionKind.ANGER, 80, h.now));
        for (int i = 0; i < 30; i++) { h.advance(100); em().tick(patient.id(), h.now, Activity.NONE); em().tick(hothead.id(), h.now, Activity.NONE); }
        assertTrue(em().intensity(patient.id(), EmotionKind.ANGER) < em().intensity(hothead.id(), EmotionKind.ANGER), "patient people let go of anger sooner");
    }

    @Test void aWitnessedDeathLeavesATraumaThatHealsByPartsWhileTheMemoryStays() {
        EntityRef killer = h.player("Killer"), ally = h.npc("Ally");
        var out = h.live(h.input(kenji, ExperienceKind.WITNESSED_DEATH).actor(killer).target(ally).traumatic(true).place(h.at("gate", 5, 5)));
        assertNotNull(out.emotion().trauma());
        TraumaRecord t = out.emotion().trauma();
        assertTrue(t.intensity() > 0.3 && t.progress() == 0 && t.triggers().stream().anyMatch(k -> k.startsWith("cell:")) && t.triggers().contains("entity:" + killer.id()));
        assertTrue(t.memories().contains(out.memory().record().id()));
        assertEquals(1, h.events(TraumaCreatedEvent.class).size());
        MemoryRecord memory = out.memory().record();
        assertTrue(memory.isProtected() && memory.emotion().traumatic());
        double before = em().intensity(kenji.id(), EmotionKind.SADNESS);
        // Friendship, a good night and time each move recovery; the wound closes progressively.
        for (int i = 0; i < 12; i++) em().recover(kenji.id(), RecoverySource.FRIENDSHIP, h.now);
        assertEquals(TraumaRecord.Phase.RECOVERING, t.phase());
        h.run(kenji, 24000, 1000, Activity.SLEEPING);
        for (int i = 0; i < 40 && t.active(); i++) em().recover(kenji.id(), RecoverySource.POSITIVE_EVENT, h.now);
        assertEquals(TraumaRecord.Phase.RECOVERED, t.phase());
        assertEquals(1, h.events(TraumaRecoveredEvent.class).size());
        h.run(kenji, 200000, 1000, Activity.NONE);
        assertTrue(em().intensity(kenji.id(), EmotionKind.SADNESS) < before);
        assertNotNull(h.engine.memory().runtime(kenji.id()).get(memory.id()), "the memory remains after the trauma has healed");
    }

    @Test void resilienceMakesNpcsFeelLessAndHealFaster() {
        EntityRef stoic = h.npc("Stoic"), fragile = h.npc("Fragile");
        h.setTrait(stoic, Trait.DISCIPLINE, 95); h.setTrait(stoic, Trait.COURAGE, 95);
        h.setTrait(fragile, Trait.DISCIPLINE, 5); h.setTrait(fragile, Trait.COURAGE, 5);
        assertTrue(em().resilienceOf(stoic.id()) > 1.2 && em().resilienceOf(fragile.id()) < 0.8);
        for (EntityRef e : List.of(stoic, fragile)) em().trigger(new EmotionTrigger(e.id(), TriggerSource.MEMORY, List.of(new EmotionEffect(EmotionKind.SADNESS, 90)), UUID.randomUUID(), "", null, h.now, null, null, true, 1.0, false, "WITNESSED_DEATH", ""));
        assertTrue(em().intensity(stoic.id(), EmotionKind.SADNESS) < em().intensity(fragile.id(), EmotionKind.SADNESS));
        for (int i = 0; i < 6; i++) { em().recover(stoic.id(), RecoverySource.FRIENDSHIP, h.now); em().recover(fragile.id(), RecoverySource.FRIENDSHIP, h.now); }
        assertTrue(em().traumas(stoic.id()).get(0).progress() > em().traumas(fragile.id()).get(0).progress());
        assertTrue(em().resilienceOf(fragile.id()) < em().resilienceOf(stoic.id()));
    }

    @Test void traumaTriggersFlashBackAsFeelingsOnlyAndWithACooldown() {
        h.live(h.input(kenji, ExperienceKind.NEAR_DEATH).actor(yeremi).place(h.at("well", 40, 40)));
        var first = em().flashbacks(kenji.id(), h.at("well", 42, 41), List.of(), h.now + 10);
        assertEquals(1, first.size());
        assertTrue(first.get(0).fromEcho());
        assertTrue(em().flashbacks(kenji.id(), h.at("well", 42, 41), List.of(), h.now + 20).isEmpty(), "cooldown");
        assertEquals(1, em().flashbacks(kenji.id(), h.at("elsewhere", 900, 900), List.of(yeremi.id()), h.now + 5000).size(), "the person who caused it also brings it back");
        assertTrue(em().flashbacks(kenji.id(), h.at("elsewhere", 900, 900), List.of(), h.now + 5100).isEmpty());
        int memories = h.engine.memory().runtime(kenji.id()).size();
        h.engine.context(kenji.id(), new yadi.samuraiai.ai.memory.retrieval.RetrievalContext(h.at("well", 42, 41), List.of(), null, java.util.Set.of()), h.now + 100000);
        assertEquals(memories, h.engine.memory().runtime(kenji.id()).size(), "echoes never become new memories");
    }

    @Test void panicAndCalmSpreadToNearbyTrustingNpcsButNeverEchoOnwards() {
        EntityRef a = h.npc("A"), b = h.npc("B"), c = h.npc("C");
        em().trigger(new EmotionTrigger(a.id(), TriggerSource.PERCEPTION, List.of(new EmotionEffect(EmotionKind.FEAR, 100)), null, "fire", null, h.now, null, null, false, 1.0, false, "", ""));
        var transfers = em().contagion(a.id(), List.of(new EmotionNeighbor(b.id(), 3, 80, 0.5), new EmotionNeighbor(c.id(), 60, 80, 0.5)), h.now + 300);
        assertEquals(1, transfers.stream().filter(t -> t.target().equals(b.id())).count());
        assertTrue(transfers.stream().noneMatch(t -> t.target().equals(c.id())), "too far to catch it");
        assertFalse(h.events(EmotionContagionEvent.class).isEmpty());
        var caught = transfers.get(0);
        em().trigger(new EmotionTrigger(b.id(), TriggerSource.CONTAGION, List.of(new EmotionEffect(caught.kind(), caught.intensity())), null, "contagion:" + a.id(), null, h.now + 300, null, null, false, 0.5, false, "", ""));
        assertTrue(em().contagion(b.id(), List.of(new EmotionNeighbor(c.id(), 2, 90, 0.9)), h.now + 900).isEmpty(), "an emotion that was itself caught does not spread again");
    }

    @Test void feelingsColourExpressionBodyAndSpeech() {
        em().trigger(EmotionTrigger.simple(kenji.id(), TriggerSource.PERCEPTION, EmotionKind.FEAR, 100, h.now));
        h.run(kenji, 300, 100, Activity.NONE);
        assertEquals(Expression.STEP_BACK, em().expression(kenji.id()));
        assertTrue(em().physiology(kenji.id()).speedScale() > 1.0);
        var afraidTone = em().tone(kenji.id());
        EntityRef grieving = h.npc("Grieving");
        em().trigger(EmotionTrigger.simple(grieving.id(), TriggerSource.MEMORY, EmotionKind.SADNESS, 100, h.now));
        h.run(grieving, 300, 100, Activity.NONE);
        assertEquals(Expression.LOOK_DOWN, em().expression(grieving.id()));
        assertTrue(em().physiology(grieving.id()).speedScale() < 1.0);
        var sadTone = em().tone(grieving.id());
        assertTrue(sadTone.silence() > 0.3 && sadTone.verbosity() < afraidTone.verbosity() + 0.3);
        assertNotEquals("neutral", afraidTone.voiceTone());
    }

    @Test void anNpcThatIsOverwhelmedWantsATechniqueThatSuitsItsCharacter() {
        EntityRef monk = h.npc("Monk"), talker = h.npc("Talker");
        h.setTrait(monk, Trait.SPIRITUALITY, 95); h.setTrait(monk, Trait.SOCIABILITY, 10);
        h.setTrait(talker, Trait.SOCIABILITY, 95); h.setTrait(talker, Trait.SPIRITUALITY, 10);
        for (EntityRef e : List.of(monk, talker)) {
            em().trigger(new EmotionTrigger(e.id(), TriggerSource.MEMORY, List.of(new EmotionEffect(EmotionKind.ANGER, 100), new EmotionEffect(EmotionKind.SADNESS, 100)), UUID.randomUUID(), "", null, h.now, null, null, false, 1.0, false, "", ""));
            h.run(e, 400, 100, Activity.NONE);
        }
        assertEquals(Technique.MEDITATE, em().regulationAdvice(monk.id()).technique());
        assertEquals(Technique.TALK, em().regulationAdvice(talker.id()).technique());
        assertTrue(em().regulationAdvice(monk.id()).urgency() > 0);
    }

    @Test void emotionalStateSurvivesARestart(@org.junit.jupiter.api.io.TempDir Path dir) {
        h.live(h.input(kenji, ExperienceKind.WITNESSED_DEATH).actor(yeremi).traumatic(true).place(h.at("gate", 5, 5)));
        h.run(kenji, 3000, 100, Activity.NONE);
        EmotionRuntime before = em().runtime(kenji.id());
        MoodKind mood = before.mood();
        int active = before.active().size();
        double sadness = em().intensity(kenji.id(), EmotionKind.SADNESS);
        CognitionStorage storage = new CognitionStorage(dir, false);
        h.engine.useStorage(storage);
        h.engine.ensureLoaded(kenji.id(), h.now);
        storage.saveNpc(h.engine, kenji.id(), h.now, false);
        CognitionHarness second = new CognitionHarness();
        second.engine.useStorage(new CognitionStorage(dir, false));
        second.engine.ensureLoaded(kenji.id(), second.now);
        EmotionRuntime after = second.engine.emotions().runtime(kenji.id());
        assertEquals(mood, after.mood());
        assertEquals(active, after.active().size());
        assertEquals(sadness, second.engine.emotions().intensity(kenji.id(), EmotionKind.SADNESS), 1e-6);
        assertEquals(1, after.traumas().size());
        assertEquals(before.traumas().get(0).triggers(), after.traumas().get(0).triggers());
        assertEquals(before.history().size(), after.history().size());
    }

    @Test void whyDoesItFeelThisIsAnswerableFromTheRecord() {
        h.live(h.input(kenji, ExperienceKind.ATTACKED_ME).actor(yeremi));
        List<String> why = em().explain(kenji.id(), EmotionKind.FEAR);
        String text = String.join("\n", why);
        assertTrue(text.contains("FEAR") && text.contains("origen MEMORY") && text.contains("memoria"), text);
    }
}
