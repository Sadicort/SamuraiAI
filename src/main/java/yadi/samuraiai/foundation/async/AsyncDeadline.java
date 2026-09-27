package yadi.samuraiai.foundation.async;

public interface AsyncDeadline extends AutoCloseable {
    boolean cancel();
    boolean done();
    @Override default void close() { cancel(); }
}
