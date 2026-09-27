package yadi.samuraiai.event;

/**
 * Where a subsystem publishes its facts. Production wires this to the {@link NPCEventBus}; tests capture events in a
 * list. Shared by navigation, perception and the scheduler so none of them depends on another's event package.
 */
@FunctionalInterface
public interface EventSink {
    void publish(NpcEvent event);

    static EventSink eventBus() { return event -> NPCEventBus.getInstance().post(event); }
}
