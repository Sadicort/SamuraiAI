package yadi.samuraiai.living.core.persistence;

import com.google.gson.JsonObject;
import java.util.List;
import yadi.samuraiai.ai.cognition.storage.Migration;

/**
 * One persistent part of a living-world engine (the calendar, the villages, the economy ledger...). The engine decides what
 * a snapshot contains; {@link LivingStorage} decides when it is written (only when dirty, batched, at shutdown) and how
 * (versioned envelope, checksum, backup, migrations). {@link #read} receives a payload already migrated to
 * {@link #schemaVersion()}.
 */
public interface StoreSection {
    /** Envelope domain; a file with another domain is refused. */
    String domain();

    /** Relative file path inside the living store, built from fixed names only. */
    String file();

    int schemaVersion();

    default List<Migration> migrations() { return List.of(); }

    boolean dirty();

    JsonObject write();

    void read(JsonObject payload);

    void clean();
}
