package yadi.samuraiai.runtime;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.perception.PerceptionSystem;
import yadi.samuraiai.perception.WorldPerceptionSystem;

/**
 * Drives every active NPC's Brain from the server tick.
 *
 * <p>This is the piece the mod was missing entirely. NPCs were created, given
 * a Brain and registered, and then nothing ever called {@link
 * yadi.samuraiai.brain.Brain#tick} — so no NPC in the world ever had a single
 * thought. Decision engine, behaviours, goals and emotions were all
 * unreachable code.
 *
 * <p>Three things keep this cheap enough to run every tick:
 * <ul>
 *   <li>brains run on an interval (one second by default), not 20 times a
 *       second;</li>
 *   <li>NPCs are staggered across that interval on first sight, so 50 of them
 *       do not all think on the same tick and spike the frame;</li>
 *   <li>an NPC with injected work — a player just spoke to it — runs
 *       immediately instead of waiting out the rest of its interval.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class NPCTickService {

    /**
     * Marker for "this NPC has never thought", matching the initial value of
     * NPCRuntime.lastBrainTick. A sentinel rather than 0 because a staggered
     * start can legitimately compute a last-tick of 0 or a negative one.
     */
    private static final long NEVER_TICKED = Long.MIN_VALUE;

    private static final PerceptionSystem PERCEPTION = new WorldPerceptionSystem();

    private static long serverTick;
    private static volatile yadi.samuraiai.foundation.scheduler.TickBudget.Snapshot lastBudget =
            new yadi.samuraiai.foundation.scheduler.TickBudget(1, java.time.Duration.ofMillis(4)).snapshot();
    private static final java.util.Map<java.util.UUID, java.util.Set<java.util.UUID>> perceived = new java.util.HashMap<>();

    public static void forget(java.util.UUID id) { perceived.remove(id); }

    private NPCTickService() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {

        // END only: running at both phases would double every NPC's think rate.
        if (event.phase != TickEvent.Phase.END || !ServerScheduler.getInstance().isRunning()) {
            return;
        }

        serverTick++;

        NPCManager manager = NPCManager.getInstance();

        if (manager.isEmpty()) {
            return;
        }

        int interval = Math.max(1, SamuraiSettings.brainTickInterval());
        int expectedDue = Math.max(1, (manager.count() + interval - 1) / interval);
        var budget = new yadi.samuraiai.foundation.scheduler.TickBudget(Math.max(2, expectedDue * 2), java.time.Duration.ofMillis(4));

        for (NPCRuntime runtime : manager.getActive()) {
            if (!runtime.isActive()) continue;

            if (!isDue(runtime, interval)) {
                continue;
            }
            if (!budget.tryAcquire()) break;

            runtime.setLastBrainTick(serverTick);

            try {
                think(runtime);
            } catch (RuntimeException e) {
                // One NPC's failure must never break the server tick loop or
                // stop the other NPCs from thinking.
                SamuraiLogger.BRAIN.error("El cerebro de {} fallo: {}",
                        runtime.getName(), e.toString(), e);
            }
        }
        lastBudget = budget.snapshot();
    }

    private static boolean isDue(NPCRuntime runtime, int interval) {

        if (runtime.getBrain().hasPendingWork()) {
            return true;
        }

        long last = runtime.getLastBrainTick();

        if (last == NEVER_TICKED) {
            // Spread first thoughts deterministically over one interval, so a
            // batch of NPCs spawned together does not stay in lockstep forever.
            long offset = Math.floorMod(runtime.getId().hashCode(), interval);
            runtime.setLastBrainTick(serverTick - interval + offset);
            return false;
        }

        return serverTick - last >= interval;
    }

    private static void think(NPCRuntime runtime) {
        var controller = runtime.getController();
        if (controller.requiresPhysicalBody() && !controller.isPhysicalPresent(runtime.getInstance())) {
            yadi.samuraiai.spawn.NPCSpawnService.getInstance().remove(runtime.getId(), "avatar absent", true);
            return;
        }
        controller.synchronize(runtime.getInstance());

        WorldContext world = yadi.samuraiai.perception.PerceptionSystems.current().perceive(runtime)
                .withAdvice(yadi.samuraiai.ai.scheduler.world.SchedulerService.getInstance().adviceFor(runtime.getId()).orElse(null))
                .withCognition(yadi.samuraiai.ai.cognition.world.CognitionService.getInstance().adviceFor(runtime.getId()).orElse(null));
        var previous = perceived.getOrDefault(runtime.getId(), java.util.Set.of());
        var next = new java.util.HashSet<java.util.UUID>();
        for (var entity : world.getPlayers()) {
            next.add(entity.id());
            if (!previous.contains(entity.id()))
                yadi.samuraiai.event.NPCEventBus.getInstance().post(new yadi.samuraiai.event.PlayerApproachEvent(
                        runtime.getId(), entity.id(), entity.name(), entity.distance()));
        }
        if (!runtime.isActive()) return;
        perceived.put(runtime.getId(), java.util.Set.copyOf(next));
        yadi.samuraiai.event.NPCEventBus.getInstance().post(new yadi.samuraiai.event.npc.PerceptionUpdatedEvent(runtime.getId(), world));
        if (!runtime.isActive()) return;
        NPCContext context = NPCContext.of(runtime);

        runtime.getInstance().markSeenNow();
        runtime.getBrain().tick(context, world, runtime, runtime.getController());
    }

    /** Ticks elapsed since the server started, for diagnostics. */
    public static long currentTick() {
        return serverTick;
    }
    public static yadi.samuraiai.foundation.scheduler.TickBudget.Snapshot lastBudget() { return lastBudget; }

    /** Resets the counter on server shutdown so a new world starts clean. */
    public static void reset() {
        serverTick = 0L;
        perceived.clear();
        lastBudget = new yadi.samuraiai.foundation.scheduler.TickBudget(1, java.time.Duration.ofMillis(4)).snapshot();
    }
}
