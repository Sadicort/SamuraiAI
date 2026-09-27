package yadi.samuraiai.npc;

import yadi.samuraiai.npc.relationship.Relationship;
import yadi.samuraiai.world.SpawnLocation;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Who" a specific NPC is: identity plus everything that should survive a
 * server restart or a chunk unload (relationships, reputation, last known
 * position). Does NOT hold the Brain or any other runtime-only object —
 * that lives on {@link NPCRuntime}, created only while the NPC is active.
 */
public class NPCInstance {

    private final NPCIdentity identity;


    private final Instant createdAt;

    private volatile Instant lastSeenAt;

    /**
     * Where the NPC is. Previously the spawn location was passed to the
     * controller and then discarded, so nothing downstream — perception,
     * chat range, {@code /samuraiai list} — could tell where an NPC was.
     */
    private volatile SpawnLocation location;

    /**
     * Where the NPC belongs: fixed at spawn (or restore). Unlike {@link #location} it does not move as the NPC
     * walks, so a patrol keeps circling the same place instead of drifting away one square at a time.
     */
    private volatile SpawnLocation home;
    private volatile boolean homeAssigned;

    public NPCInstance(NPCIdentity identity) {
        this(identity, null);
    }

    public NPCInstance(NPCIdentity identity, SpawnLocation location) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.location = location;
        this.home = location;
        this.homeAssigned = false;
        this.createdAt = Instant.now();
        this.lastSeenAt = this.createdAt;
    }

    public NPCIdentity getIdentity() {
        return identity;
    }

    /** Creates the bond on first contact, so callers never handle null. */
    public Relationship getRelationship(UUID targetId) {
        return yadi.samuraiai.npc.relationship.RelationshipService.getInstance()
                .find(identity.id(), targetId).orElseGet(Relationship::new);
    }

    /** True only if these two already have a history. */
    public boolean hasRelationship(UUID targetId) {
        return yadi.samuraiai.npc.relationship.RelationshipService.getInstance().find(identity.id(), targetId).isPresent();
    }

    /**
     * Read-only view. The previous version handed out the live map, letting
     * any caller replace or clear another NPC's entire social history.
     */
    public Map<UUID, Relationship> getRelationships() {
        return yadi.samuraiai.npc.relationship.RelationshipService.getInstance().snapshot(identity.id());
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void markSeenNow() {
        this.lastSeenAt = Instant.now();
    }

    public SpawnLocation getLocation() {
        return location;
    }

    /** The stable anchor for routines such as patrol; falls back to the current location when never set. */
    public SpawnLocation getHome() {
        SpawnLocation anchor = home;
        return anchor != null ? anchor : location;
    }

    /**
     * Whether someone gave this NPC a home (a restored snapshot, the living world's bed, a command), as opposed to the default
     * home every NPC starts with: the place it appeared. Pinning the current home again does not count as giving one.
     */
    public boolean homeAssigned() {
        return homeAssigned;
    }

    public void setHome(SpawnLocation value) {
        if (!Objects.equals(value, this.home)) this.homeAssigned = value != null;
        this.home = value;
    }

    public void setLocation(SpawnLocation location) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        this.location = location;
        NPCManager.getInstance().find(identity.id()).ifPresent(NPCManager.getInstance()::refresh);
    }

    @Override
    public String toString() {
        return identity.name() + " (" + identity.type().value() + ")";
    }
}
