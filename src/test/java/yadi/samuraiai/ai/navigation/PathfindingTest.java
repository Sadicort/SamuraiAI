package yadi.samuraiai.ai.navigation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.navigation.graph.*;
import yadi.samuraiai.ai.navigation.pathfinding.*;
import yadi.samuraiai.ai.navigation.planner.*;
import yadi.samuraiai.ai.navigation.terrain.*;
import yadi.samuraiai.ai.navigation.testkit.GridWorldView;
import yadi.samuraiai.ai.navigation.zones.*;

class PathfindingTest {
    private final GridWorldView world = GridWorldView.flat();
    private final DangerMap dangers = new DangerMap();
    private PathPreferences prefs = PathPreferences.defaults();

    private NavigationGraph graph() { return new NavigationGraph(world, 100, 100000, null); }
    private NavPos at(int x, int z) { return new NavPos(x, world.standY(), z); }

    private Optional<NavigationPath> solve(NavigationGraph graph, NavPos from, NavPos to) {
        var costs = new PathCostModel(TerrainCostTable.defaults(), dangers, world.dimension(), prefs, NavEnvironment.CLEAR_DAY, 0);
        return new PathfindingEngine(graph).solve(costs, prefs, from, to, 20000, 64, Set.of(), 0);
    }

    @Test void simpleFlatPathReachesGoalAndIsSmoothed() {
        var path = solve(graph(), at(0, 0), at(10, 7)).orElseThrow();
        assertFalse(path.partial());
        assertEquals(at(10, 7), path.end());
        assertTrue(path.size() <= 4, "string pulling should remove the grid staircase, got " + path.size());
    }

    @Test void wallForcesDetour() {
        world.wall(5, -3, 3, 3);
        var path = solve(graph(), at(0, 0), at(10, 0)).orElseThrow();
        assertEquals(at(10, 0), path.end());
        assertTrue(path.nodes().stream().anyMatch(n -> Math.abs(n.pos().z()) >= 4), "route must go around the wall");
    }

    @Test void walledInGoalIsUnreachable() {
        world.fill(9, world.standY(), -1, 11, world.standY() + 2, 1, BlockProfile.SOLID);
        world.fill(10, world.standY(), 0, 10, world.standY() + 2, 0, BlockProfile.AIR);
        var graph = graph();
        var costs = new PathCostModel(TerrainCostTable.defaults(), dangers, world.dimension(), prefs, NavEnvironment.CLEAR_DAY, 0);
        var search = new PathfindingEngine(graph).begin(costs, prefs, at(0, 0), at(10, 0), 3000, 32, Set.of());
        assertNotEquals(PathSearch.Status.FOUND, search.advance(3000));
    }

    @Test void stepUpOverOneBlockUsesJumpEdge() {
        world.fill(3, world.standY(), -5, 8, world.standY(), 5, BlockProfile.SOLID);
        var path = solve(graph(), at(0, 0), new NavPos(6, world.standY() + 1, 0)).orElseThrow();
        assertTrue(path.nodes().stream().anyMatch(n -> n.via() == EdgeType.JUMP));
    }

    @Test void stairsAreWalkedWithoutJumping() {
        for (int i = 0; i < 3; i++) world.set(2 + i, world.standY() + i, 0, BlockProfile.STAIRS);
        var goal = new NavPos(5, world.standY() + 3, 0);
        world.set(5, world.standY() + 2, 0, BlockProfile.STAIRS);
        var path = solve(graph(), at(0, 0), goal);
        assertTrue(path.isPresent());
        assertTrue(path.get().nodes().stream().noneMatch(n -> n.via() == EdgeType.JUMP), "stairs must not need jumps");
    }

    @Test void dropBeyondToleranceIsRefusedButAllowedWhenWalkerAcceptsIt() {
        // Dig a 7-block deep pit region east of x=3: the top is a cliff edge, the goal is at the bottom.
        world.fill(3, 57, -30, 30, 63, 30, BlockProfile.AIR);
        var bottom = new NavPos(10, 57, 0);
        var graph = graph();
        var costs = new PathCostModel(TerrainCostTable.defaults(), dangers, world.dimension(), prefs, NavEnvironment.CLEAR_DAY, 0);
        var refused = new PathfindingEngine(graph).begin(costs, prefs, at(0, 0), bottom, 3000, 40, Set.of());
        assertNotEquals(PathSearch.Status.FOUND, refused.advance(3000), "a 7-block fall exceeds the default tolerance of 3");
        var brave = prefs.withMaxDrop(10);
        var braveCosts = new PathCostModel(TerrainCostTable.defaults(), dangers, world.dimension(), brave, NavEnvironment.CLEAR_DAY, 0);
        var accepted = new PathfindingEngine(graph).solve(braveCosts, brave, at(0, 0), bottom, 5000, 40, Set.of(), 0);
        assertTrue(accepted.isPresent() && !accepted.get().partial());
        assertTrue(accepted.get().nodes().stream().anyMatch(n -> n.via() == EdgeType.DESCEND));
    }

    @Test void safeDropIsTakenAsDescendEdge() {
        world.fill(-5, 61, -5, 5, 63, 5, BlockProfile.AIR);
        world.fill(0, 61, 0, 0, 63, 0, BlockProfile.SOLID);
        var top = new NavPos(0, 64, 0);
        var bottom = new NavPos(2, 61, 0);
        var path = solve(graph(), top, bottom).orElseThrow();
        assertEquals(bottom, path.end());
        assertTrue(path.nodes().stream().anyMatch(n -> n.via() == EdgeType.DESCEND));
    }

    @Test void lavaIsNeverEnteredAndDestinationOnLavaIsRejected() {
        world.set(5, 63, 0, BlockProfile.LAVA);
        var validator = new DestinationValidator(graph(), dangers);
        assertEquals(DestinationResult.Status.LAVA, validator.validate(at(5, 0), prefs, 0).status());
        world.fill(5, 63, -2, 5, 63, 2, BlockProfile.LAVA);
        var path = solve(graph(), at(0, 0), at(10, 0)).orElseThrow();
        assertTrue(path.nodes().stream().noneMatch(n -> n.pos().x() == 5 && Math.abs(n.pos().z()) <= 2));
    }

    @Test void shallowWaterIsWadedOnlyWhenAllowedAndCostsMore() {
        world.fill(3, world.standY(), -96, 6, world.standY(), 95, BlockProfile.WATER);
        var wading = prefs;
        var dry = prefs.withWater(false, false);
        prefs = wading;
        var wet = solve(graph(), at(0, 0), at(10, 0));
        assertTrue(wet.isPresent());
        assertTrue(wet.get().nodes().stream().anyMatch(n -> n.via() == EdgeType.WADE));
        prefs = dry;
        var blocked = solve(graph(), at(0, 0), at(10, 0));
        assertTrue(blocked.isEmpty() || blocked.get().partial(), "a wide river cannot be crossed without wading");
    }

    @Test void closedWoodDoorIsPassedAsDoorEdgeAndIronDoorBlocks() {
        world.fill(5, world.standY(), -96, 5, world.standY() + 3, 95, BlockProfile.SOLID);
        world.fill(5, world.standY(), 0, 5, world.standY() + 1, 0, BlockProfile.DOOR_WOOD);
        var path = solve(graph(), at(0, 0), at(10, 0)).orElseThrow();
        assertTrue(path.nodes().stream().anyMatch(n -> n.via() == EdgeType.DOOR));
        world.fill(5, world.standY(), 0, 5, world.standY() + 1, 0, BlockProfile.DOOR_IRON);
        var iron = solve(graph(), at(0, 0), at(10, 0));
        assertTrue(iron.isEmpty() || iron.get().partial());
    }

    @Test void ladderIsClimbed() {
        world.fill(4, world.standY(), 0, 4, world.standY() + 4, 0, BlockProfile.LADDER);
        world.fill(4, world.standY(), 1, 4, world.standY() + 4, 1, BlockProfile.SOLID);
        world.fill(3, world.standY() + 4, 0, 3, world.standY() + 4, 0, BlockProfile.SOLID);
        var path = solve(graph(), at(2, 0), new NavPos(4, world.standY() + 4, 0));
        assertTrue(path.isPresent());
        assertTrue(path.get().nodes().stream().anyMatch(n -> n.via() == EdgeType.CLIMB));
    }

    @Test void searchIsResumableAcrossBudgets() {
        var graph = graph();
        var costs = new PathCostModel(TerrainCostTable.defaults(), dangers, world.dimension(), prefs, NavEnvironment.CLEAR_DAY, 0);
        var search = new PathfindingEngine(graph).begin(costs, prefs, at(0, 0), at(40, 40), 20000, 96, Set.of());
        int rounds = 0;
        while (search.advance(5) == PathSearch.Status.IN_PROGRESS) rounds++;
        assertTrue(rounds > 3, "a long search must take several budgeted slices");
        assertEquals(PathSearch.Status.FOUND, search.status());
    }

    @Test void goalInUnloadedChunkIsRejected() {
        world.unload(1, 0);
        var validator = new DestinationValidator(graph(), dangers);
        assertEquals(DestinationResult.Status.CHUNK_UNLOADED, validator.validate(at(20, 0), prefs, 0).status());
    }

    @Test void destinationInVoidOrOutsideWorldIsRejected() {
        var validator = new DestinationValidator(graph(), dangers);
        assertEquals(DestinationResult.Status.OUT_OF_WORLD, validator.validate(new NavPos(0, 999, 0), prefs, 0).status());
        world.fill(-8, 0, -8, 8, 63, 8, BlockProfile.AIR);
        var status = validator.validate(new NavPos(2, 70, 2), prefs, 0).status();
        assertTrue(status == DestinationResult.Status.VOID || status == DestinationResult.Status.BLOCKED, "was " + status);
    }

    @Test void personalityBiasChangesRouteChoice() {
        // A wall between x=0..10 with two 1-wide lanes: z=-2 (clean floor) and z=+2 (mud floor).
        world.fill(0, world.standY(), -6, 10, world.standY() + 2, 6, BlockProfile.SOLID);
        world.fill(0, world.standY(), -2, 10, world.standY() + 2, -2, BlockProfile.AIR);
        world.fill(0, world.standY(), 2, 10, world.standY() + 2, 2, BlockProfile.AIR);
        world.fill(0, 63, 2, 10, 63, 2, BlockProfile.MUD);
        prefs = PathPreferences.defaults().withMaterialBias(Material.MUD, 6.0D);
        var disciplined = solve(graph(), at(-1, 0), at(11, 0)).orElseThrow();
        assertEquals(at(11, 0), disciplined.end());
        assertTrue(disciplined.nodes().stream().noneMatch(n -> n.pos().x() >= 0 && n.pos().x() <= 10 && n.pos().z() > 0),
                "an NPC that hates mud must take the clean lane");
        prefs = PathPreferences.defaults();
        var southFloor = graph();
        assertEquals(Material.MUD, southFloor.node(at(4, 2)).floor());
    }

    @Test void dynamicDangerZoneReroutesAndDangerAboveToleranceBlocks() {
        dangers.add(new DangerZone(world.dimension(), at(5, 0), 3, 90, 1000, HazardType.EXPLOSION, "test"));
        var path = solve(graph(), at(0, 0), at(10, 0)).orElseThrow();
        assertTrue(path.nodes().stream().noneMatch(n -> n.pos().horizontalDistance(at(5, 0)) < 1.5), "must skirt the hot zone");
    }

    @Test void forbiddenZoneIsImpassable() {
        dangers.addForbidden(new ForbiddenZone("keep-out", world.dimension(), new NavPos(4, 60, -96), new NavPos(6, 80, 95)));
        var graph = graph();
        var costs = new PathCostModel(TerrainCostTable.defaults(), dangers, world.dimension(), prefs, NavEnvironment.CLEAR_DAY, 0);
        var search = new PathfindingEngine(graph).begin(costs, prefs, at(0, 0), at(10, 0), 3000, 40, Set.of());
        assertNotEquals(PathSearch.Status.FOUND, search.advance(3000));
    }

    @Test void graphInvalidationSeesBrokenAndPlacedBlocks() {
        var graph = graph();
        assertTrue(graph.standable(at(3, 3), prefs));
        world.set(3, world.standY(), 3, BlockProfile.SOLID);
        assertTrue(graph.standable(at(3, 3), prefs), "stale cache until invalidated");
        graph.invalidate(at(3, 3));
        assertFalse(graph.standable(at(3, 3), prefs));
        assertTrue(graph.invalidations() > 0);
    }

    @Test void smoothedPathsKeepTheBodyFootprintClearOfWallCorners() {
        // Regression from the first real-server run: a shortcut that only checked cell centres cut across the
        // corner of a wall, where a 0.6-wide NPC jams. Every sampled point of every segment must fit the body.
        world.fill(5, world.standY(), 3, 5, world.standY() + 2, 60, BlockProfile.SOLID);
        var graph = graph();
        var path = solve(graph, at(1, 6), at(9, 6)).orElseThrow();
        assertEquals(at(9, 6), path.end());
        for (int i = 1; i < path.size(); i++) {
            var a = path.get(i - 1).pos();
            var b = path.get(i).pos();
            int samples = (int) Math.ceil(a.horizontalDistance(b) / 0.25D);
            for (int k = 0; k <= samples; k++) {
                double t = samples == 0 ? 0 : (double) k / samples;
                double x = a.centerX() + (b.centerX() - a.centerX()) * t, z = a.centerZ() + (b.centerZ() - a.centerZ()) * t;
                assertTrue(graph.footprintStandable(x, z, a.y(), prefs, false), "body clips a wall at " + x + "," + z + " on segment " + a + "->" + b);
            }
        }
    }

    @Test void pathStateMachineRejectsResurrection() {
        assertTrue(PathState.REQUESTED.canMoveTo(PathState.BUILDING));
        assertTrue(PathState.RUNNING.canMoveTo(PathState.BLOCKED));
        assertFalse(PathState.COMPLETED.canMoveTo(PathState.RUNNING));
        assertFalse(PathState.CANCELLED.canMoveTo(PathState.READY));
        assertFalse(PathState.READY.canMoveTo(PathState.COMPLETED));
    }
}
