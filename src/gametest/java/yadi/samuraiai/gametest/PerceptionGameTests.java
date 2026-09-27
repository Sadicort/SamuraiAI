package yadi.samuraiai.gametest;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import yadi.samuraiai.ai.navigation.engine.NavigationOptions;
import yadi.samuraiai.ai.navigation.world.NavigationService;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.awareness.ThreatLevel;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.ai.perception.hearing.SoundCategory;
import yadi.samuraiai.ai.perception.world.PerceptionService;
import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.spawn.NPCSpawnRequest;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Perception in a real server: real entities are seen (or hidden by real blocks), real Forge events become sounds and damage
 * reports, and what an NPC perceives changes what its brain does. Runs with and without CustomNPCs: a body-less NPC perceives
 * from its spawn point, so the sensing itself is testable in both profiles.
 */
@GameTestHolder("samuraiai")
@PrefixGameTestTemplate(false)
public final class PerceptionGameTests {
    private static final int FLOOR = 1;

    private PerceptionGameTests() { }

    private static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }

    @GameTest(template = "empty", batch = "perc_vision", timeoutTicks = 500)
    public static void seesAHostileAndTreatsItAsAThreat(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0));
        ServerLevel level = helper.getLevel();
        arena(level, origin, 24, 14);
        NPCRuntime npc = spawn(level, origin, "merchant", "perc_vision_npc", 5, 2);
        Mob creeper = helper.spawn(EntityType.CREEPER, new BlockPos(5, FLOOR + 1, 10));
        creeper.setNoAi(true);
        creeper.setPersistenceRequired();
        UUID creeperId = creeper.getUUID();
        cleanupAt(helper, 480, npc, creeper);
        helper.runAfterDelay(100, () -> { if (Boolean.getBoolean("samuraiai.navTrace")) System.out.println("PERC_VISION_TRACE " + PerceptionService.getInstance().snapshot(npc.getId()).map(x -> x.awareness() + " targets=" + x.targets() + " env=" + x.environment()).orElse("none") + " npcAt=" + npc.getInstance().getLocation() + " creeper=" + creeper.position()); });
        helper.succeedWhen(() -> {
            PerceptionSnapshot snap = snapshot(npc);
            check(snap.seenTargets().stream().anyMatch(t -> t.id().equals(creeperId) && t.kind() == EntityClass.HOSTILE), "the creeper is not seen: " + snap.targets());
            check(snap.threatLevel().atLeast(ThreatLevel.WARNING), "a hostile in view is a threat: " + snap.threatLevel() + " " + (int) snap.threatScore());
            check(snap.awareness().atLeast(AwarenessLevel.TRACKING), "awareness " + snap.awareness());
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            creeper.discard();
            System.out.println("PHASE23_PERC_VISION_OK awareness=" + snap.awareness() + " threat=" + snap.threatLevel());
        });
    }

    @GameTest(template = "empty", batch = "perc_wall", timeoutTicks = 500)
    public static void aRealWallHidesTheHostile(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0));
        ServerLevel level = helper.getLevel();
        arena(level, origin, 24, 14);
        for (int x = 1; x <= 9; x++) for (int y = 1; y <= 4; y++) level.setBlock(origin.offset(x, y, 6), Blocks.STONE.defaultBlockState(), 3);
        NPCRuntime npc = spawn(level, origin, "merchant", "perc_wall_npc", 5, 2);
        Mob creeper = helper.spawn(EntityType.CREEPER, new BlockPos(5, FLOOR + 1, 10));
        creeper.setNoAi(true);
        creeper.setPersistenceRequired();
        UUID creeperId = creeper.getUUID();
        cleanupAt(helper, 480, npc, creeper);
        helper.runAfterDelay(120, () -> {
            try {
            PerceptionSnapshot snap = snapshot(npc);
            check(snap.seenTargets().stream().noneMatch(t -> t.id().equals(creeperId)), "the NPC must not see through a stone wall: " + snap.seenTargets());
            check(snap.threatLevel() == ThreatLevel.SAFE, "nothing threatening is perceived: " + snap.threatLevel() + " threats=" + snap.threats() + " targets=" + snap.targets());
            System.out.println("PHASE23_PERC_WALL_OK");
            } finally {
                // a failed check must not leave the NPC (and its beliefs) behind for the next test
                NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
                creeper.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "perc_sound", timeoutTicks = 500)
    public static void aBlockBrokenNearbyIsHeardAndInvestigated(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0));
        ServerLevel level = helper.getLevel();
        arena(level, origin, 24, 14);
        NPCRuntime npc = spawn(level, origin, "merchant", "perc_sound_npc", 3, 3);
        cleanupAt(helper, 480, npc);
        BlockPos broken = origin.offset(10, 1, 3);
        level.setBlock(broken, Blocks.STONE.defaultBlockState(), 3);
        // A real Forge break event, exactly as a player mining would produce: the perception hook must turn it into a sound.
        // (the handler is called directly: posting a mock player on the bus makes CustomNPCs' own listener cast it to a real player)
        yadi.samuraiai.ai.perception.world.PerceptionEvents.onBreak(new BlockEvent.BreakEvent(level, broken, level.getBlockState(broken), helper.makeMockPlayer()));
        helper.succeedWhen(() -> {
            PerceptionSnapshot snap = snapshot(npc);
            check(snap.recentSounds().stream().anyMatch(h -> h.sound().category() == SoundCategory.BLOCK_BREAK), "the break was not heard: " + snap.recentSounds());
            var target = snap.investigationTarget();
            check(target.isPresent() || snap.suspicion() > 0.0D, "an unexplained noise raises suspicion or an investigation target");
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            System.out.println("PHASE23_PERC_SOUND_OK suspicion=" + (int) snap.suspicion());
        });
    }

    @GameTest(template = "empty", batch = "perc_damage", timeoutTicks = 500)
    public static void beingHurtIsCriticalEvidence(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        if (!NPCSpawnService.getInstance().getController().requiresPhysicalBody()) { helper.succeed(); return; }
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0));
        ServerLevel level = helper.getLevel();
        arena(level, origin, 20, 12);
        NPCRuntime npc = spawn(level, origin, "merchant", "perc_damage_npc", 4, 4);
        cleanupAt(helper, 480, npc);
        helper.runAfterDelay(10, () -> {
            var body = npc.getController().movementBody(npc.getInstance()).orElseThrow();
            ((yadi.samuraiai.ai.navigation.world.MobMovementBody) body).mob().hurt(DamageSource.GENERIC, 4.0F);
        });
        helper.succeedWhen(() -> {
            PerceptionSnapshot snap = snapshot(npc);
            check(snap.threatLevel() == ThreatLevel.CRITICAL, "damage is critical: " + snap.threatLevel());
            check(snap.focus() != null && snap.focus().category().name().equals("DAMAGE_TAKEN"), "attention goes to the damage: " + snap.focus());
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            System.out.println("PHASE23_PERC_DAMAGE_OK");
        });
    }

    @GameTest(template = "empty", batch = "perc_bridge", timeoutTicks = 600)
    public static void perceivedThreatsBecomeDangerForNavigation(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        if (!NPCSpawnService.getInstance().getController().requiresPhysicalBody()) { helper.succeed(); return; }
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0));
        ServerLevel level = helper.getLevel();
        arena(level, origin, 24, 14);
        NPCRuntime npc = spawn(level, origin, "merchant", "perc_bridge_npc", 5, 2);
        // Make sure navigation has an engine for this dimension, as it would once any NPC walks.
        NavigationService.getInstance().navigate(npc, new SpawnLocation(level.dimension().location().toString(), origin.getX() + 5.5D, origin.getY() + 1, origin.getZ() + 4.5D, 0.0F),
                NavigationOptions.walk("gametest"));
        Mob creeper = helper.spawn(EntityType.CREEPER, new BlockPos(5, FLOOR + 1, 10));
        creeper.setNoAi(true);
        creeper.setPersistenceRequired();
        cleanupAt(helper, 580, npc, creeper);
        helper.succeedWhen(() -> {
            String dimension = level.dimension().location().toString();
            boolean zone = NavigationService.getInstance().dimensions().stream().anyMatch(d ->
                    d.dangers().activeZones(dimension, NavigationService.getInstance().currentTick()).stream().anyMatch(z -> z.source().equals("src:perception")));
            check(zone, "the creeper the NPC sees must appear as a hazard in the navigation danger map");
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            creeper.discard();
            System.out.println("PHASE23_PERC_BRIDGE_OK");
        });
    }

    @GameTest(template = "empty", batch = "perc_commands", timeoutTicks = 300)
    public static void perceptionCommandsRespond(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0));
        ServerLevel level = helper.getLevel();
        arena(level, origin, 16, 12);
        NPCRuntime npc = spawn(level, origin, "merchant", "perc_cmd_npc", 4, 4);
        var commands = level.getServer().getCommands();
        var source = level.getServer().createCommandSourceStack().withPermission(4);
        cleanupAt(helper, 280, npc);
        helper.runAfterDelay(60, () -> {
            try {
                check(commands.performPrefixedCommand(source, "samuraiai perception status") > 0, "perception status");
                check(commands.performPrefixedCommand(source, "samuraiai perception inspect perc_cmd_npc") > 0, "perception inspect");
                BlockPos at = origin.offset(8, 1, 4);
                check(commands.performPrefixedCommand(source, "samuraiai perception sound " + at.getX() + " " + at.getY() + " " + at.getZ() + " DOOR 0.9") > 0, "perception sound");
            } finally { NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true); }
            System.out.println("PHASE23_PERC_COMMANDS_OK");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "perc_investigate", timeoutTicks = 1500)
    public static void aFarSoundMakesTheSamuraiInvestigate(GameTestHelper helper) {
        GameTestIsolation.soloEngines();
        boolean physical = NPCSpawnService.getInstance().getController().requiresPhysicalBody();
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0));
        ServerLevel level = helper.getLevel();
        arena(level, origin, 34, 12);
        NPCRuntime npc = spawn(level, origin, "samurai", "perc_investigate_npc", 3, 5);
        final double startZ = origin.getZ() + 5.5D, startX = origin.getX() + 3.5D;
        cleanupAt(helper, 1300, npc);
        Set<GoalType> goals = new HashSet<>();
        helper.runAfterDelay(30, () ->
                PerceptionService.getInstance().sound(level, SoundCategory.EXPLOSION, null, origin.getX() + 28.5D, origin.getY() + 1, origin.getZ() + 5.5D, 1.0D));
        long[] ticks = {0};
        helper.onEachTick(() -> {
            if (npc.getCurrentGoal() != null) goals.add(npc.getCurrentGoal().getType());
            if (++ticks[0] % 40 == 0 && Boolean.getBoolean("samuraiai.navTrace")) {
                npc.getController().synchronize(npc.getInstance());
                var at = npc.getInstance().getLocation();
                var snap = PerceptionService.getInstance().snapshot(npc.getId());
                var session = NavigationService.getInstance().session(npc.getId());
                System.out.println("INV_TRACE t=" + ticks[0] + " goal=" + npc.getCurrentGoal() + " x=" + (at == null ? "?" : String.format("%.1f", at.x() - startX))
                        + " aware=" + snap.map(x -> x.awareness().name()).orElse("-") + " target=" + snap.map(x -> x.investigationTarget().map(i -> String.format("%.0f,%.0f", i.x() - startX, i.z() - startZ)).orElse("none")).orElse("-")
                        + " nav=" + session.map(x -> x.state() + "/" + x.lastEvent()).orElse("none"));
            }
        });
        helper.succeedWhen(() -> {
            check(goals.contains(GoalType.INVESTIGATE), "the brain never chose to investigate; goals seen: " + goals);
            if (physical) {
                npc.getController().synchronize(npc.getInstance());
                var at = npc.getInstance().getLocation();
                check(at != null && at.x() - startX >= 6.0D, "the samurai should be walking toward the sound, x moved " + (at == null ? "?" : String.format("%.1f", at.x() - startX)));
            }
            NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
            System.out.println("PHASE23_PERC_INVESTIGATE_OK goals=" + goals);
        });
    }

    // ------------------------------------------------------------------ helpers

    /** Removes the NPC and any test entities even when the test fails, so nothing leaks into the next test. */
    private static void cleanupAt(GameTestHelper helper, long tick, NPCRuntime npc, Mob... entities) {
        helper.runAfterDelay(tick, () -> {
            if (NPCManager.getInstance().find(npc.getId()).isPresent()) NPCSpawnService.getInstance().remove(npc.getId(), "gametest cleanup", true);
            for (Mob entity : entities) if (entity != null && !entity.isRemoved()) entity.discard();
        });
    }

    private static PerceptionSnapshot snapshot(NPCRuntime npc) {
        var snap = PerceptionService.getInstance().snapshot(npc.getId());
        check(snap.isPresent(), "no perception snapshot yet");
        return snap.get();
    }

    private static void arena(ServerLevel level, BlockPos origin, int width, int depth) {
        TestTerrain.flatten(level, origin, width, depth);
    }

    private static NPCRuntime spawn(ServerLevel level, BlockPos origin, String type, String name, int dx, int dz) {
        var result = NPCSpawnService.getInstance().spawn(new NPCSpawnRequest(NPCTypeId.of(type), name,
                new SpawnLocation(level.dimension().location().toString(), origin.getX() + dx + 0.5D, origin.getY() + 1, origin.getZ() + dz + 0.5D, 0.0F)));
        if (!result.success()) throw new AssertionError("spawn failed: " + result.message());
        return NPCManager.getInstance().find(result.instance().getIdentity().id()).orElseThrow();
    }
}
