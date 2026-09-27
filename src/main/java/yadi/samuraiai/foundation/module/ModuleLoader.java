package yadi.samuraiai.foundation.module;

import java.util.*;

/** Deterministic dependency loader. One failed transaction is rolled back in reverse order. */
public final class ModuleLoader implements AutoCloseable {
    private static final AutoCloseable NOOP = () -> { };
    private final ModuleContext context;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final List<String> loadOrder = new ArrayList<>();
    private boolean loading, closed;

    private static final class Entry {
        final ModuleDescriptor descriptor;
        final ModuleInitializer initializer;
        ModuleState state = ModuleState.DISABLED;
        String detail = "Registered";
        AutoCloseable resource;
        Entry(ModuleDescriptor descriptor, ModuleInitializer initializer) {
            this.descriptor = descriptor; this.initializer = initializer;
        }
    }

    public ModuleLoader(ModuleContext context) { this.context = Objects.requireNonNull(context); }

    public synchronized void register(ModuleDescriptor descriptor, ModuleInitializer initializer) {
        ensureOpen();
        Objects.requireNonNull(descriptor); Objects.requireNonNull(initializer);
        if (loading || !loadOrder.isEmpty()) throw new IllegalStateException("Cannot register after loading began");
        if (entries.putIfAbsent(descriptor.id(), new Entry(descriptor, initializer)) != null)
            throw new IllegalArgumentException("Duplicate module: " + descriptor.id());
    }

    public synchronized void loadAll() {
        ensureOpen();
        if (loading) throw new IllegalStateException("Recursive module load");
        if (!loadOrder.isEmpty()) return;
        loading = true;
        List<String> transaction = new ArrayList<>();
        try {
            List<Entry> ordered = entries.values().stream()
                    .sorted(Comparator.comparingInt((Entry e) -> e.descriptor.priority()).reversed()
                            .thenComparing(e -> e.descriptor.id())).toList();
            for (Entry entry : ordered) load(entry, new ArrayDeque<>(), transaction);
        } catch (RuntimeException failure) {
            rollback(transaction);
            throw failure;
        } finally { loading = false; }
    }

    private void load(Entry entry, Deque<String> path, List<String> transaction) {
        if (entry.state == ModuleState.READY || entry.state == ModuleState.DISABLED) {
            if (entry.state == ModuleState.DISABLED && !entry.detail.equals("Registered")) return;
            if (entry.state == ModuleState.READY) return;
        }
        if (!entry.descriptor.side().supports(context.side())) {
            entry.state = ModuleState.DISABLED;
            entry.detail = "Not applicable on " + context.side();
            return;
        }
        if (path.contains(entry.descriptor.id())) {
            String cycle = String.join(" -> ", path) + " -> " + entry.descriptor.id();
            entry.state = ModuleState.FAILED; entry.detail = "Dependency cycle: " + cycle;
            throw new IllegalStateException(entry.detail);
        }
        path.addLast(entry.descriptor.id());
        try {
            for (ModuleDependency dependency : entry.descriptor.dependencies()) {
                Entry required = entries.get(dependency.id());
                if (required == null) {
                    if (dependency.required()) {
                        entry.state = ModuleState.NOT_FOUND;
                        entry.detail = "Missing dependency: " + dependency.id();
                        throw new IllegalStateException(entry.detail);
                    }
                    continue;
                }
                load(required, path, transaction);
                if (dependency.required() && required.state != ModuleState.READY) {
                    entry.state = ModuleState.INCOMPATIBLE;
                    entry.detail = "Dependency not ready: " + dependency.id() + " (" + required.state + ")";
                    throw new IllegalStateException(entry.detail);
                }
            }
            entry.state = ModuleState.LOADING;
            try {
                entry.resource = Objects.requireNonNullElse(entry.initializer.initialize(context), NOOP);
                entry.state = ModuleState.READY; entry.detail = "Ready";
                transaction.add(entry.descriptor.id()); loadOrder.add(entry.descriptor.id());
            } catch (Exception | LinkageError failure) {
                entry.state = ModuleState.FAILED;
                entry.detail = "Initialization failed: " + failure.getClass().getSimpleName();
                throw new IllegalStateException("Module " + entry.descriptor.id() + " failed", failure);
            }
        } finally { path.removeLast(); }
    }

    private void rollback(List<String> transaction) {
        ListIterator<String> iterator = transaction.listIterator(transaction.size());
        while (iterator.hasPrevious()) {
            String id = iterator.previous(); Entry entry = entries.get(id);
            try { if (entry.resource != null) entry.resource.close(); }
            catch (Exception ignored) { entry.detail = "Rollback close failed"; }
            entry.resource = null; entry.state = ModuleState.DISABLED;
            if (!entry.detail.equals("Rollback close failed")) entry.detail = "Rolled back";
            loadOrder.remove(id);
        }
    }

    public synchronized Optional<ModuleSnapshot> find(String id) {
        Entry entry = entries.get(id);
        return entry == null ? Optional.empty() : Optional.of(new ModuleSnapshot(id, entry.state, entry.detail));
    }
    public synchronized List<ModuleSnapshot> snapshot() {
        return entries.values().stream().map(e -> new ModuleSnapshot(e.descriptor.id(), e.state, e.detail)).toList();
    }
    public synchronized List<String> loadOrder() { return List.copyOf(loadOrder); }
    public synchronized boolean ready(String id) { return find(id).map(s -> s.state() == ModuleState.READY).orElse(false); }
    private void ensureOpen() { if (closed) throw new IllegalStateException("Module loader closed"); }
    @Override public synchronized void close() { if (closed) return; rollback(new ArrayList<>(loadOrder)); closed = true; }
}
