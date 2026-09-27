package yadi.samuraiai.npc.persistence;

import com.google.gson.Gson;
import java.util.*;
import java.util.function.Consumer;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.runtime.ServerScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Lifecycle hooks only. No implicit disk writes or retention across worlds. */
public final class PersistenceBootstrap {
    private static final PersistenceBootstrap INSTANCE = new PersistenceBootstrap();
    private static final Gson GSON = new Gson();
    private final List<Consumer<NPCSnapshot>> activation = new ArrayList<>(), deactivation = new ArrayList<>();
    public static PersistenceBootstrap getInstance() { return INSTANCE; }
    public String serialize(NPCSnapshot snapshot) { return GSON.toJson(snapshot); }
    public NPCSnapshot deserialize(String json) {
        // Minecraft 1.19.2 supplies Gson 2.8.9, which cannot instantiate Java records.
        var object = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        var position = object.getAsJsonObject("location");
        var location = new yadi.samuraiai.world.SpawnLocation(position.get("dimensionKey").getAsString(),
                position.get("x").getAsDouble(), position.get("y").getAsDouble(), position.get("z").getAsDouble(),
                position.get("yaw").getAsFloat());
        return new NPCSnapshot(object.get("schemaVersion").getAsInt(), UUID.fromString(object.get("id").getAsString()),
                object.get("type").getAsString(), object.get("name").getAsString(),
                object.has("personality") ? object.get("personality").getAsString() : "", location);
    }
    public void onActivate(Consumer<NPCSnapshot> hook) { activation.add(Objects.requireNonNull(hook)); }
    public void onDeactivate(Consumer<NPCSnapshot> hook) { deactivation.add(Objects.requireNonNull(hook)); }
    public void activated(NPCRuntime npc) { notifyHooks(activation, npc); }
    public void deactivated(NPCRuntime npc) { notifyHooks(deactivation, npc); }
    /** Stores snapshots in the overworld SavedData; all calls are server-thread bound. */
    public void saveWorld(MinecraftServer server) {
        ServerScheduler.getInstance().requireServerThread();
        if (server == null) return;
        SamuraiWorldData data = SamuraiWorldData.get(server.overworld());
        for (var runtime : yadi.samuraiai.npc.NPCManager.getInstance().getActive()) data.put(NPCSnapshot.capture(runtime));
    }
    public List<NPCSnapshot> loadWorld(MinecraftServer server) {
        ServerScheduler.getInstance().requireServerThread();
        return server == null ? List.of() : SamuraiWorldData.get(server.overworld()).snapshots();
    }
    public void forget(UUID id, MinecraftServer server) {
        ServerScheduler.getInstance().requireServerThread();
        if (id != null && server != null) SamuraiWorldData.get(server.overworld()).remove(id);
    }
    private void notifyHooks(List<Consumer<NPCSnapshot>> hooks, NPCRuntime npc) {
        ServerScheduler.getInstance().requireServerThread();
        for (var hook : List.copyOf(hooks)) {
            try { hook.accept(NPCSnapshot.capture(npc)); }
            catch (RuntimeException error) { yadi.samuraiai.logging.SamuraiLogger.PERSISTENCE.error("Persistence hook failed npc={}", npc.getId(), error); }
        }
    }
    public void clear() { activation.clear(); deactivation.clear(); }
}
