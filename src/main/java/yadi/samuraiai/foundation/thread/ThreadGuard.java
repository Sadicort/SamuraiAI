package yadi.samuraiai.foundation.thread;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import yadi.samuraiai.runtime.ServerScheduler;

public final class ThreadGuard {
    private static volatile BooleanSupplier clientThread = () -> false;
    public static void bindClient(BooleanSupplier check) { clientThread = Objects.requireNonNull(check); }
    public static void clearClient() { clientThread = () -> false; }
    public static void assertServerThread() { ServerScheduler.getInstance().requireServerThread(); }
    public static void assertClientThread() {
        if (!clientThread.getAsBoolean()) throw new IllegalStateException("Operation requires the client thread");
    }
    public static void assertWorkerThread() {
        if (ServerScheduler.getInstance().isServerThread() || clientThread.getAsBoolean())
            throw new IllegalStateException("Blocking operation is forbidden on a Minecraft owner thread");
    }
    private ThreadGuard() { }
}
