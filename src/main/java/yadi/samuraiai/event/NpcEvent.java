package yadi.samuraiai.event;

/**
 * Something that happened to or around an NPC, published on
 * {@link NPCEventBus}. Implementations are records: an event is a fact about
 * the past and must not be mutable after being handed to listeners.
 */
public interface NpcEvent {

    /** Stable, log-friendly name. */
    String getName();

}
