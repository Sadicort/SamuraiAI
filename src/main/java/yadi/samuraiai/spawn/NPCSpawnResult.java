package yadi.samuraiai.spawn;

import yadi.samuraiai.npc.NPCInstance;

public record NPCSpawnResult(boolean success, NPCInstance instance, String message) {

    public static NPCSpawnResult ok(NPCInstance instance, String message) {
        return new NPCSpawnResult(true, instance, message);
    }

    public static NPCSpawnResult failure(String message) {
        return new NPCSpawnResult(false, null, message);
    }
}
