package yadi.samuraiai.ai.scheduler.energy;

import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/** What a worn-down NPC needs, and the routines that answer the need (in order of preference). */
public enum EnergyNeed {
    SLEEP(RoutineType.SLEEP), REST(RoutineType.REST), REFUEL(RoutineType.EAT), CALM_DOWN(RoutineType.MEDITATE), MOTIVATE(RoutineType.SOCIAL);

    private final RoutineType remedy;
    EnergyNeed(RoutineType remedy) { this.remedy = remedy; }
    public RoutineType remedy() { return remedy; }
}
