package yadi.samuraiai.living.world.environment;

import java.util.UUID;

/**
 * A place in the physical world where people interact with their surroundings: a bed, a door, a seat, a fireplace or
 * campfire, a crop, a workstation (anvil, furnace, crafting table), a well, a shrine. Found by the adapter when it scans a
 * building's volume; the living world only keeps where they are and which building they belong to.
 */
public record InteractionPoint(Kind kind, String dimension, int x, int y, int z, UUID building, UUID settlement) {
    public enum Kind { BED, DOOR, SEAT, FIRE, CROP, WORKSTATION, WELL, SHRINE, STORAGE }

    public String key() { return dimension + "@" + x + "," + y + "," + z; }
}
