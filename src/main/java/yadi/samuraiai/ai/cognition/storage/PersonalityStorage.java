package yadi.samuraiai.ai.cognition.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.personality.PersonalityLedger;

/** Persists one NPC's personality ledger (base and long-term evolution) as {@code npc/<uuid>/personality.json}. */
public final class PersonalityStorage {
    public static final int SCHEMA = 1;
    public static final String DOMAIN = "personality";

    private final VersionedStore store;
    private final List<Migration> migrations;

    public PersonalityStorage(VersionedStore store, List<Migration> migrations) { this.store = store; this.migrations = migrations == null ? List.of() : List.copyOf(migrations); }

    public static String path(UUID npc) { return "npc/" + npc + "/personality.json"; }

    public boolean save(PersonalityLedger ledger) {
        JsonObject o = new JsonObject();
        JsonArray base = new JsonArray(), evolution = new JsonArray();
        for (Trait t : Trait.values()) { base.add(ledger.base(t)); evolution.add(ledger.evolution(t)); }
        o.add("base", base); o.add("evolution", evolution);
        JsonArray names = new JsonArray();
        for (Trait t : Trait.values()) names.add(t.name());
        o.add("traits", names);
        boolean ok = store.write(path(ledger.npcId()), DOMAIN, SCHEMA, o);
        if (ok) ledger.clean();
        return ok;
    }

    public LoadResult load(PersonalityLedger ledger) {
        LoadResult result = store.read(path(ledger.npcId()), DOMAIN, SCHEMA, migrations);
        if (!result.usable()) return result;
        JsonObject o = result.payload();
        // Values are stored in trait order but matched by name, so adding a trait later cannot shift the others.
        List<String> names = Json.stringList(Json.arr(o, "traits"));
        JsonArray base = Json.arr(o, "base"), evolution = Json.arr(o, "evolution");
        double[] b = new double[Trait.values().length], e = new double[Trait.values().length];
        java.util.Arrays.fill(b, 50.0D);
        for (int i = 0; i < names.size() && i < base.size(); i++) {
            Trait t = Trait.parse(names.get(i)).orElse(null);
            if (t == null) continue;
            try { b[t.ordinal()] = base.get(i).getAsDouble(); e[t.ordinal()] = i < evolution.size() ? evolution.get(i).getAsDouble() : 0.0D; } catch (RuntimeException ignored) { /* keep default */ }
        }
        ledger.restore(b, e);
        return result;
    }

    public void delete(UUID npc) { store.delete(path(npc)); }
}
