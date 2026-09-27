package yadi.samuraiai.ai.cognition;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.catalog.ExperienceCatalog;
import yadi.samuraiai.ai.cognition.engine.CognitionSettings;
import yadi.samuraiai.ai.cognition.engine.CognitiveAdvice;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.emotion.model.Expression;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.emotion.model.Technique;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.HonorCategory;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.goal.GoalType;

/** How the cognitive advice reaches the Brain, and how the data-driven parts (the catalogue, the settings) behave. */
class CognitionAdapterTest {
    private static CognitiveAdvice advice(MoodKind mood, List<CognitiveAdvice.Familiar> familiars, double danger, String ritual, String zone) {
        return new CognitiveAdvice(UUID.randomUUID(), 0, mood, EmotionKind.CALM, 0, "CALM", 0, 0, 0, 0, 0, Technique.NONE, Expression.NEUTRAL, 1.0, familiars, danger, ritual, zone, List.of());
    }

    private static CognitiveAdvice.Familiar familiar(boolean hostile, boolean friendly) {
        return new CognitiveAdvice.Familiar(UUID.randomUUID(), "X", 50, 0, 50, 0, HonorCategory.NEUTRAL, FriendshipStage.ACQUAINTANCE, hostile, friendly);
    }

    private static int modifier(GoalType type, CognitiveAdvice a) throws Exception {
        Method m = Class.forName("yadi.samuraiai.decision.UtilityDecisionEngine").getDeclaredMethod("cognitionModifier", GoalType.class, CognitiveAdvice.class);
        m.setAccessible(true);
        return (int) m.invoke(null, type, a);
    }

    @Test void historyWithSomeoneNearbyLeansTheBrainTowardsFleeingOrTalking() throws Exception {
        var enemy = advice(MoodKind.NEUTRAL, List.of(familiar(true, false)), 0, "", "");
        var friend = advice(MoodKind.NEUTRAL, List.of(familiar(false, true)), 0, "", "");
        assertTrue(modifier(GoalType.FLEE, enemy) > 0 && modifier(GoalType.TALK, enemy) < 0);
        assertTrue(modifier(GoalType.TALK, friend) > 0 && modifier(GoalType.FLEE, friend) <= 0);
    }

    @Test void aKnownDangerAndADueTraditionEachLeanOnTheScale() throws Exception {
        assertTrue(modifier(GoalType.FLEE, advice(MoodKind.NEUTRAL, List.of(), 0.9, "", "")) > 0);
        assertTrue(modifier(GoalType.INVESTIGATE, advice(MoodKind.NEUTRAL, List.of(), 0.9, "", "")) < 0);
        var ritual = advice(MoodKind.NEUTRAL, List.of(), 0, "Meditación", "TEMPLE");
        assertTrue(modifier(GoalType.MEDITATE, ritual) > 0);
        assertEquals(0, modifier(GoalType.COMBAT, ritual));
    }

    @Test void moodColoursDecisionsAndTheTotalIsCappedSoHistoryNeverDecidesAlone() throws Exception {
        assertTrue(modifier(GoalType.REST, advice(MoodKind.EXHAUSTED, List.of(), 0, "", "")) > 0);
        assertTrue(modifier(GoalType.TALK, advice(MoodKind.MELANCHOLIC, List.of(), 0, "", "")) < 0);
        var everything = advice(MoodKind.FEARFUL, List.of(familiar(true, false), familiar(true, false)), 1.0, "x", "TEMPLE");
        for (GoalType t : GoalType.values()) assertTrue(Math.abs(modifier(t, everything)) <= 30);
    }

    @Test void theWorldContextCarriesCognitionWithoutDisturbingTheOtherAdvice() {
        var a = advice(MoodKind.HAPPY, List.of(), 0, "", "");
        WorldContext base = WorldContext.empty();
        assertTrue(base.cognition().isEmpty());
        WorldContext with = base.withCognition(a);
        assertSame(a, with.cognition().orElseThrow());
        assertTrue(with.withAdvice(null).cognition().isPresent(), "attaching scheduler advice keeps the cognition");
        assertTrue(with.advice().isEmpty());
    }

    @Test void everyKindOfExperienceHasAProfileAndConfigurationCanRetuneIt() {
        var catalog = new ExperienceCatalog(List.of());
        for (ExperienceKind k : ExperienceKind.values()) assertNotNull(catalog.get(k), k.toString());
        double before = catalog.get(ExperienceKind.INSULTED).magnitude;
        var tuned = new ExperienceCatalog(List.of("INSULTED|0.9|0.2|ANGER:80;SHAME:60|-20,-20,-20,0,0,10,-10", "NOT_A_KIND|1|1||", "GIFT_RECEIVED|zzz"));
        var p = tuned.get(ExperienceKind.INSULTED);
        assertNotEquals(before, p.magnitude);
        assertEquals(0.9, p.magnitude, 1e-9);
        assertEquals(80.0, p.emotions.get(EmotionKind.ANGER), 1e-9);
        assertEquals(-20.0, p.social.trust(), 1e-9);
        assertEquals(ExperienceKind.values().length, tuned.size(), "a malformed line changes nothing");
    }

    @Test void settingsAreClampedAndBuiltFromTheSameRecordTheConfigFileMirrors() {
        var s = CognitionSettings.builder().set("saveIntervalTicks", -5).set("nearDistance", 100.0).set("midDistance", 10.0).build();
        assertEquals(20, s.saveIntervalTicks());
        assertTrue(s.midDistance() >= s.nearDistance() && s.farDistance() >= s.midDistance());
        assertThrows(IllegalArgumentException.class, () -> CognitionSettings.builder().set("noSuchSetting", 1));
    }
}
