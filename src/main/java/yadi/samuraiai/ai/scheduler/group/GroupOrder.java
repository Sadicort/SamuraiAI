package yadi.samuraiai.ai.scheduler.group;

import java.util.UUID;
import yadi.samuraiai.ai.scheduler.engine.Intent;

/** What a leader asks of one follower right now: do the routine the group is doing, at this slot. */
public record GroupOrder(String groupId, UUID leader, Intent intent, GroupRole role, int slot, String formation, long issuedTick) { }
