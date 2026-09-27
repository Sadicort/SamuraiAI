package yadi.samuraiai.living.village.events;

/** The village has citizens without a bed: it needs more houses. */
public record HousingShortageEvent(long minute, java.util.UUID villageId, int homeless) implements VillageEngineEvent { }
