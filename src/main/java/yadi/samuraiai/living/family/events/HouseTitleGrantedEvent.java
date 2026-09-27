package yadi.samuraiai.living.family.events;

/** A family became historically important enough to be known as a house ("Casa Ashborne"). */
public record HouseTitleGrantedEvent(long minute, java.util.UUID familyId, String houseTitle, double historicalImportance) implements FamilyEngineEvent { }
