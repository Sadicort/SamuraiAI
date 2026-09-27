package yadi.samuraiai.npc.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
import yadi.samuraiai.world.SpawnLocation;

/** World-scoped durable snapshot store. Runtime objects are never serialized. */
public final class SamuraiWorldData extends SavedData {
    private static final String ID = "samuraiai_npcs";
    private final Map<UUID, NPCSnapshot> snapshots = new LinkedHashMap<>();
    public static SamuraiWorldData load(CompoundTag tag) {
        SamuraiWorldData data = new SamuraiWorldData();
        ListTag list = tag.getList("npcs", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            CompoundTag n = (CompoundTag) raw;
            try {
                UUID id = n.getUUID("id");
                SpawnLocation location = new SpawnLocation(n.getString("dimension"), n.getDouble("x"), n.getDouble("y"), n.getDouble("z"), n.getFloat("yaw"));
                data.snapshots.put(id, new NPCSnapshot(n.getInt("schema"), id, n.getString("type"), n.getString("name"), n.getString("personality"), location));
            } catch (RuntimeException ignored) { /* corrupt entries are isolated */ }
        }
        return data;
    }
    public static SamuraiWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SamuraiWorldData::load, SamuraiWorldData::new, ID);
    }
    public synchronized void put(NPCSnapshot snapshot) { snapshots.put(snapshot.id(), snapshot); setDirty(); }
    public synchronized void remove(UUID id) { if (snapshots.remove(id) != null) setDirty(); }
    public synchronized List<NPCSnapshot> snapshots() { return List.copyOf(snapshots.values()); }
    public synchronized void clearSnapshots() { if (!snapshots.isEmpty()) { snapshots.clear(); setDirty(); } }
    @Override public synchronized CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (NPCSnapshot s : snapshots.values()) {
            CompoundTag n = new CompoundTag(); n.putUUID("id", s.id()); n.putInt("schema", s.schemaVersion());
            n.putString("type", s.type()); n.putString("name", s.name()); n.putString("personality", s.personality());
            n.putString("dimension", s.location().dimensionKey()); n.putDouble("x", s.location().x()); n.putDouble("y", s.location().y());
            n.putDouble("z", s.location().z()); n.putFloat("yaw", s.location().yaw()); list.add(n);
        }
        tag.put("npcs", list); return tag;
    }
}
