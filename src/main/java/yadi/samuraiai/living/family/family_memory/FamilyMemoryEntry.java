package yadi.samuraiai.living.family.family_memory;

import java.util.List;
import java.util.UUID;

/**
 * One entry of a family's collective memory — not a personal memory (those belong to the Memory Engine and are never copied to
 * children), but what the family as a whole tells about itself: births, unions, deaths, migrations, wars, betrayals, heroism,
 * trades taken up, homes founded, property lost or gained, masters succeeded.
 */
public record FamilyMemoryEntry(long minute, Kind kind, String text, List<UUID> persons, double significance) {
    public enum Kind { FOUNDING, BIRTH, UNION, DEATH, MISSING, MIGRATION, WAR, BETRAYAL, HEROISM, PROFESSION, HOME, PROPERTY_LOST, ACQUISITION, SUCCESSION, HEIRLOOM, TRADITION, BRANCH, OTHER }

    public FamilyMemoryEntry { persons = persons == null ? List.of() : List.copyOf(persons); text = text == null ? "" : text; }
}
