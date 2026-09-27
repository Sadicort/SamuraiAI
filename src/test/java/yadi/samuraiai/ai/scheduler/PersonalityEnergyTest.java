package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.cooldown.CooldownEngine;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInput;
import yadi.samuraiai.ai.scheduler.emotion.EmotionScheduler;
import yadi.samuraiai.ai.scheduler.emotion.Mood;
import yadi.samuraiai.ai.scheduler.energy.EnergyModel;
import yadi.samuraiai.ai.scheduler.energy.EnergyNeed;
import yadi.samuraiai.ai.scheduler.energy.EnergyState;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.personality.DriftCause;
import yadi.samuraiai.ai.scheduler.personality.PersonalityEngine;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.personality.Trait;
import yadi.samuraiai.ai.scheduler.routine.RoutineProfiles;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.social.Proxemics;
import yadi.samuraiai.ai.scheduler.social.SocialDistance;

class PersonalityEnergyTest {
    private final SchedulerSettings cfg = SchedulerSettings.defaults();
    private final PersonalityEngine personality = new PersonalityEngine(cfg);
    private final EnergyModel energy = new EnergyModel(cfg);
    private final RoutineProfiles profiles = new RoutineProfiles(java.util.List.of());

    private static PersonalityTraits with(Trait t, double v) { return PersonalityTraits.of(Map.of(t, v)); }

    @Test void aTraitRaisesTheRoutinesItSuitsAndLowersThoseItDoesNot() {
        assertEquals(1.0D, personality.affinity(PersonalityTraits.neutral(), RoutineType.PRAYER), 1e-9);
        assertTrue(personality.affinity(with(Trait.SPIRITUALITY, 95), RoutineType.PRAYER) > 1.2D);
        assertTrue(personality.affinity(with(Trait.SPIRITUALITY, 5), RoutineType.PRAYER) < 0.8D);
        assertTrue(personality.affinity(with(Trait.SOCIABILITY, 90), RoutineType.SOCIAL) > personality.affinity(with(Trait.SOCIABILITY, 10), RoutineType.SOCIAL));
    }

    @Test void temperamentMakesTheBoldReactLessAndTheCautiousMore() {
        var bold = personality.temperament(PersonalityTraits.of(Map.of(Trait.COURAGE, 95.0D, Trait.CAUTION, 10.0D)));
        var timid = personality.temperament(PersonalityTraits.of(Map.of(Trait.COURAGE, 10.0D, Trait.CAUTION, 95.0D)));
        assertTrue(bold.fearfulness() < timid.fearfulness());
        assertTrue(bold.suspicionGain() < timid.suspicionGain());
        assertTrue(bold.dangerAversion() < timid.dangerAversion());
        assertEquals(1.0D, personality.temperament(PersonalityTraits.neutral()).fearfulness(), 1e-9);
    }

    @Test void responsesAreTiltedByCharacter() {
        var brave = PersonalityTraits.of(Map.of(Trait.COURAGE, 95.0D, Trait.CAUTION, 10.0D));
        var timid = PersonalityTraits.of(Map.of(Trait.COURAGE, 10.0D, Trait.CAUTION, 95.0D));
        assertTrue(personality.responseBias(brave, ResponseKind.INVESTIGATE) > personality.responseBias(timid, ResponseKind.INVESTIGATE));
        assertTrue(personality.responseBias(timid, ResponseKind.FLEE) > personality.responseBias(brave, ResponseKind.FLEE));
    }

    @Test void individualsAreDeterministicAndDifferFromEachOther() {
        var base = PersonalityTraits.neutral();
        assertEquals(base.individual(7, 12), base.individual(7, 12));
        assertNotEquals(base.individual(7, 12), base.individual(8, 12));
        for (Trait t : Trait.values()) assertTrue(Math.abs(base.individual(7, 12).get(t) - 50) <= 12.0001D);
    }

    @Test void experienceMovesTraitsAndReportsTheChange() {
        var start = PersonalityTraits.neutral();
        var result = personality.drift(start, DriftCause.FRIGHTENED, 5.0D);
        assertTrue(result.traits().get(Trait.CAUTION) > 50);
        assertTrue(result.traits().get(Trait.COURAGE) < 50);
        assertEquals(2, result.changes().size());
        var maxed = personality.drift(with(Trait.CAUTION, 100), DriftCause.FRIGHTENED, 100.0D);
        assertEquals(100.0D, maxed.traits().get(Trait.CAUTION), 1e-9, "traits are clamped");
    }

    @Test void personalSpaceGrowsWithCautionAndShrinksWithSociability() {
        var shy = PersonalityTraits.of(Map.of(Trait.CAUTION, 95.0D, Trait.SOCIABILITY, 5.0D));
        var open = PersonalityTraits.of(Map.of(Trait.CAUTION, 5.0D, Trait.SOCIABILITY, 95.0D));
        assertTrue(personality.personalSpace(shy) > personality.personalSpace(open));
        var social = new SocialDistance(cfg, personality);
        assertEquals(Proxemics.INTIMATE, social.classify(0.2D, PersonalityTraits.neutral()));
        assertEquals(Proxemics.SOCIAL, social.classify(2.5D, PersonalityTraits.neutral()));
        assertEquals(Proxemics.DISTANT, social.classify(30.0D, PersonalityTraits.neutral()));
        assertTrue(social.crowded(0.5D, shy));
    }

    @Test void crowdedStandingPointsAreSpreadApartWithinTheRadius() {
        var social = new SocialDistance(cfg, personality);
        double[] spot = social.spread(10, 10, java.util.List.of(new double[]{10.2, 10}), PersonalityTraits.neutral());
        assertTrue(Math.hypot(spot[0] - 10.2, spot[1] - 10) >= personality.personalSpace(PersonalityTraits.neutral()) - 0.05D);
        assertTrue(Math.hypot(spot[0] - 10, spot[1] - 10) <= cfg.crowdSpreadRadius() + 1e-6);
        double[] free = social.spread(10, 10, java.util.List.of(new double[]{40, 40}), PersonalityTraits.neutral());
        assertEquals(10, free[0], 1e-9);
    }

    @Test void beingAwakeWearsAnNpcDownAndSleepRestoresIt() {
        EnergyState s = new EnergyState();
        for (int day = 0; day < 1; day++) energy.advance(s, profiles.of(RoutineType.WORK), true, 16000, 1.0D);
        assertTrue(s.fatigue() > cfg.sleepNeedFatigue(), s.toString());
        assertTrue(s.energy() < cfg.lowEnergy() + 25, s.toString());
        assertTrue(energy.urgency(s, EnergyNeed.SLEEP) > 50);
        assertTrue(energy.exhausted(s));
        energy.advance(s, profiles.of(RoutineType.SLEEP), true, 6000, 1.0D);
        assertTrue(s.fatigue() < 10, s.toString());
        assertTrue(s.energy() > 80, s.toString());
        assertEquals(0.0D, energy.urgency(s, EnergyNeed.SLEEP), 1e-9);
    }

    @Test void elapsedTimeIsWhatCountsSoRarelyEvaluatedNpcsLoseNothing() {
        EnergyState a = new EnergyState(), b = new EnergyState();
        for (int i = 0; i < 100; i++) energy.advance(a, profiles.of(RoutineType.PATROL), true, 10, 1.0D);
        energy.advance(b, profiles.of(RoutineType.PATROL), true, 1000, 1.0D);
        assertEquals(a.energy(), b.energy(), 1e-6);
        assertEquals(a.fatigue(), b.fatigue(), 1e-6);
    }

    @Test void stressCanBeAddedAndDecaysWithTime() {
        EnergyState s = new EnergyState();
        s.addStress(80);
        assertTrue(energy.overstressed(s));
        energy.advance(s, null, false, 4000, 1.0D);
        assertFalse(energy.overstressed(s), s.toString());
    }

    @Test void cooldownsCountDownAndAreForgotten() {
        CooldownEngine c = new CooldownEngine();
        c.start("R:EAT", 100, 50);
        assertFalse(c.ready("R:EAT", 120));
        assertEquals(30, c.remaining("R:EAT", 120));
        assertTrue(c.ready("R:EAT", 150));
        assertTrue(c.ready("R:SLEEP", 0));
        assertEquals(1, c.prune(200));
        assertEquals(0, c.size());
    }

    @Test void routineOverridesReplaceDefaultsAndBadOnesAreReported() {
        RoutineProfiles p = new RoutineProfiles(java.util.List.of("SLEEP;minTicks=100;maxTicks=200;policy=RESTART;energy=0.5", "DANCE;minTicks=1", "WORK;policy=FLY"));
        assertEquals(100, p.of(RoutineType.SLEEP).minTicks());
        assertEquals(yadi.samuraiai.ai.scheduler.interrupt.InterruptPolicy.RESTART, p.of(RoutineType.SLEEP).policy());
        assertEquals(yadi.samuraiai.ai.scheduler.interrupt.InterruptPolicy.PAUSE, p.of(RoutineType.WORK).policy(), "an unknown policy keeps the default");
        assertEquals(2, p.problems().size(), p.problems().toString());
    }

    @Test void moodOnlyChangesWhenAnotherClearlyOvertakesIt() {
        var scheduler = new EmotionScheduler(cfg);
        var tracker = new EmotionScheduler.Tracker();
        assertTrue(scheduler.update(tracker, EmotionInput.CALM).isEmpty() || tracker.mood() == Mood.CALM);
        var change = scheduler.update(tracker, new EmotionInput(90, 0, 0, 0, 10, 60, 0));
        assertTrue(change.isPresent());
        assertEquals(Mood.PANICKED, tracker.mood());
        assertTrue(scheduler.update(tracker, new EmotionInput(80, 0, 0, 0, 10, 60, 0)).isEmpty(), "still panicked");
        var influence = scheduler.influence(tracker, new EmotionInput(90, 0, 0, 0, 10, 60, 0), PersonalityTraits.neutral());
        assertTrue(influence.panic());
        assertTrue(influence.bias(RoutineType.SLEEP) < 0);
        assertTrue(influence.stressPerSecond() > 0);
    }

    @Test void griefPullsTowardsPrayerAndAngerTowardsTraining() {
        var scheduler = new EmotionScheduler(cfg);
        var grief = new EmotionScheduler.Tracker();
        scheduler.update(grief, new EmotionInput(0, 0, 80, 0, 10, 0, 0));
        assertEquals(Mood.GRIEVING, grief.mood());
        assertTrue(scheduler.influence(grief, new EmotionInput(0, 0, 80, 0, 10, 0, 0), PersonalityTraits.neutral()).bias(RoutineType.PRAYER) > 0);
        var anger = new EmotionScheduler.Tracker();
        scheduler.update(anger, new EmotionInput(0, 70, 0, 0, 10, 0, 0));
        assertEquals(Mood.ANGRY, anger.mood());
        assertTrue(scheduler.influence(anger, new EmotionInput(0, 70, 0, 0, 10, 0, 0), PersonalityTraits.neutral()).bias(RoutineType.TRAINING) > 0);
    }
}
