package yadi.samuraiai.living.family.lifecycle;

/**
 * Where a person is in life, as far as the family record goes. UNBORN_FUTURE and DECEASED_FUTURE are prepared states: births
 * are only registered on purpose (a command, an event) and natural death is not simulated yet, but a death that happens in
 * the world (a killed NPC) is recorded as DECEASED_FUTURE. HISTORICAL is an ancestor known only as a record.
 */
public enum LifeState {
    UNBORN_FUTURE, ALIVE, MISSING, MIGRATED, DECEASED_FUTURE, HISTORICAL;

    public boolean living() { return this == ALIVE || this == MIGRATED; }
    public boolean gone() { return this == MISSING || this == DECEASED_FUTURE || this == HISTORICAL; }
}
