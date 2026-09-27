package yadi.samuraiai.gametest;

import yadi.samuraiai.ai.scheduler.world.SchedulerConfig;

/**
 * Physical tests exercise one engine at a time. The scheduler drives NPCs through the Brain, and two drivers on one NPC would
 * make a navigation or perception test measure the wrong thing, so those tests switch the scheduler off and the scheduler's
 * own tests switch it back on.
 */
final class GameTestIsolation {
    private GameTestIsolation() { }

    static void soloEngines() { SchedulerConfig.forceEnabled(false); }

    static void withScheduler() { SchedulerConfig.forceEnabled(true); }
}
