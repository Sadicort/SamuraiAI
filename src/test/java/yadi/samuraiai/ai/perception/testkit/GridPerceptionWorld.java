package yadi.samuraiai.ai.perception.testkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.perception.engine.BlockInterest;
import yadi.samuraiai.ai.perception.engine.PerceptionWorld;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.environment.EnvironmentSnapshot;
import yadi.samuraiai.ai.perception.environment.WeatherState;

/** Sparse in-memory world for perception tests: opacity, light and interesting blocks by coordinate, entities in a list. */
public final class GridPerceptionWorld implements PerceptionWorld {
    private final Map<NavPos, Double> opacity = new HashMap<>();
    private final Map<NavPos, Integer> light = new HashMap<>();
    private final Map<NavPos, BlockInterest> interest = new HashMap<>();
    public final List<SensedEntity> entities = new ArrayList<>();
    public int defaultLight = 15;
    public EnvironmentSnapshot environment = new EnvironmentSnapshot("test:world", "plains", 64, 0.8D, WeatherState.CLEAR, 6000L, 15, false, false, true);
    public long tick;

    public GridPerceptionWorld wall(int x1, int y1, int z1, int x2, int y2, int z2, double value) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) opacity.put(new NavPos(x, y, z), value);
        return this;
    }
    public GridPerceptionWorld solidWall(int x1, int y1, int z1, int x2, int y2, int z2) { return wall(x1, y1, z1, x2, y2, z2, 1.0D); }
    public GridPerceptionWorld light(int x, int y, int z, int level) { light.put(new NavPos(x, y, z), level); return this; }
    public GridPerceptionWorld interest(int x, int y, int z, BlockInterest kind) { if (kind == null) interest.remove(new NavPos(x, y, z)); else interest.put(new NavPos(x, y, z), kind); return this; }
    public SensedEntity add(SensedEntity entity) { entities.add(entity); return entity; }
    public void remove(UUID id) { entities.removeIf(e -> e.id().equals(id)); }
    public void replace(SensedEntity entity) { remove(entity.id()); entities.add(entity); }

    @Override public String dimension() { return "test:world"; }
    @Override public long gameTick() { return tick; }
    @Override public List<SensedEntity> entitiesNear(double x, double y, double z, double radius, UUID exclude) {
        return entities.stream().filter(e -> !e.id().equals(exclude) && e.distanceTo(x, y, z) <= radius).toList();
    }
    @Override public double opacity(int x, int y, int z) { return opacity.getOrDefault(new NavPos(x, y, z), 0.0D); }
    @Override public int lightLevel(int x, int y, int z) { return light.getOrDefault(new NavPos(x, y, z), defaultLight); }
    @Override public BlockInterest interest(int x, int y, int z) { return interest.get(new NavPos(x, y, z)); }
    @Override public boolean isLoaded(int chunkX, int chunkZ) { return true; }
    @Override public EnvironmentSnapshot environment(double x, double y, double z) { return environment; }
}
