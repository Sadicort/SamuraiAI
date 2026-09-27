package yadi.samuraiai.npc.lifecycle;

public enum NPCLifecycleState {
    UNINITIALIZED,
    CREATING,
    INITIALIZING,
    LOADING_RUNTIME,
    ACTIVE,
    IDLE,
    PAUSED,
    SLEEPING,
    HIBERNATING,
    INACTIVE,
    UNLOADING,
    REMOVING,
    REMOVED,
    ERROR,
    INVALID
}
