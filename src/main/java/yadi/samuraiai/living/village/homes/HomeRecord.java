package yadi.samuraiai.living.village.homes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Where a citizen lives: the house, the room, the bed (the point the scheduler's SLEEP and WAKE routines go to), a chest for
 * later, personal objects, and the radius of the private zone others do not wander into. The Family Engine groups homes into
 * households; there is no second house system.
 */
public final class HomeRecord {
    private final UUID citizen;
    private UUID house;
    private String room;
    private double bedX, bedY, bedZ;
    private double chestX = Double.NaN, chestY, chestZ;
    private final List<String> personalObjects = new ArrayList<>();
    private double privateRadius;
    private final long since;

    public HomeRecord(UUID citizen, UUID house, String room, double bedX, double bedY, double bedZ, double privateRadius, long since) {
        this.citizen = citizen; this.house = house; this.room = room == null ? "" : room; this.bedX = bedX; this.bedY = bedY; this.bedZ = bedZ;
        this.privateRadius = Math.max(0.0D, privateRadius); this.since = since;
    }

    public UUID citizen() { return citizen; }
    public UUID house() { return house; }
    public void house(UUID v) { house = v; }
    public String room() { return room; }
    public void room(String v) { room = v == null ? "" : v; }
    public double bedX() { return bedX; }
    public double bedY() { return bedY; }
    public double bedZ() { return bedZ; }
    public void bed(double x, double y, double z) { bedX = x; bedY = y; bedZ = z; }
    public boolean hasChest() { return !Double.isNaN(chestX); }
    public double chestX() { return chestX; }
    public double chestY() { return chestY; }
    public double chestZ() { return chestZ; }
    public void chest(double x, double y, double z) { chestX = x; chestY = y; chestZ = z; }
    public List<String> personalObjects() { return personalObjects; }
    public double privateRadius() { return privateRadius; }
    public long since() { return since; }
}
