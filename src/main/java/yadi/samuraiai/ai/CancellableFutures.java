package yadi.samuraiai.ai;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/** CompletionStage combinators alone do not propagate cancellation upstream. */
public final class CancellableFutures {
    public static <T,R> CompletableFuture<R> map(CompletableFuture<T> source, BiFunction<T,Throwable,R> mapper) {
        CompletableFuture<R> target = new CompletableFuture<>();
        target.whenComplete((r,e) -> { if (target.isCancelled()) source.cancel(true); });
        source.whenComplete((r,e) -> {
            if (target.isDone()) return;
            try { target.complete(mapper.apply(r,e)); }
            catch (Throwable error) { target.completeExceptionally(error); }
        });
        return target;
    }
    private CancellableFutures() {}
}
