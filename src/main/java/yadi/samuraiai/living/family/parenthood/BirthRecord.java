package yadi.samuraiai.living.family.parenthood;

import java.util.List;
import java.util.UUID;

/**
 * The record of a birth: who, when (Deiliora minute and calendar year), where, the parents, family, household and generation,
 * and who witnessed it (prepared). Births are registered on purpose (a command, an event); the living world does not yet
 * produce them on its own.
 */
public record BirthRecord(UUID person, long minute, int calendarYear, UUID birthPlace, UUID parentA, UUID parentB, UUID family, UUID household, int generation, List<UUID> witnesses) {
    public BirthRecord { witnesses = witnesses == null ? List.of() : List.copyOf(witnesses); }
}
