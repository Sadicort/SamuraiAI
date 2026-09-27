package yadi.samuraiai.gametest;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import yadi.samuraiai.ai.navigation.engine.NavigationFailure;
import yadi.samuraiai.ai.navigation.engine.NavigationHandle;
import yadi.samuraiai.ai.navigation.engine.NavigationOptions;
import yadi.samuraiai.ai.navigation.events.DoorClosedEvent;
import yadi.samuraiai.ai.navigation.events.DoorOpenedEvent;
import yadi.samuraiai.ai.navigation.events.PathRecalculatedEvent;
import yadi.samuraiai.ai.navigation.world.NavigationService;
import yadi.samuraiai.event.EventSubscription;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.spawn.NPCSpawnRequest;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Physical navigation tests: a real CustomNPCs avatar walks in a real world. Without CustomNPCs (chat-only
 * backend) the same requests must fail cleanly with NO_BODY instead of pretending to move.
 *
 * <p>The NPC is a merchant: with no player nearby it only rests, so its own brain never issues a competing
 * movement request while the test drives navigation directly.
 */
@GameTestHolder("samuraiai")
@PrefixGameTestTemplate(false)
public final class NavigationGameTests {
    private static final int FLOOR = 1;
    static {
        // Physical failures are diagnosed from the log; run with -Dsamuraiai.navTrace=true to keep tracing on.
        if (Boolean.getBoolean("samuraiai.navTrace"))
            yadi.samuraiai.ai.navigation.engine.NavigationSettings.apply(
                    yadi.samuraiai.ai.navigation.engine.NavigationSettings.current().toBuilder().set("debugLogging", true).build());
    }

    private NavigationGameTests() { }

    @GameTest(template = "empty", batch = "nav_door", timeoutTicks = 1400)
    public static void walksThroughAWoodenDoorAndClosesIt(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        var service = NPCSpawnService.getInstance();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(2000, 0, 0); // own patch of world: arenas of other tests must not overlap
        ServerLevel level = helper.getLevel();
        arena(level, origin, 26, 10);
        // A wall across the arena with a single wooden door in it.
        for (int z = -2; z < 12; z++) for (int y = 1; y <= 3; y++) level.setBlock(origin.offset(12, y, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        placeDoor(level, origin.offset(12, 1, 5));
        forgetTerrain(level, origin, 26, 10);
        NPCRuntime npc = spawn(level, origin, "nav_door_walker", 3, 5);
        Goal goal = new Goal(origin.getX() + 22.5D, origin.getY() + 1, origin.getZ() + 5.5D);

        if (!service.getController().requiresPhysicalBody()) { expectNoBody(helper, npc, goal); return; }

        AtomicInteger opened = new AtomicInteger(), closed = new AtomicInteger();
        EventSubscription a = NPCEventBus.getInstance().subscribe(DoorOpenedEvent.class, e -> opened.incrementAndGet());
        EventSubscription b = NPCEventBus.getInstance().subscribe(DoorClosedEvent.class, e -> closed.incrementAndGet());
        NavigationHandle handle = NavigationService.getInstance().navigate(npc, goal.location(level), NavigationOptions.walk("gametest"));
        scheduleCleanup(helper, npc.getId(), 1250);
        helper.succeedWhen(() -> {
            check(handle.done(), "still walking: " + describe(npc, handle));
            check(handle.succeeded(), "navigation ended as " + handle.state() + " " + handle.failure() + " " + handle.failureDetail() + " " + describe(npc, handle));
            check(near(npc, goal, 2.5D), "did not reach the goal: " + describe(npc, handle));
            check(opened.get() >= 1, "the door was never opened");
            check(closed.get() >= 1, "the door was never closed behind the walker");
            a.close(); b.close();
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            System.out.println("PHASE22_NAV_DOOR_OK opened=" + opened.get() + " closed=" + closed.get());
        });
    }

    @GameTest(template = "empty", batch = "nav_ledge", timeoutTicks = 1400)
    public static void jumpsOntoALedgeAndBackDown(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        var service = NPCSpawnService.getInstance();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(4000, 0, 0); // own patch of world: arenas of other tests must not overlap
        ServerLevel level = helper.getLevel();
        arena(level, origin, 20, 10);
        for (int x = 8; x <= 12; x++) for (int z = 3; z <= 7; z++) level.setBlock(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState(), 3);
        forgetTerrain(level, origin, 20, 10);
        NPCRuntime npc = spawn(level, origin, "nav_ledge_walker", 3, 5);
        Goal onLedge = new Goal(origin.getX() + 10.5D, origin.getY() + 2, origin.getZ() + 5.5D);
        Goal back = new Goal(origin.getX() + 3.5D, origin.getY() + 1, origin.getZ() + 5.5D);

        if (!service.getController().requiresPhysicalBody()) { expectNoBody(helper, npc, onLedge); return; }

        NavigationHandle up = NavigationService.getInstance().navigate(npc, onLedge.location(level), NavigationOptions.walk("gametest-up"));
        NavigationHandle[] down = new NavigationHandle[1];
        boolean[] climbed = new boolean[1];
        scheduleCleanup(helper, npc.getId(), 1250);
        // succeedWhen re-runs this body every tick, so each phase must be remembered once it has passed.
        helper.succeedWhen(() -> {
            if (!climbed[0]) {
                check(up.done(), "still climbing: " + describe(npc, up));
                check(up.succeeded(), "climb ended as " + up.state() + " " + up.failure() + " " + up.failureDetail());
                check(near(npc, onLedge, 2.0D), "not on the ledge: " + describe(npc, up));
                climbed[0] = true;
                down[0] = NavigationService.getInstance().navigate(npc, back.location(level), NavigationOptions.walk("gametest-down"));
            }
            check(down[0].done(), "still descending: " + describe(npc, down[0]));
            check(down[0].succeeded(), "descent ended as " + down[0].state() + " " + down[0].failure() + " " + down[0].failureDetail());
            check(near(npc, back, 2.5D), "not back on the floor: " + describe(npc, down[0]));
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            System.out.println("PHASE22_NAV_LEDGE_OK");
        });
    }

    @GameTest(template = "empty", batch = "nav_replan", timeoutTicks = 1400)
    public static void replansWhenAWallAppearsInFrontOfTheWalker(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        var service = NPCSpawnService.getInstance();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(6000, 0, 0); // own patch of world: arenas of other tests must not overlap
        ServerLevel level = helper.getLevel();
        arena(level, origin, 30, 14);
        NPCRuntime npc = spawn(level, origin, "nav_wall_walker", 3, 7);
        Goal goal = new Goal(origin.getX() + 26.5D, origin.getY() + 1, origin.getZ() + 7.5D);

        if (!service.getController().requiresPhysicalBody()) { expectNoBody(helper, npc, goal); return; }

        AtomicInteger recalculated = new AtomicInteger();
        EventSubscription sub = NPCEventBus.getInstance().subscribe(PathRecalculatedEvent.class, e -> recalculated.incrementAndGet());
        NavigationHandle handle = NavigationService.getInstance().navigate(npc, goal.location(level), NavigationOptions.walk("gametest"));
        scheduleCleanup(helper, npc.getId(), 1250);
        helper.runAfterDelay(25, () -> {
            for (int z = 3; z <= 11; z++) for (int y = 1; y <= 3; y++) {
                BlockPos wall = origin.offset(14, y, z);
                level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
                NavigationService.getInstance().worldChanged(level, wall);
            }
        });
        helper.succeedWhen(() -> {
            check(handle.done(), "still walking: " + describe(npc, handle));
            check(handle.succeeded(), "navigation ended as " + handle.state() + " " + handle.failure() + " " + handle.failureDetail() + " " + describe(npc, handle));
            check(near(npc, goal, 2.5D), "did not reach the goal: " + describe(npc, handle));
            check(recalculated.get() >= 1, "the wall should have forced a recalculation");
            sub.close();
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            System.out.println("PHASE22_NAV_REPLAN_OK recalculations=" + recalculated.get());
        });
    }

    @GameTest(template = "empty", batch = "nav_command", timeoutTicks = 400)
    public static void navigationCommandsAndInspectorRespond(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        var service = NPCSpawnService.getInstance();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(8000, 0, 0); // own patch of world: arenas of other tests must not overlap
        ServerLevel level = helper.getLevel();
        arena(level, origin, 20, 10);
        NPCRuntime npc = spawn(level, origin, "nav_cmd_walker", 3, 5);
        var commands = level.getServer().getCommands();
        var source = level.getServer().createCommandSourceStack().withPermission(4);
        boolean physical = service.getController().requiresPhysicalBody();
        BlockPos goal = origin.offset(12, 1, 5);
        try {
            check(commands.performPrefixedCommand(source, "samuraiai nav status") > 0, "nav status");
            if (physical) {
                check(commands.performPrefixedCommand(source, "samuraiai nav goto nav_cmd_walker " + goal.getX() + " " + goal.getY() + " " + goal.getZ()) > 0, "nav goto");
                check(commands.performPrefixedCommand(source, "samuraiai nav inspect nav_cmd_walker") > 0, "nav inspect");
                check(commands.performPrefixedCommand(source, "samuraiai nav terrain nav_cmd_walker") > 0, "nav terrain");
                check(commands.performPrefixedCommand(source, "samuraiai nav danger " + goal.getX() + " " + goal.getY() + " " + goal.getZ() + " 3 60 30") > 0, "nav danger");
                check(commands.performPrefixedCommand(source, "samuraiai nav status") > 0, "nav status with sessions");
                check(commands.performPrefixedCommand(source, "samuraiai nav cancel nav_cmd_walker") > 0, "nav cancel");
                check(NavigationService.getInstance().session(npc.getId()).map(x -> x.state().terminal()).orElse(false), "cancel ends the session");
            } else {
                check(commands.performPrefixedCommand(source, "samuraiai nav goto nav_cmd_walker " + goal.getX() + " " + goal.getY() + " " + goal.getZ()) == 0,
                        "goto must be refused for a body-less NPC");
            }
        } finally {
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
        }
        System.out.println("PHASE22_NAV_COMMANDS_OK physical=" + physical);
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** Retryable assertion: succeedWhen keeps polling while this throws. */
    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private record Goal(double x, double y, double z) {
        SpawnLocation location(ServerLevel level) { return new SpawnLocation(level.dimension().location().toString(), x, y, z, 0.0F); }
    }

    private static void arena(ServerLevel level, BlockPos origin, int width, int depth) {
        TestTerrain.flatten(level, origin, width, depth);
    }

    /** Blocks set straight into the level raise no Forge event, so tell navigation its cached terrain here is stale. */
    private static void forgetTerrain(ServerLevel level, BlockPos origin, int width, int depth) {
        for (int cx = (origin.getX() - 4) >> 4; cx <= (origin.getX() + width + 4) >> 4; cx++)
            for (int cz = (origin.getZ() - 4) >> 4; cz <= (origin.getZ() + depth + 4) >> 4; cz++)
                NavigationService.getInstance().chunkChanged(level, cx, cz);
    }

    private static void placeDoor(ServerLevel level, BlockPos lowerPos) {
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST);
        level.setBlock(lowerPos, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(lowerPos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
    }

    private static NPCRuntime spawn(ServerLevel level, BlockPos origin, String name, int dx, int dz) {
        var result = NPCSpawnService.getInstance().spawn(new NPCSpawnRequest(NPCTypeId.of("merchant"), name,
                new SpawnLocation(level.dimension().location().toString(), origin.getX() + dx + 0.5D, origin.getY() + 1, origin.getZ() + dz + 0.5D, 0.0F)));
        if (!result.success()) throw new AssertionError("spawn failed: " + result.message());
        return NPCManager.getInstance().find(result.instance().getIdentity().id()).orElseThrow();
    }

    private static void expectNoBody(GameTestHelper helper, NPCRuntime npc, Goal goal) {
        var handle = NavigationService.getInstance().navigate(npc, goal.location(helper.getLevel()), NavigationOptions.walk("gametest"));
        check(handle.done() && handle.failure() == NavigationFailure.NO_BODY,
                "a body-less NPC must fail with NO_BODY, got " + handle.state() + " " + handle.failure());
        NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
        System.out.println("PHASE22_NAV_NOBODY_OK");
        helper.succeed();
    }

    private static boolean near(NPCRuntime npc, Goal goal, double tolerance) {
        npc.getController().synchronize(npc.getInstance());
        var at = npc.getInstance().getLocation();
        return at != null && Math.hypot(at.x() - goal.x(), at.z() - goal.z()) <= tolerance && Math.abs(at.y() - goal.y()) <= 1.6D;
    }

    private static String describe(NPCRuntime npc, NavigationHandle handle) {
        npc.getController().synchronize(npc.getInstance());
        var at = npc.getInstance().getLocation();
        String where = at == null ? "?" : String.format("%.1f,%.1f,%.1f", at.x(), at.y(), at.z());
        var session = NavigationService.getInstance().session(npc.getId());
        return "state=" + handle.state() + " at=" + where + " level=" + npc.getInstance().getLocation() + " progress=" + String.format("%.2f", handle.progress())
                + session.map(s -> " last=" + s.lastEvent() + " recalcs=" + s.recalculations()).orElse("")
                + " dangers=" + NavigationService.getInstance().dimensions().stream().flatMap(d -> d.dangers().activeZones(d.key(), NavigationService.getInstance().currentTick()).stream()).map(Object::toString).toList();
    }

    private static void scheduleCleanup(GameTestHelper helper, UUID npcId, long tick) {
        helper.runAfterDelay(tick, () -> {
            if (NPCManager.getInstance().find(npcId).isPresent()) NPCSpawnService.getInstance().remove(npcId, "gametest timeout", true);
        });
    }
}
