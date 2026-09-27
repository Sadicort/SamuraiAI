package yadi.samuraiai.event;

public interface EventSubscription extends AutoCloseable {
    boolean active();
    @Override void close();
}
