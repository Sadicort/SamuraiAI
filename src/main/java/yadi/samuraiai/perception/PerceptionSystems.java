package yadi.samuraiai.perception;

import yadi.samuraiai.ai.perception.world.PerceptionService;

/** Chooses the perception system the brain reads: the engine when enabled, the legacy nearest-player one otherwise. */
public final class PerceptionSystems {
    private static final PerceptionSystem LEGACY = new WorldPerceptionSystem();
    private static final PerceptionSystem ENGINE = new EnginePerceptionSystem(LEGACY);

    private PerceptionSystems() { }

    public static PerceptionSystem current() { return PerceptionService.getInstance().enabled() ? ENGINE : LEGACY; }
}
