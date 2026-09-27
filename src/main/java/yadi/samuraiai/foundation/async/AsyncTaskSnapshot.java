package yadi.samuraiai.foundation.async;

import java.time.Instant;
import java.util.UUID;

public record AsyncTaskSnapshot(UUID id, String module, String owner, AsyncPriority priority,
                                AsyncTaskState state, Instant createdAt, long timeoutMillis,
                                long elapsedMillis, String failure) { }
