package yadi.samuraiai.ai.perception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.events.VisionDetectedEvent;
import yadi.samuraiai.ai.perception.events.VisionLostEvent;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.testkit.PerceptionHarness;
import yadi.samuraiai.ai.perception.vision.*;

class VisionTest {
    private final PerceptionHarness h = new PerceptionHarness();

    private VisualTrack track(SensedEntity e) { return h.state.tracks.get(e.id()); }

    @Test void aPlayerInFrontIsSeenAndReportedExactlyOnce() {
        var player = h.player(0.5D, 10.5D);
        h.run(12);
        assertEquals(VisibilityState.VISIBLE, track(player).state);
        assertTrue(track(player).confidence >= 0.6D);
        assertEquals(1, h.events(VisionDetectedEvent.class).size(), "detected once, not once per scan");
        assertEquals(EntityClass.PLAYER.name(), h.events(VisionDetectedEvent.class).get(0).kind());
    }

    @Test void aSolidWallHidesThePlayerEvenAtShortRange() {
        var player = h.player(0.5D, 8.5D);
        h.world.solidWall(-3, 64, 4, 3, 67, 4);
        h.run(12);
        assertEquals(VisibilityState.OBSTRUCTED, track(player).state);
        assertFalse(h.saw(VisionDetectedEvent.class));
        assertTrue(h.snapshot.seenTargets().isEmpty());
    }

    @Test void leavesGiveOnlyPartialVisibilityAndGlassAlmostNone() {
        var behindLeaves = h.player(0.5D, 10.5D);
        h.world.wall(-3, 64, 5, 3, 67, 5, 0.5D);
        h.run(6);
        assertEquals(VisibilityState.PARTIAL, track(behindLeaves).state, "half-opaque cover is a partial view");
        var second = new PerceptionHarness();
        var seen = second.player(0.5D, 10.5D);
        second.world.wall(-3, 64, 5, 3, 67, 5, 0.15D);
        second.run(6);
        assertEquals(VisibilityState.VISIBLE, second.state.tracks.get(seen.id()).state, "glass barely reduces sight");
    }

    @Test void thingsBehindTheNpcAreNotSeenButSomethingRightNextToItIs() {
        var far = h.player(0.5D, -10.5D);
        h.run(6);
        assertNull(track(far), "outside the field of view and beyond the rear range: nothing");
        var close = h.player(0.5D, -0.6D);
        h.run(6);
        assertNotNull(track(close));
        assertTrue(track(close).state.seen(), "presence right behind is noticed: " + track(close).state);
    }

    @Test void confidenceFadesWithDistanceAndIsLostBeyondTheEffectiveRange() {
        var near = h.player(0.5D, 8.5D);
        var edge = h.player(3.5D, 23.5D);
        h.run(6);
        assertTrue(track(near).state.seen());
        assertTrue(track(edge) == null || !track(edge).state.seen(), "at the very edge of the range nothing registers");
    }

    @Test void darknessLowersConfidence() {
        var lit = h.player(0.5D, 10.5D);
        h.run(6);
        double bright = track(lit).confidence;
        var dark = new PerceptionHarness();
        dark.world.defaultLight = 0;
        var target = dark.player(0.5D, 10.5D);
        dark.run(6);
        assertTrue(dark.state.tracks.get(target.id()).confidence < bright * 0.5D, "dark " + dark.state.tracks.get(target.id()).confidence + " vs bright " + bright);
    }

    @Test void aSneakingTargetIsHarderToSee() {
        var walking = h.player(0.5D, 17.5D);
        h.run(6);
        double normal = track(walking).confidence;
        var quiet = new PerceptionHarness();
        var sneaker = quiet.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Sneaker", 0.5D, 64.0D, 17.5D).sneaking(true));
        quiet.run(6);
        assertTrue(quiet.state.tracks.get(sneaker.id()).confidence < normal);
    }

    @Test void aTargetThatLeavesSightIsRememberedThenLostThenOnlyMemory() {
        var player = h.player(0.5D, 8.5D);
        h.run(6);
        assertTrue(track(player).state.seen());
        h.world.remove(player.id());
        h.run(20);
        assertEquals(VisibilityState.OBSTRUCTED, track(player).state, "just out of sight: still tracked");
        h.run(40);
        assertTrue(h.saw(VisionLostEvent.class), "after the grace period the target is lost");
        assertEquals(1, h.events(VisionLostEvent.class).size());
        h.run(6);
        assertEquals(VisibilityState.MEMORY_ONLY, track(player).state);
        assertTrue(h.state.memory.lastKnown(player.id()).isPresent(), "the last known position is remembered");
        assertEquals(8.5D, h.state.memory.lastKnown(player.id()).get().z, 0.01D);
        assertTrue(h.snapshot.targets().stream().anyMatch(t -> t.id().equals(player.id()) && t.state() == VisibilityState.MEMORY_ONLY));
        h.run(900);
        assertNull(track(player), "eventually even the track is forgotten");
        assertTrue(h.state.memory.entries(MemoryKind.VISUAL).isEmpty());
    }

    @Test void visionUsesSeveralRaysAndSpendsTheSharedBudget() {
        h.player(0.5D, 10.5D);
        h.run(4);
        assertTrue(h.metrics.snapshot().raycasts() >= h.settings.raysPerTarget(), "raycasts were spent: " + h.metrics.snapshot().raycasts());
        assertTrue(h.metrics.snapshot().targetsEvaluated() >= 1);
    }

    @Test void whenTheRayBudgetIsSpentTheTargetIsDeferredNotForgotten() {
        var player = h.player(0.5D, 10.5D);
        h.rayBudget = 2;
        h.run(6);
        assertTrue(h.metrics.snapshot().raysRefused() > 0);
        assertFalse(h.saw(VisionDetectedEvent.class), "no evidence without rays");
        h.rayBudget = 1000;
        h.run(6);
        assertTrue(track(player).state.seen(), "seen as soon as budget allows");
    }

    @Test void theConeHasCentreMainPeripheralAndRearZones() {
        var eye = yadi.samuraiai.ai.perception.engine.Perceiver.simple(UUID.randomUUID(), 0.5D, 64.0D, 0.5D, 0.0F);
        var s = yadi.samuraiai.ai.perception.engine.PerceptionSettings.defaults();
        assertEquals(VisionZone.CENTER, VisionCone.evaluate(eye, 0.5D, 65.6D, 10.5D, s).zone());
        assertEquals(VisionZone.MAIN, VisionCone.evaluate(eye, 10.5D, 65.6D, 10.5D, s).zone(), "45 degrees off centre");
        assertEquals(VisionZone.PERIPHERAL, VisionCone.evaluate(eye, 12.5D, 65.6D, 3.5D, s).zone(), "about 74 degrees off");
        assertEquals(VisionZone.REAR, VisionCone.evaluate(eye, 0.5D, 65.6D, -10.5D, s).zone());
        assertTrue(VisionCone.evaluate(eye, 0.5D, 65.6D, 10.5D, s).sensitivity() > VisionCone.evaluate(eye, 12.5D, 65.6D, 3.5D, s).sensitivity());
    }

    @Test void theVerticalFieldExcludesTargetsFarAboveOrBelow() {
        var eye = yadi.samuraiai.ai.perception.engine.Perceiver.simple(UUID.randomUUID(), 0.5D, 64.0D, 0.5D, 0.0F);
        var s = yadi.samuraiai.ai.perception.engine.PerceptionSettings.defaults();
        assertEquals(VisionZone.OUT_OF_VIEW, VisionCone.evaluate(eye, 0.5D, 100.0D, 5.5D, s).zone(), "almost straight up");
    }

    @Test void raycastAccumulatesOpacityInsteadOfStoppingAtTheFirstBlock() {
        h.world.wall(0, 65, 3, 0, 65, 3, 0.5D);
        h.world.wall(0, 65, 4, 0, 65, 4, 0.5D);
        double t = Raycaster.transmittance(h.world, 0.5D, 65.5D, 0.5D, 0.5D, 65.5D, 8.5D);
        assertEquals(0.25D, t, 0.01D);
        h.world.solidWall(0, 65, 5, 0, 65, 5);
        assertEquals(0.0D, Raycaster.transmittance(h.world, 0.5D, 65.5D, 0.5D, 0.5D, 65.5D, 8.5D), 1.0E-9D);
    }
}
