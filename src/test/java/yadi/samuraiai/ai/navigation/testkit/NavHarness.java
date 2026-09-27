package yadi.samuraiai.ai.navigation.testkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import yadi.samuraiai.ai.navigation.cache.PathCache;
import yadi.samuraiai.ai.navigation.engine.*;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavigationGraph;
import yadi.samuraiai.ai.navigation.metrics.NavigationMetrics;
import yadi.samuraiai.ai.navigation.obstacles.EntityObstacleInfo;
import yadi.samuraiai.ai.navigation.obstacles.EntityObstacleSource;
import yadi.samuraiai.ai.navigation.planner.PathPreferences;
import yadi.samuraiai.ai.navigation.planner.PathRequest;
import yadi.samuraiai.ai.navigation.terrain.TerrainCostTable;
import yadi.samuraiai.ai.navigation.zones.DangerMap;
import yadi.samuraiai.event.NpcEvent;

/** Wires a full navigation stack over a grid world with simulated bodies and a captured event log. */
public final class NavHarness {
    public final GridWorldView world = GridWorldView.flat();
    public final DangerMap dangers = new DangerMap();
    public final NavigationMetrics metrics = new NavigationMetrics();
    public final List<NpcEvent> events = new ArrayList<>();
    public final Map<UUID, SimulatedBody> bodies = new HashMap<>();
    public final List<EntityObstacleInfo> entities = new ArrayList<>();
    public NavigationSettings settings;
    public PathCache cache;
    public NavigationGraph graph;
    public NavigationEngine engine;
    public NavigationRuntime runtime;
    public long tick;
    public double playerDistance;
    /** When true every simulated body also appears as an NPC entity in entity scans, like in a real world. */
    public boolean bodiesAreObstacles;

    public NavHarness() { this(s -> s); }

    public NavHarness(UnaryOperator<NavigationSettings.Builder> tuning) {
        settings = tuning.apply(NavigationSettings.builder()).build();
        rebuild();
    }

    /** Rebuilds engine and runtime (fresh caches) after the world has been edited before any walking. */
    public void rebuild() {
        graph = new NavigationGraph(world, settings.dangerCap(), settings.nodeCacheMax(), null);
        cache = new PathCache(settings.cacheCapacity(), settings.cacheTtlTicks());
        EntityObstacleSource source = (dimension, x, y, z, radius, exclude) -> {
            List<EntityObstacleInfo> all = new ArrayList<>(entities);
            if (bodiesAreObstacles)
                bodies.values().forEach(b -> all.add(new EntityObstacleInfo(b.entityId(), yadi.samuraiai.ai.navigation.obstacles.EntityKind.NPC, b.x(), b.y(), b.z(), 0.6D)));
            return all.stream().filter(e -> !e.id().equals(exclude))
                    .filter(e -> Math.hypot(e.x() - x, e.z() - z) <= radius && Math.abs(e.y() - y) < 2.0D).toList();
        };
        engine = new NavigationEngine(graph, dangers, cache, TerrainCostTable.defaults(), () -> settings, metrics, events::add, source);
        runtime = new NavigationRuntime(engine, bodies::get, id -> playerDistance, () -> settings);
    }

    public int standY() { return world.standY(); }
    public NavPos at(int x, int z) { return new NavPos(x, standY(), z); }

    public UUID spawn(double x, double z) { return spawn(x, standY(), z); }
    public UUID spawn(double x, double y, double z) {
        UUID id = UUID.randomUUID();
        bodies.put(id, new SimulatedBody(world, x + 0.5D, y, z + 0.5D));
        return id;
    }

    public PathRequest request(UUID npc, NavPos goal) { return request(npc, goal, PathPreferences.defaults()); }
    public PathRequest request(UUID npc, NavPos goal, PathPreferences prefs) {
        SimulatedBody body = bodies.get(npc);
        return new PathRequest(npc, world.dimension(), body.block(), goal, prefs, "test", "test", 1.0D, 4000, false);
    }

    /** A request for an NPC that has no registered body (chat-only NPC). */
    public NavigationSession goBodiless(UUID npc, NavPos goal) {
        return runtime.request(new PathRequest(npc, world.dimension(), at(0, 0), goal, PathPreferences.defaults(), "test", "test", 1.0D, 4000, false), tick);
    }

    public NavigationSession go(UUID npc, NavPos goal) { return runtime.request(request(npc, goal), tick); }

    /** One game tick: physics first, then the navigation runtime. */
    public void tick() {
        tick++;
        world.advance(1);
        bodies.values().forEach(SimulatedBody::physicsTick);
        runtime.tick(tick);
    }

    public void run(int ticks) { for (int i = 0; i < ticks; i++) tick(); }

    /** Runs until the session ends or the tick limit passes. Returns ticks used. */
    public int runUntilDone(NavigationHandle session, int limit) {
        int used = 0;
        while (!session.done() && used < limit) { tick(); used++; }
        return used;
    }

    public <T extends NpcEvent> List<T> events(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }
    public boolean saw(Class<? extends NpcEvent> type) { return events.stream().anyMatch(type::isInstance); }
}
