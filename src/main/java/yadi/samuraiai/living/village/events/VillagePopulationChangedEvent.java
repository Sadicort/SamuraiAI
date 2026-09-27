package yadi.samuraiai.living.village.events;

/** A village's population changed; consumption recalculates on it. */
public record VillagePopulationChangedEvent(long minute, java.util.UUID villageId, int population, java.util.Map<String, Integer> professions) implements VillageEngineEvent { }
