package yadi.samuraiai.ai.scheduler.routine;

import yadi.samuraiai.ai.scheduler.zone.ZoneKind;

/** The twelve things an NPC's day is made of, each tied to the kind of place it happens in. */
public enum RoutineType {
    WAKE(ZoneKind.HOME), PATROL(ZoneKind.PATROL_ROUTE), WORK(ZoneKind.WORK), REST(ZoneKind.REST_AREA), EAT(ZoneKind.DINING),
    MEDITATE(ZoneKind.TEMPLE), SLEEP(ZoneKind.HOME), SOCIAL(ZoneKind.PLAZA), TRAINING(ZoneKind.TRAINING), PRAYER(ZoneKind.TEMPLE),
    GUARD(ZoneKind.GUARD_POST), MERCHANT(ZoneKind.MARKET);

    private final ZoneKind zoneKind;
    RoutineType(ZoneKind zoneKind) { this.zoneKind = zoneKind; }
    public ZoneKind zoneKind() { return zoneKind; }
}
