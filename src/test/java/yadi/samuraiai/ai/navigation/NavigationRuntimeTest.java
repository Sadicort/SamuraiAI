package yadi.samuraiai.ai.navigation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.navigation.engine.NavigationFailure;
import yadi.samuraiai.ai.navigation.engine.NavigationSession;
import yadi.samuraiai.ai.navigation.events.*;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.obstacles.EntityKind;
import yadi.samuraiai.ai.navigation.obstacles.EntityObstacleInfo;
import yadi.samuraiai.ai.navigation.pathfinding.PathState;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;
import yadi.samuraiai.ai.navigation.terrain.BlockProfile;
import yadi.samuraiai.ai.navigation.testkit.NavHarness;
import yadi.samuraiai.ai.navigation.testkit.SimulatedBody;
import yadi.samuraiai.ai.navigation.zones.DangerZone;
import yadi.samuraiai.ai.navigation.zones.HazardType;

/** Whole-loop tests: engine, movement controller and runtime driving simulated bodies over a grid world. */
class NavigationRuntimeTest {

    private static void assertNear(SimulatedBody body, NavPos goal, double tolerance) {
        assertTrue(Math.hypot(body.x() - goal.centerX(), body.z() - goal.centerZ()) <= tolerance,
                "body at " + body.x() + "," + body.z() + " expected near " + goal);
    }

    @Test void walksAcrossFlatGroundAndPublishesTheFullLifecycle() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(12, 8));
        h.runUntilDone(s, 600);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertNear(h.bodies.get(npc), h.at(12, 8), 1.6);
        assertTrue(h.saw(PathRequestedEvent.class) && h.saw(PathCreatedEvent.class) && h.saw(PathStartedEvent.class) && h.saw(PathCompletedEvent.class));
        var snapshot = h.metrics.snapshot();
        assertEquals(1, snapshot.requests());
        assertEquals(1, snapshot.completed());
        assertTrue(snapshot.distance() > 10, "distance walked is measured");
        assertTrue(s.metrics().nodesExpanded > 0);
    }

    @Test void impossibleDestinationsFailWithExplicitReasons() {
        var h = new NavHarness();
        h.world.set(6, 63, 0, BlockProfile.LAVA);
        UUID npc = h.spawn(0, 0);
        var lava = h.go(npc, h.at(6, 0));
        h.tick();
        assertEquals(PathState.FAILED, lava.state());
        assertEquals(NavigationFailure.DANGER, lava.failure());
        var outside = h.go(npc, new NavPos(0, 900, 0));
        h.tick();
        assertEquals(NavigationFailure.DESTINATION_INVALID, outside.failure());
        assertTrue(h.saw(PathFailedEvent.class));
        assertEquals(2, h.metrics.snapshot().failed());
    }

    @Test void npcWithoutABodyOrWithADeadBodyFailsSafely() {
        var h = new NavHarness();
        var ghost = h.goBodiless(UUID.randomUUID(), h.at(5, 5));
        h.tick();
        assertEquals(NavigationFailure.NO_BODY, ghost.failure());
        UUID npc = h.spawn(0, 0);
        var s = h.go(npc, h.at(20, 0));
        h.run(5);
        h.bodies.get(npc).kill();
        h.run(3);
        assertEquals(NavigationFailure.BODY_LOST, s.failure());
    }

    @Test void movingEntityIsWaitedOutAndThenPassed() {
        var h = new NavHarness(b -> b.set("tempObstacleWaitTicks", 400));
        UUID npc = h.spawn(0, 0);
        UUID blocker = UUID.randomUUID();
        h.entities.add(new EntityObstacleInfo(blocker, EntityKind.PLAYER, 6.5, h.standY() + 0.5, 0.5, 0.6));
        NavigationSession s = h.go(npc, h.at(14, 0));
        h.run(120);
        SimulatedBody body = h.bodies.get(npc);
        assertEquals(PathState.RUNNING, s.state());
        assertTrue(body.x() < 6.0D, "must stop short of the entity, was at " + body.x());
        assertTrue(s.holding());
        h.entities.clear();
        h.runUntilDone(s, 400);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
    }

    @Test void entityThatNeverLeavesTriggersALocalDetour() {
        var h = new NavHarness(b -> b.set("tempObstacleWaitTicks", 20));
        UUID npc = h.spawn(0, 0);
        h.entities.add(new EntityObstacleInfo(UUID.randomUUID(), EntityKind.HOSTILE, 6.5, h.standY() + 0.5, 0.5, 0.6));
        NavigationSession s = h.go(npc, h.at(14, 0));
        h.runUntilDone(s, 900);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertTrue(s.recalculations() >= 1);
        assertTrue(h.saw(PathRecalculatedEvent.class));
    }

    @Test void woodenDoorIsOpenedCrossedAndClosedBehind() {
        var h = new NavHarness();
        h.world.fill(5, h.standY(), -96, 5, h.standY() + 3, 95, BlockProfile.SOLID);
        h.world.fill(5, h.standY(), 0, 5, h.standY() + 1, 0, BlockProfile.DOOR_WOOD);
        h.rebuild();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(10, 0));
        h.runUntilDone(s, 900);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertEquals(1, h.events(DoorOpenedEvent.class).size());
        assertEquals(1, h.events(DoorClosedEvent.class).size());
        assertFalse(h.bodies.get(npc).isOpen(new NavPos(5, h.standY(), 0)), "door closed behind the walker");
        assertEquals(1, s.metrics().doorsOpened);
    }

    @Test void ironDoorSealsTheWayAndTheRequestFails() {
        var h = new NavHarness(b -> b.set("maxSearchNodes", 3000));
        h.world.fill(5, h.standY(), -96, 5, h.standY() + 3, 95, BlockProfile.SOLID);
        h.world.fill(5, h.standY(), 0, 5, h.standY() + 1, 0, BlockProfile.DOOR_IRON);
        h.rebuild();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(10, 0));
        h.runUntilDone(s, 900);
        assertEquals(PathState.FAILED, s.state());
        assertTrue(s.failure() == NavigationFailure.DESTINATION_UNREACHABLE || s.failure() == NavigationFailure.SEARCH_LIMIT, "was " + s.failure());
    }

    @Test void jumpsOntoAPlatformAndClimbsALadder() {
        var h = new NavHarness();
        h.world.fill(3, h.standY(), -5, 8, h.standY(), 5, BlockProfile.SOLID);
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, new NavPos(6, h.standY() + 1, 0));
        h.runUntilDone(s, 600);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertTrue(h.bodies.get(npc).jumps >= 1, "the body actually jumped");
        assertTrue(s.metrics().jumps >= 1);

        var ladder = new NavHarness();
        ladder.world.fill(4, ladder.standY(), 0, 4, ladder.standY() + 4, 0, BlockProfile.LADDER);
        ladder.world.fill(4, ladder.standY(), 1, 4, ladder.standY() + 4, 1, BlockProfile.SOLID);
        UUID climber = ladder.spawn(1, 0);
        NavigationSession up = ladder.go(climber, new NavPos(4, ladder.standY() + 4, 0));
        ladder.runUntilDone(up, 900);
        assertEquals(PathState.COMPLETED, up.state(), up.lastEvent());
        assertTrue(ladder.bodies.get(climber).y() >= ladder.standY() + 3.0D);
    }

    @Test void walksUpStairsWithoutJumping() {
        var h = new NavHarness();
        for (int i = 0; i < 4; i++) h.world.set(3 + i, h.standY() + i, 0, BlockProfile.STAIRS);
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, new NavPos(7, h.standY() + 4, 0));
        h.world.set(7, h.standY() + 3, 0, BlockProfile.STAIRS);
        h.runUntilDone(s, 700);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent() + " " + s.failureDetail());
        assertEquals(0, h.bodies.get(npc).jumps, "stairs need no jumps");
    }

    @Test void blockPlacedInFrontOfAWalkerTriggersRecalculationAndArrival() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(16, 0));
        h.run(15);
        h.world.fill(8, h.standY(), -3, 8, h.standY() + 2, 3, BlockProfile.SOLID);
        for (int z = -3; z <= 3; z++) h.runtime.worldChanged(new NavPos(8, h.standY(), z));
        h.runUntilDone(s, 900);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertTrue(h.saw(PathRecalculatedEvent.class), "the broken route must be replanned");
        assertTrue(s.recalculations() >= 1);
        assertNear(h.bodies.get(npc), h.at(16, 0), 1.6);
    }

    @Test void frozenWalkerEscalatesThroughRecoveryAndFailsAsStuck() {
        var h = new NavHarness(b -> b.set("stuckTicks", 20).set("maxRecoveryAttempts", 4));
        UUID npc = h.spawn(0, 0);
        h.bodies.get(npc).frozen(true);
        NavigationSession s = h.go(npc, h.at(15, 0));
        h.runUntilDone(s, 1500);
        assertEquals(PathState.FAILED, s.state(), s.lastEvent());
        assertEquals(NavigationFailure.STUCK, s.failure());
        assertTrue(h.events(NPCStuckEvent.class).size() >= 4, "each recovery rung reports a stuck event");
        assertTrue(h.bodies.get(npc).jumps == 0 && h.saw(PathRecalculatedEvent.class), "recovery tried recalculating");
        assertEquals(1, h.metrics.snapshot().failuresByReason().get(NavigationFailure.STUCK));
    }

    @Test void teleportRecoveryIsOptInAndRelocatesToAValidatedNode() {
        var h = new NavHarness(b -> b.set("stuckTicks", 20).set("maxRecoveryAttempts", 4).set("allowTeleportRecovery", true));
        UUID npc = h.spawn(0, 0);
        h.bodies.get(npc).frozen(true);
        NavigationSession s = h.go(npc, h.at(15, 0));
        h.runUntilDone(s, 1500);
        assertTrue(h.bodies.get(npc).teleports >= 1, "teleport is used only as the last rung, and only when enabled");
    }

    @Test void deadlineEndsTheSessionWithTimeout() {
        var h = new NavHarness(b -> b.set("stuckTicks", 900));
        UUID npc = h.spawn(0, 0);
        h.bodies.get(npc).frozen(true);
        var request = new yadi.samuraiai.ai.navigation.planner.PathRequest(npc, h.world.dimension(), h.at(0, 0), h.at(30, 0),
                PathPreferences.defaults(), "test", "test", 1.0D, 60, false);
        NavigationSession s = h.runtime.request(request, h.tick);
        h.runUntilDone(s, 200);
        assertEquals(NavigationFailure.TIMEOUT, s.failure());
    }

    @Test void cancellationStopsTheBodyAndPublishesTheEvent() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(30, 0));
        h.run(10);
        s.cancel("behavior changed");
        assertEquals(PathState.CANCELLED, s.state());
        double x = h.bodies.get(npc).x();
        h.run(30);
        assertEquals(x, h.bodies.get(npc).x(), 0.01, "cancelled walker must stand still");
        assertEquals("behavior changed", h.events(PathCancelledEvent.class).get(0).reason());
        assertEquals(1, h.metrics.snapshot().cancelled());
    }

    @Test void newRequestReplacesThePreviousOneForTheSameNpc() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        NavigationSession first = h.go(npc, h.at(30, 0));
        h.run(3);
        NavigationSession second = h.go(npc, h.at(0, 20));
        assertEquals(PathState.CANCELLED, first.state());
        h.runUntilDone(second, 900);
        assertEquals(PathState.COMPLETED, second.state());
    }

    @Test void repeatedRouteIsServedFromCacheUntilTheWorldChanges() {
        var h = new NavHarness();
        UUID a = h.spawn(0, 0), b = h.spawn(0, 0), c = h.spawn(0, 0);
        NavigationSession first = h.go(a, h.at(20, 12));
        h.runUntilDone(first, 800);
        NavigationSession second = h.go(b, h.at(20, 12));
        h.run(2);
        assertTrue(second.cacheHit(), "identical start and goal reuse the cached path");
        h.runtime.worldChanged(new NavPos(10, h.standY(), 6));
        NavigationSession third = h.go(c, h.at(20, 12));
        h.run(2);
        assertFalse(third.cacheHit(), "a block change on the route invalidates it");
        assertTrue(h.cache.stats().hits() >= 1);
    }

    @Test void unloadedChunkAheadBlocksThenResumesWhenItLoads() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(30, 0));
        h.run(20);
        h.world.unload(1, 0);
        h.runtime.chunkChanged(1, 0);
        h.run(30);
        assertEquals(PathState.BLOCKED, s.state(), s.lastEvent());
        assertEquals("chunk-unloaded", s.blockReason());
        double x = h.bodies.get(npc).x();
        h.run(20);
        assertEquals(x, h.bodies.get(npc).x(), 0.05, "no walking into an unloaded chunk");
        h.world.load(1, 0);
        h.runtime.chunkChanged(1, 0);
        h.runUntilDone(s, 800);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
    }

    @Test void chunkThatNeverLoadsEndsWithChunkUnloadedFailure() {
        var h = new NavHarness(b -> b.set("chunkWaitTicks", 40));
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(30, 0));
        h.run(20);
        h.world.unload(1, 0);
        h.runtime.chunkChanged(1, 0);
        h.runUntilDone(s, 400);
        assertEquals(NavigationFailure.CHUNK_UNLOADED, s.failure(), s.lastEvent());
    }

    @Test void walkerPushedOffPathReplansFromItsRealPosition() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(24, 0));
        h.run(30);
        h.bodies.get(npc).teleportSafe(new NavPos(10, h.standY(), 14));
        h.runUntilDone(s, 900);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertTrue(s.recalculations() >= 1);
    }

    @Test void dangerZoneAppearingOnTheRouteMakesTheWalkerSkirtIt() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(20, 0));
        h.run(10);
        h.dangers.add(new DangerZone(h.world.dimension(), h.at(10, 0), 4, 95, 100000, HazardType.EXPLOSION, "test"));
        for (int x = 6; x <= 14; x++) h.runtime.worldChanged(new NavPos(x, h.standY(), 0));
        List<Double> closest = new ArrayList<>();
        while (!s.done() && h.tick < 1500) {
            h.tick();
            closest.add(Math.hypot(h.bodies.get(npc).x() - 10.5, h.bodies.get(npc).z() - 0.5));
        }
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertTrue(closest.stream().mapToDouble(Double::doubleValue).min().orElse(0) > 1.5, "kept clear of the hot centre");
    }

    @Test void aWalkerStandingInsideAHazardCanStillWalkOutOfIt() {
        // Regression: a stale danger zone over the starting cell made every neighbour impassable (the search expanded one node).
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        h.dangers.add(new DangerZone(h.world.dimension(), h.at(0, 0), 5, 95, 100000, HazardType.CUSTOM, "test"));
        NavigationSession s = h.go(npc, h.at(14, 0));
        h.runUntilDone(s, 900);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent() + " " + s.failureDetail());
    }

    @Test void manyWalkersShareTheSearchBudgetWithoutExceedingIt() {
        var h = new NavHarness(b -> b.set("searchNodesPerTick", 60).set("maxSessionsPerTick", 64));
        List<NavigationSession> sessions = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            UUID npc = h.spawn(-20 + i, -10);
            sessions.add(h.go(npc, h.at(20 - i, 30)));
        }
        long worst = 0;
        long previous = 0;
        for (int t = 0; t < 4000 && sessions.stream().anyMatch(x -> !x.done()); t++) {
            h.tick();
            long total = sessions.stream().mapToLong(x -> x.metrics().nodesExpanded).sum();
            worst = Math.max(worst, total - previous);
            previous = total;
        }
        assertTrue(sessions.stream().allMatch(x -> x.state() == PathState.COMPLETED), "every walker arrives");
        assertTrue(worst <= 60 + 25, "search nodes per tick must respect the shared budget, worst was " + worst);
        assertTrue(h.metrics.snapshot().budgetExhausted() > 0, "the small budget really was the limiting factor");
    }

    @Test void farWalkersUpdateLessOftenButStillArrive() {
        var near = new NavHarness();
        UUID n = near.spawn(0, 0);
        NavigationSession ns = near.go(n, near.at(30, 0));
        near.runUntilDone(ns, 900);
        var far = new NavHarness();
        far.playerDistance = 500;
        UUID f = far.spawn(0, 0);
        NavigationSession fs = far.go(f, far.at(30, 0));
        far.runUntilDone(fs, 1500);
        assertEquals(PathState.COMPLETED, ns.state());
        assertEquals(PathState.COMPLETED, fs.state(), fs.lastEvent());
        assertTrue(fs.metrics().ticks < ns.metrics().ticks * 0.6D, "far " + fs.metrics().ticks + " vs near " + ns.metrics().ticks);
        // ...but thinking less often must not mean walking slower: the far walker arrives in about the same wall-clock time.
        assertTrue(far.tick < near.tick * 1.6D + 20, "far walker took " + far.tick + " ticks, near took " + near.tick);
    }

    @Test void aWalkerNeverTreatsItsOwnEntityAsAnObstacle() {
        // Regression from the first real-server run: the scan excluded the NPC identity UUID, not the entity UUID,
        // so every walker saw itself standing on its own path and burned its recalculations detouring around itself.
        var h = new NavHarness();
        h.bodiesAreObstacles = true;
        UUID npc = h.spawn(0, 0);
        NavigationSession s = h.go(npc, h.at(20, 0));
        h.runUntilDone(s, 900);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
        assertEquals(0, s.recalculations(), "no detours around itself");
    }

    @Test void otherNpcStandingOnThePathIsAnObstacleButOneThatMovesAwayIsWaitedOut() {
        var h = new NavHarness(b -> b.set("tempObstacleWaitTicks", 400));
        h.bodiesAreObstacles = true;
        UUID walker = h.spawn(0, 0), sitter = h.spawn(8, 0);
        NavigationSession s = h.go(walker, h.at(16, 0));
        h.run(100);
        assertTrue(h.bodies.get(walker).x() < 8.0D, "held behind the other NPC");
        h.bodies.get(sitter).teleportSafe(h.at(8, 6));
        h.runUntilDone(s, 500);
        assertEquals(PathState.COMPLETED, s.state(), s.lastEvent());
    }

    @Test void gaitChangesTheBodyCommands() {
        var h = new NavHarness();
        UUID npc = h.spawn(0, 0);
        var sprint = PathPreferences.defaults().withMode(yadi.samuraiai.ai.navigation.movement.MovementMode.SPRINT);
        NavigationSession s = h.runtime.request(h.request(npc, h.at(20, 0), sprint), h.tick);
        h.run(15);
        assertTrue(h.bodies.get(npc).sprinting());
        h.runUntilDone(s, 600);
        assertEquals(PathState.COMPLETED, s.state());
    }
}
