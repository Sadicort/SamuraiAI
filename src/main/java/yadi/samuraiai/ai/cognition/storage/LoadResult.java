package yadi.samuraiai.ai.cognition.storage;

import com.google.gson.JsonObject;

/** The outcome of reading a persistent file: the payload (already migrated to the current schema) and how it was obtained. */
public record LoadResult(Status status, int storedVersion, JsonObject payload, String detail) {
    /** TOO_NEW: written by a newer schema. UNSUPPORTED: older schema with no migration path. Neither is touched or replaced: the file is left exactly as found. */
    public enum Status { OK, MIGRATED, MISSING, RECOVERED_FROM_BACKUP, CORRUPT, TOO_NEW, UNSUPPORTED }

    public boolean usable() { return payload != null && (status == Status.OK || status == Status.MIGRATED || status == Status.RECOVERED_FROM_BACKUP); }

    static LoadResult missing() { return new LoadResult(Status.MISSING, 0, null, "no file"); }
    static LoadResult failed(Status status, String detail) { return new LoadResult(status, 0, null, detail); }
}
