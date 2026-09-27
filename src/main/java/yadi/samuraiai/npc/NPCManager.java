package yadi.samuraiai.npc;

import java.util.*;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.world.SpawnLocation;

/** Atomic registry with defensive snapshots and UUID/name/type/state/dimension indices. */
public final class NPCManager {
    private static final NPCManager INSTANCE = new NPCManager();
    public static NPCManager getInstance() { return INSTANCE; }
    private final Map<UUID, NPCRuntime> active = new LinkedHashMap<>();
    private final Map<String, UUID> names = new HashMap<>();
    private final Map<NPCTypeId, Set<UUID>> types = new HashMap<>();
    private final Map<NPCState, Set<UUID>> states = new EnumMap<>(NPCState.class);
    private final Map<String, Set<UUID>> worlds = new HashMap<>();
    private static String key(String name) { return name.trim().toLowerCase(Locale.ROOT); }
    public synchronized void register(NPCRuntime runtime) {
        ServerScheduler.getInstance().requireServerThread();
        Objects.requireNonNull(runtime);
        if (active.containsKey(runtime.getId()) || names.containsKey(key(runtime.getName())))
            throw new IllegalStateException("Duplicate NPC UUID or name: " + runtime.getName());
        active.put(runtime.getId(), runtime); names.put(key(runtime.getName()), runtime.getId()); refresh(runtime);
    }
    public synchronized Optional<NPCRuntime> unregister(UUID id) {
        ServerScheduler.getInstance().requireServerThread();
        NPCRuntime runtime = active.remove(id);
        if (runtime != null) {
            names.remove(key(runtime.getName())); removeIndices(id);
        }
        return Optional.ofNullable(runtime);
    }
    private void removeIndices(UUID id) {
        for (Map<?, Set<UUID>> index : List.of(types, states, worlds)) {
            index.values().forEach(ids -> ids.remove(id));
            index.values().removeIf(Set::isEmpty);
        }
    }
    public synchronized void refresh(NPCRuntime runtime) {
        ServerScheduler.getInstance().requireServerThread();
        if (active.get(runtime.getId()) != runtime) return;
        removeIndices(runtime.getId());
        types.computeIfAbsent(runtime.getInstance().getIdentity().type(), k -> new LinkedHashSet<>()).add(runtime.getId());
        states.computeIfAbsent(runtime.getState(), k -> new LinkedHashSet<>()).add(runtime.getId());
        SpawnLocation location = runtime.getInstance().getLocation();
        worlds.computeIfAbsent(location == null ? "unknown" : location.dimensionKey(), k -> new LinkedHashSet<>()).add(runtime.getId());
    }
    public synchronized Optional<NPCRuntime> find(UUID id) { return Optional.ofNullable(active.get(id)); }
    public synchronized Optional<NPCRuntime> findByName(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(active.get(names.get(key(name))));
    }
    public synchronized String uniqueName(String requested) {
        String base = requested.trim().replaceAll("[^\\p{L}\\p{N}_-]", "_");
        if (base.isBlank()) base = "NPC";
        base = base.substring(0, Math.min(48, base.length()));
        String candidate = base;
        for (int i = 2; names.containsKey(key(candidate)); i++) candidate = base + "_" + i;
        return candidate;
    }
    private List<NPCRuntime> lookup(Set<UUID> ids) { return ids.stream().map(active::get).filter(Objects::nonNull).toList(); }
    public synchronized List<NPCRuntime> byType(NPCTypeId type) { return lookup(types.getOrDefault(type, Set.of())); }
    public synchronized List<NPCRuntime> byState(NPCState state) { return lookup(states.getOrDefault(state, Set.of())); }
    public synchronized List<NPCRuntime> byWorld(String world) { return lookup(worlds.getOrDefault(world, Set.of())); }
    public synchronized Optional<NPCRuntime> findNearest(String dimension, double x, double y, double z, double radius) {
        return byWorld(dimension).stream().filter(NPCRuntime::isActive)
                .filter(r -> r.getInstance().getLocation() != null && r.getInstance().getLocation().distanceSquaredTo(x,y,z) <= radius*radius)
                .min(Comparator.comparingDouble(r -> r.getInstance().getLocation().distanceSquaredTo(x,y,z)));
    }
    public synchronized Collection<NPCRuntime> getActive() { return List.copyOf(active.values()); }
    public synchronized List<NPCRuntime> listSorted() { return active.values().stream().sorted(Comparator.comparing(NPCRuntime::getName)).toList(); }
    public synchronized int count() { return active.size(); }
    public synchronized boolean isEmpty() { return active.isEmpty(); }
    public synchronized void clear() {
        ServerScheduler.getInstance().requireServerThread();
        if (!active.isEmpty()) throw new IllegalStateException("Remove NPCs through NPCSpawnService before clearing");
        names.clear(); types.clear(); states.clear(); worlds.clear();
    }
}
