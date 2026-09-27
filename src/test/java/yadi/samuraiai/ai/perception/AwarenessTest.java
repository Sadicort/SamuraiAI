package yadi.samuraiai.ai.perception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.perception.attention.AttentionLevel;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.awareness.ThreatLevel;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.Reports;
import yadi.samuraiai.ai.perception.engine.SenseProfile;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.events.*;
import yadi.samuraiai.ai.perception.hearing.SoundCategory;
import yadi.samuraiai.ai.perception.hearing.SoundEvent;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.memory.PerceptionMemory;
import yadi.samuraiai.ai.perception.prediction.TrajectoryPredictor;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.testkit.PerceptionHarness;

class AwarenessTest {
    private final PerceptionHarness h = new PerceptionHarness();

    @Test void attentionPrefersThePlayerOverAnAnimal() {
        var cow = h.entity(EntityClass.ANIMAL, "Cow", 1.5D, 8.5D);
        var player = h.player(-1.5D, 12.5D);
        h.run(10);
        assertNotNull(h.snapshot.focus());
        assertEquals(player.id(), h.snapshot.focus().subject());
        assertTrue(h.snapshot.attention().atLeast(AttentionLevel.MEDIUM));
    }

    @Test void aMuchMoreImportantStimulusStealsAttentionButASlightlyBetterOneDoesNot() {
        h.entity(EntityClass.ANIMAL, "Cow", 1.5D, 8.5D);
        var player = h.player(-1.5D, 12.5D);
        h.run(10);
        assertEquals(player.id(), h.snapshot.focus().subject());
        var zombie = h.entity(EntityClass.HOSTILE, "Zombie", 0.5D, 9.5D);
        h.run(10);
        assertEquals(zombie.id(), h.snapshot.focus().subject(), "a hostile at close range outranks a player by a wide margin");
    }

    @Test void aLostFocusIsRecoveredWhenTheTargetReappearsInTime() {
        var player = h.player(0.5D, 10.5D);
        h.run(10);
        var key = h.snapshot.focus().key();
        h.world.solidWall(-3, 64, 5, 3, 68, 5);
        h.run(30);
        assertTrue(h.snapshot.focus().lost(), "the focus is kept but marked lost");
        assertEquals(key, h.snapshot.focus().key());
        h.world.wall(-3, 64, 5, 3, 68, 5, 0.0D);
        h.run(10);
        assertFalse(h.snapshot.focus().lost(), "recovered");
        assertEquals(player.id(), h.snapshot.focus().subject());
    }

    @Test void aFocusThatIsNeverRecoveredIsEventuallyDropped() {
        var player = h.player(0.5D, 10.5D);
        h.run(10);
        h.world.remove(player.id());
        h.run(h.settings.attentionRecoverTicks() + 60);
        assertNull(h.snapshot.focus());
        assertEquals(AttentionLevel.NONE, h.snapshot.attention());
    }

    @Test void aHostileApproachingRaisesThreatAndFullAwarenessThenTheNpcSearchesAndCalmsDown() {
        var zombie = h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.HOSTILE, "Zombie", 0.5D, 64.0D, 12.5D).moving(0.0D, -0.12D, false));
        h.run(12);
        assertTrue(h.snapshot.threatened(), "threat " + h.snapshot.threatLevel());
        assertEquals(AwarenessLevel.FOCUSED, h.snapshot.awareness());
        assertTrue(h.saw(ThreatDetectedEvent.class));
        assertTrue(h.saw(AwarenessChangedEvent.class));
        h.world.remove(zombie.id());
        h.run(45);
        assertEquals(AwarenessLevel.SEARCHING, h.snapshot.awareness(), "the target is out of sight but remembered: " + h.snapshot.awareness());
        h.run(3000);
        assertEquals(AwarenessLevel.UNAWARE, h.snapshot.awareness(), "eventually everything calms down");
        var changes = h.events(AwarenessChangedEvent.class);
        for (var change : changes) if (change.next().compareTo(change.previous()) < 0)
            assertEquals(change.previous().ordinal() - 1, change.next().ordinal(), "calming down happens one step at a time");
    }

    @Test void aFarNpcWithSparseScansKeepsTrackingAHostileItCanSee() {
        // Regression from the first real-server run: stimuli lasted 10 ticks but a far NPC scans every 12, so its focus expired
        // between scans, was marked lost and awareness dropped to SEARCHING while the target was in plain sight.
        var far = new PerceptionHarness();
        far.tierMultiplier = 4;
        far.entity(EntityClass.HOSTILE, "Creeper", 0.5D, 10.5D);
        far.run(200);
        assertTrue(far.snapshot.seenTargets().size() == 1);
        assertFalse(far.snapshot.focus().lost(), "the focus survives the gap between scans");
        assertTrue(far.snapshot.awareness().atLeast(AwarenessLevel.TRACKING), "awareness " + far.snapshot.awareness());
        assertTrue(far.events(AwarenessChangedEvent.class).stream().noneMatch(e -> e.next() == AwarenessLevel.SEARCHING), "never dropped to searching while in sight");
    }

    @Test void awarenessDoesNotFlickerWhenEvidenceBlinks() {
        var player = h.player(0.5D, 10.5D);
        h.run(10);
        var level = h.snapshot.awareness();
        assertTrue(level.atLeast(AwarenessLevel.AWARE));
        h.world.remove(player.id());
        h.run(5);
        assertEquals(level, h.snapshot.awareness(), "no drop within the dwell time");
    }

    @Test void movementGlimpsedThroughCoverRaisesSuspicionWhichLaterClears() {
        var runner = h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.HOSTILE, "Rustle", 0.5D, 64.0D, 10.5D).moving(0.25D, 0.0D, true));
        h.world.wall(-6, 64, 5, 6, 67, 5, 0.9D);
        for (int i = 0; i < 260 && !h.state.suspicion.raised(); i++) { h.world.replace(nudge(runner, i)); h.step(); }
        assertTrue(h.state.suspicion.raised(), "value " + h.state.suspicion.value());
        assertTrue(h.saw(SuspicionRaisedEvent.class));
        h.world.remove(runner.id());
        h.run(600);
        assertTrue(h.saw(SuspicionClearedEvent.class));
        assertFalse(h.state.suspicion.raised());
    }

    private static SensedEntity nudge(SensedEntity e, int i) {
        return new SensedEntity(e.id(), e.kind(), e.name(), 0.5D + (i % 8) * 0.4D, e.y(), 10.5D, 0.25D, 0, 0.0D, 0.6D, 1.8D, false, true, true, false);
    }

    @Test void aPlayerWhoVanishesCloseByMakesTheNpcSuspicious() {
        var player = h.player(0.5D, 6.5D);
        h.run(10);
        h.world.remove(player.id());
        h.run(70);
        assertTrue(h.state.suspicion.value() >= 5.0D, "value " + h.state.suspicion.value());
    }

    @Test void aCloseExplosionIsCriticalAndTheThreatDecaysBackToSafe() {
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.EXPLOSION, null, 0.5D, 64.0D, 5.5D, 1.0D, h.tick + 1, 40, "boom"));
        h.run(4);
        assertEquals(ThreatLevel.CRITICAL, h.snapshot.threatLevel());
        assertTrue(h.events(ThreatDetectedEvent.class).stream().anyMatch(e -> e.level() == ThreatLevel.CRITICAL));
        assertTrue(h.snapshot.awareness().atLeast(AwarenessLevel.ALERT));
        h.run(400);
        assertEquals(ThreatLevel.SAFE, h.snapshot.threatLevel());
        assertTrue(h.snapshot.threats().isEmpty());
    }

    @Test void beingHurtIsCriticalAttentionAndDangerMemory() {
        var attacker = UUID.randomUUID();
        h.state.pendingDamage.add(new Reports.DamageReport(attacker, "Bandit", 2.5D, 64.0D, 3.5D, 6.0D, h.tick + 1));
        h.run(3);
        assertEquals(ThreatLevel.CRITICAL, h.snapshot.threatLevel());
        assertEquals(AttentionLevel.CRITICAL, h.snapshot.attention());
        assertEquals(StimulusCategory.DAMAGE_TAKEN, h.snapshot.focus().category());
        assertTrue(h.state.memory.entries(MemoryKind.DANGER).stream().anyMatch(e -> attacker.equals(e.subject)));
        assertTrue(h.snapshot.suspicious());
    }

    @Test void anUnknownPlayerAwakensCuriosityOnceAndAKnownFriendDoesNot() {
        h.player(0.5D, 10.5D);
        h.run(10);
        assertEquals(1, h.events(InterestDetectedEvent.class).size(), "reported once, not every scan");
        var friendly = new PerceptionHarness();
        var friend = friendly.player(0.5D, 10.5D);
        friendly.perceiver = friendly.perceiver.withRelation(id -> id.equals(friend.id()) ? 80.0D : Double.NaN);
        friendly.run(10);
        assertFalse(friendly.saw(InterestDetectedEvent.class), "someone the NPC knows and likes is not news");
    }

    @Test void curiosityScalesWithTheSenseProfile() {
        h.entity(EntityClass.ANIMAL, "Cow", 0.5D, 10.5D);
        h.run(10);
        assertFalse(h.saw(InterestDetectedEvent.class), "an animal alone is not enough for a neutral NPC");
        var curious = new PerceptionHarness();
        curious.perceiver = curious.perceiver.withSenses(new SenseProfile(1, 1, 2.5D, 1, 1, 1));
        curious.entity(EntityClass.ANIMAL, "Cow", 0.5D, 10.5D);
        curious.run(10);
        assertTrue(curious.saw(InterestDetectedEvent.class));
    }

    @Test void aHeardSoundOrALostTargetBecomesAnInvestigationTarget() {
        assertTrue(h.snapshot == null || h.snapshot.investigationTarget().isEmpty());
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.EXPLOSION, null, 0.5D, 64.0D, 20.5D, 1.0D, h.tick + 1, 40, "boom"));
        h.run(4);
        var target = h.snapshot.investigationTarget().orElseThrow();
        assertEquals(20.5D, target.z(), target.uncertainty() + 1.0D);
        assertTrue(target.reason().contains("heard") || target.reason().contains("threat") || target.reason().contains("last"), target.reason());
    }

    @Test void soundEvidenceKeepsPointingSomewhereUntilTheNpcGetsThereAndFindsNothing() {
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.EXPLOSION, null, 0.5D, 64.0D, 45.5D, 1.0D, h.tick + 1, 40, "far boom"));
        h.run(4);
        assertTrue(h.snapshot.investigationTarget().isPresent());
        h.run(120);
        assertTrue(h.snapshot.investigationTarget().isPresent(), "long after the threat itself decayed the sound is still worth checking");
        assertEquals(ThreatLevel.SAFE, h.snapshot.threatLevel());
        double z = h.snapshot.investigationTarget().get().z();
        h.moveTo(0.5D, z - 1.0D);
        h.run(4);
        assertTrue(h.snapshot.investigationTarget().isEmpty(), "once there and nothing is found, the evidence is resolved");
    }

    @Test void perceptionMemoryFadesProgressivelyAndNeverInstantly() {
        var s = yadi.samuraiai.ai.perception.engine.PerceptionSettings.defaults();
        var memory = new PerceptionMemory();
        var entry = memory.remember(MemoryKind.VISUAL, "k", UUID.randomUUID(), "thing", 1, 64, 1, 0, 0, 1.0D, 0, "PLAYER");
        memory.fade(s.visualMemoryTicks() / 2, s);
        assertTrue(entry.strength > 0.15D && entry.strength < 0.35D, "halfway through its life: " + entry.strength);
        memory.fade(s.visualMemoryTicks() + 10, s);
        assertEquals(0, memory.size());
        assertEquals(1L, memory.forgottenTotal());
    }

    @Test void memoryKindsHaveIndependentLifetimes() {
        var s = yadi.samuraiai.ai.perception.engine.PerceptionSettings.defaults();
        var memory = new PerceptionMemory();
        memory.remember(MemoryKind.AUDITORY, "a", null, "noise", 0, 64, 0, 0, 0, 1.0D, 0, "SOUND");
        memory.remember(MemoryKind.SOCIAL, "s", UUID.randomUUID(), "friend", 0, 64, 0, 0, 0, 1.0D, 0, "PLAYER");
        memory.fade(s.auditoryMemoryTicks() + 50, s);
        assertEquals(0, memory.size(MemoryKind.AUDITORY));
        assertEquals(1, memory.size(MemoryKind.SOCIAL), "social memory outlasts a sound");
    }

    @Test void memoryIsBoundedPerKind() {
        var memory = new PerceptionMemory();
        for (int i = 0; i < 200; i++) memory.remember(MemoryKind.AUDITORY, "k" + i, null, "n", i, 64, 0, 0, 0, 0.5D, i, "SOUND");
        assertTrue(memory.size(MemoryKind.AUDITORY) <= 64);
    }

    @Test void predictionExtrapolatesWithDampingAndLosesConfidence() {
        var soon = TrajectoryPredictor.predict(0, 64, 0, 0.2D, 0.0D, 10, 1.0D);
        var later = TrajectoryPredictor.predict(0, 64, 0, 0.2D, 0.0D, 60, 1.0D);
        assertTrue(soon.x() > 1.5D && soon.x() < 2.0D, "x " + soon.x());
        assertTrue(later.x() > soon.x() && later.x() < 0.2D * 60);
        assertTrue(later.confidence() < soon.confidence());
        assertEquals(-90.0D, soon.headingDegrees(), 0.01D, "heading toward -x/+x in Minecraft yaw");
    }

    @Test void predictionEstimatesWhenATargetWillLeaveSight() {
        var runner = h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Runner", 0.5D, 64.0D, 12.5D).moving(0.0D, 0.3D, true));
        h.run(8);
        var track = h.state.tracks.get(runner.id());
        int ticks = TrajectoryPredictor.ticksUntilLostFromView(h.perceiver, h.settings, track, 400);
        assertTrue(ticks > 0, "it will leave the range: " + ticks);
        var stationary = h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Still", 0.5D, 64.0D, 6.5D));
        h.run(8);
        assertEquals(-1, TrajectoryPredictor.ticksUntilLostFromView(h.perceiver, h.settings, h.state.tracks.get(stationary.id()), 200));
    }
}
