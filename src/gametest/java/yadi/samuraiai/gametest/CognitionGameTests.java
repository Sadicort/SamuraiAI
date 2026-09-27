package yadi.samuraiai.gametest;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import yadi.samuraiai.ai.cognition.engine.CognitionSettings;
import yadi.samuraiai.ai.cognition.engine.ExperienceInput;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.world.CognitionService;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.npc.relationship.RelationshipService;
import yadi.samuraiai.spawn.NPCSpawnRequest;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.world.SpawnLocation;

/**
 * The cognitive layer inside a real server: experiences become memory and feelings that the older emotion and relationship
 * services (and so the scheduler and the prompts) read, personality drifts into the scheduler, a mind survives being unloaded, a
 * rumour travels between two neighbours and the commands answer. Each test owns a patch of world and puts the settings back.
 */
@GameTestHolder("samuraiai")
@PrefixGameTestTemplate(false)
public final class CognitionGameTests {
    private static final int FLOOR = 1;

    private CognitionGameTests() { }

    private static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }

    private static NPCRuntime spawn(ServerLevel level, BlockPos origin, String name, int dx, int dz) { return spawn(level, origin, "merchant", name, dx, dz); }

    private static NPCRuntime spawn(ServerLevel level, BlockPos origin, String type, String name, int dx, int dz) {
        var result = NPCSpawnService.getInstance().spawn(new NPCSpawnRequest(NPCTypeId.of(type), name,
                new SpawnLocation(level.dimension().location().toString(), origin.getX() + dx + 0.5D, origin.getY() + 1, origin.getZ() + dz + 0.5D, 0.0F)));
        if (!result.success()) throw new AssertionError("spawn failed: " + result.message());
        return NPCManager.getInstance().find(result.instance().getIdentity().id()).orElseThrow();
    }

    private static void remove(NPCRuntime... npcs) {
        for (NPCRuntime npc : npcs) if (NPCManager.getInstance().find(npc.getId()).isPresent()) NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
    }

    private static BlockPos patch(GameTestHelper helper, ServerLevel level, int offset) {
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(offset, 0, 0);
        TestTerrain.flatten(level, origin, 24, 16);
        return origin;
    }

    @GameTest(template = "empty", batch = "cog", timeoutTicks = 300)
    public static void experiencesBecomeMemoryFeelingsAndTrustThatTheOlderServicesRead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = patch(helper, level, 26000);
        NPCRuntime npc = spawn(level, origin, "cog_feel_npc", 4, 4);
        var service = CognitionService.getInstance();
        UUID player = UUID.randomUUID();
        helper.runAfterDelay(10, () -> {
            try {
                check(service.engine().isLoaded(npc.getId()), "the NPC's mind was loaded when it activated");
                var out = service.experience(npc.getId(), ExperienceInput.of(npc.getId(), ExperienceKind.ATTACKED_ME, service.now()).actor(EntityRef.player(player, "Tester")));
                check(out.kept(), "the attack was kept as a memory: " + service.engine().metrics().lastError);
                check(service.engine().memory().runtime(npc.getId()).size() == 1, "one memory");
                check(npc.getEmotionState().get(Emotion.FEAR) >= 20, "the older emotion state carries the fear: " + npc.getEmotionState().get(Emotion.FEAR));
                var legacy = RelationshipService.getInstance().find(npc.getId(), player);
                check(legacy.isPresent() && legacy.get().getTrust() < 0, "the older relationship service sees distrust");
                check(service.engine().knowledge().runtime(npc.getId()).size() >= 1, "and it learned that the attacker attacks");
                // A mind survives being unloaded and loaded again.
                service.engine().saveAll(service.now());
                service.engine().unload(npc.getId(), service.now());
                check(service.engine().memory().peek(npc.getId()).isEmpty(), "unloaded");
                service.engine().ensureLoaded(npc.getId(), service.now());
                check(service.engine().memory().runtime(npc.getId()).size() == 1, "it remembers after coming back");
                check(service.engine().relationships().find(npc.getId(), player).isPresent(), "and still has its relationship");
                check(service.engine().storage().bytesOf(npc.getId()) > 0, "there is something on disk");
                System.out.println("PHASE3_COG_FEEL_OK");
                helper.succeed();
            } finally { remove(npc); }
        });
    }

    @GameTest(template = "empty", batch = "cog", timeoutTicks = 400)
    public static void lastingExperienceMovesTheSchedulersPersonalitySlowly(GameTestHelper helper) {
        GameTestIsolation.withScheduler();
        ServerLevel level = helper.getLevel();
        BlockPos origin = patch(helper, level, 27000);
        NPCRuntime npc = spawn(level, origin, "cog_person_npc", 4, 4);
        var service = CognitionService.getInstance();
        helper.runAfterDelay(80, () -> {
            try {
                var schedule = SchedulerService.getInstance().scheduler().schedule(npc.getId());
                check(schedule.isPresent(), "the scheduler has looked at the NPC");
                double before = schedule.get().traits().get(yadi.samuraiai.ai.scheduler.personality.Trait.CAUTION);
                for (int i = 0; i < 6; i++)
                    service.experience(npc.getId(), ExperienceInput.of(npc.getId(), ExperienceKind.ATTACKED_ME, service.now() + i * 100L).actor(EntityRef.player(UUID.randomUUID(), "P" + i)));
                double after = schedule.get().traits().get(yadi.samuraiai.ai.scheduler.personality.Trait.CAUTION);
                double moved = after - before;
                check(moved > 0.2, "caution grew in the scheduler: " + moved);
                check(moved <= CognitionSettings.current().evolutionDailyLimit() + 0.01, "but no more than the daily limit: " + moved);
                check(Math.abs(service.engine().ledger(npc.getId()).evolution(Trait.CAUTION) - moved) < 0.01,
                        "the ledger and the scheduler agree on how far it moved");
                System.out.println("PHASE3_COG_PERSONALITY_OK");
                helper.succeed();
            } finally { remove(npc); }
        });
    }

    @GameTest(template = "empty", batch = "cog", timeoutTicks = 520)
    public static void aRumourTravelsBetweenNeighboursAsARumourNotAFact(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = patch(helper, level, 28000);
        CognitionSettings previousCognition = CognitionSettings.current();
        KnowledgeSettings previousKnowledge = KnowledgeSettings.current();
        CognitionSettings.apply(CognitionSettings.builder().set("gossipScanTicks", 20).build());
        KnowledgeSettings.apply(KnowledgeSettings.builder().set("baseDelayTicks", 0).set("delayPerBlock", 0.0D).set("gossipCooldownTicks", 0).set("maxHops", 100).build());
        // Different lifestyles, so that the two neighbours do not form a scheduler group that other tests would count.
        NPCRuntime witness = spawn(level, origin, "merchant", "cog_witness", 4, 4), listener = spawn(level, origin, "samurai", "cog_listener", 7, 4);
        var service = CognitionService.getInstance();
        UUID attacker = UUID.randomUUID();
        boolean[] started = {false};
        helper.succeedWhen(() -> {
            if (!started[0]) {
                if (!service.engine().isLoaded(witness.getId()) || !service.engine().isLoaded(listener.getId())) throw new GameTestAssertException("minds not loaded yet");
                started[0] = true;
                service.experience(witness.getId(), ExperienceInput.of(witness.getId(), ExperienceKind.WITNESSED_ATTACK, service.now()).actor(EntityRef.player(attacker, "Bandit"))
                        .target(EntityRef.npc(listener.getId(), "cog_listener")).witnessed(true).publicEvent(true));
                check(!service.engine().society().rumors().isEmpty(), "the witness started a rumour");
            }
            var known = service.engine().knowledge().runtime(listener.getId()).all().stream().filter(r -> r.rumorId() != null).findFirst();
            if (known.isEmpty()) throw new GameTestAssertException("the rumour has not reached the neighbour yet: queue=" + service.engine().society().queue().size() + " witnessKnowledge=" + service.engine().knowledge().runtime(witness.getId()).size() + " rumors=" + service.engine().society().rumors().size() + " listenerKnowledge=" + service.engine().knowledge().runtime(listener.getId()).size() + " metrics=" + service.engine().knowledge().metrics().snapshot() + " loaded=" + service.engine().isLoaded(listener.getId()) + " err=" + service.engine().metrics().lastError);
            check(known.get().state() != yadi.samuraiai.ai.knowledge.model.ValidationState.VERIFIED, "heard, not verified: " + known.get().state());
            check(known.get().source() != null && known.get().source().id().equals(witness.getId()), "it knows who told it");
            check(!service.engine().relationships().reputationOf(listener.getId(), attacker).isEmpty(), "and its view of the attacker's reputation changed");
            // A test that succeeds early stops ticking, so the delayed cleanup below would never run: clean up here too, or the
            // pair is saved with the world and restored next to the next run's pair, where it swallows the rumour.
            CognitionSettings.apply(previousCognition); KnowledgeSettings.apply(previousKnowledge); remove(witness, listener);
            System.out.println("PHASE3_COG_RUMOR_OK");
        });
        helper.runAfterDelay(499, () -> { CognitionSettings.apply(previousCognition); KnowledgeSettings.apply(previousKnowledge); remove(witness, listener); });
    }

    @GameTest(template = "empty", batch = "cog", timeoutTicks = 300)
    public static void theCognitiveCommandsRespondAndExplainWhyAnNpcFeelsWhatItFeels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = patch(helper, level, 29000);
        NPCRuntime npc = spawn(level, origin, "cog_cmd_npc", 4, 4);
        var commands = level.getServer().getCommands();
        var source = level.getServer().createCommandSourceStack().withPermission(4);
        helper.runAfterDelay(20, () -> {
            try {
                for (String command : List.of("samuraiai mind status", "samuraiai mind inspect cog_cmd_npc", "samuraiai memory status", "samuraiai relationship status", "samuraiai emotion status",
                        "samuraiai knowledge status", "samuraiai society status", "samuraiai society cultures", "samuraiai society communities"))
                    check(commands.performPrefixedCommand(source, command) > 0, command);
                check(commands.performPrefixedCommand(source, "samuraiai emotion trigger cog_cmd_npc FEAR 60") > 0, "emotion trigger");
                check(CognitionService.getInstance().engine().emotions().intensity(npc.getId(), EmotionKind.FEAR) > 20, "the fear was felt");
                check(commands.performPrefixedCommand(source, "samuraiai mind why cog_cmd_npc emotion FEAR") > 0, "why");
                check(commands.performPrefixedCommand(source, "samuraiai emotion show cog_cmd_npc") > 0, "emotion show");
                check(commands.performPrefixedCommand(source, "samuraiai memory list cog_cmd_npc") > 0, "memory list");
                check(commands.performPrefixedCommand(source, "samuraiai society create gt_cog_hamlet VILLAGE") >= 0, "society create");
                check(commands.performPrefixedCommand(source, "samuraiai society join cog_cmd_npc gt_cog_hamlet MEMBERS") >= 0, "society join");
                System.out.println("PHASE3_COG_COMMANDS_OK");
                helper.succeed();
            } finally {
                CognitionService.getInstance().engine().society().remove("gt_cog_hamlet");
                remove(npc);
            }
        });
    }
}
