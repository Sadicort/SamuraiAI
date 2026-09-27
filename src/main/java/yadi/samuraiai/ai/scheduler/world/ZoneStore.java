package yadi.samuraiai.ai.scheduler.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.ai.scheduler.zone.ZoneRegistry;
import yadi.samuraiai.logging.SamuraiLogger;

/** Saves the world's zones next to the world data ({@code data/samuraiai_zones.json}) so that they survive a restart. */
public final class ZoneStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ZoneStore() { }

    private static Path file(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("samuraiai_zones.json"); }

    public static void save(MinecraftServer server, ZoneRegistry registry) {
        try {
            Path path = file(server);
            Files.createDirectories(path.getParent());
            JsonArray zones = new JsonArray();
            for (Zone zone : registry.all()) zones.add(toJson(zone));
            Files.writeString(path, GSON.toJson(zones), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException error) {
            SamuraiLogger.CONFIG.warn("Could not save the scheduler zones", error);
        }
    }

    /** Replaces the registry's zones with the saved ones; a missing file leaves it empty and a bad entry is skipped. */
    public static int load(MinecraftServer server, ZoneRegistry registry) {
        Path path = file(server);
        registry.clear();
        if (!Files.isRegularFile(path)) return 0;
        int loaded = 0;
        try {
            JsonArray array = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonArray.class);
            if (array == null) return 0;
            for (JsonElement element : array) {
                try { registry.add(fromJson(element.getAsJsonObject())); loaded++; }
                catch (RuntimeException error) { SamuraiLogger.CONFIG.warn("Skipping an unreadable zone: {}", error.toString()); }
            }
        } catch (IOException | RuntimeException error) {
            SamuraiLogger.CONFIG.warn("Could not read the scheduler zones", error);
        }
        return loaded;
    }

    static JsonObject toJson(Zone zone) {
        JsonObject o = new JsonObject();
        o.addProperty("id", zone.id());
        o.addProperty("dimension", zone.dimension());
        o.addProperty("kind", zone.kind().name());
        o.addProperty("x", zone.x()); o.addProperty("y", zone.y()); o.addProperty("z", zone.z());
        o.addProperty("radius", zone.radius());
        o.addProperty("capacity", zone.capacity());
        if (zone.owner() != null) o.addProperty("owner", zone.owner());
        JsonArray periods = new JsonArray();
        if (zone.openPeriods().size() < DayPeriod.values().length) zone.openPeriods().forEach(p -> periods.add(p.name()));
        o.add("periods", periods);
        JsonArray points = new JsonArray();
        for (double[] w : zone.waypoints()) { JsonArray p = new JsonArray(); for (double v : w) p.add(v); points.add(p); }
        o.add("waypoints", points);
        return o;
    }

    static Zone fromJson(JsonObject o) {
        Set<DayPeriod> periods = EnumSet.noneOf(DayPeriod.class);
        if (o.has("periods")) for (JsonElement p : o.getAsJsonArray("periods")) periods.add(DayPeriod.valueOf(p.getAsString().toUpperCase(Locale.ROOT)));
        List<double[]> waypoints = new ArrayList<>();
        if (o.has("waypoints")) for (JsonElement w : o.getAsJsonArray("waypoints")) {
            JsonArray a = w.getAsJsonArray();
            double[] p = new double[a.size()];
            for (int i = 0; i < p.length; i++) p[i] = a.get(i).getAsDouble();
            waypoints.add(p);
        }
        return new Zone(o.get("id").getAsString(), o.get("dimension").getAsString(), ZoneKind.valueOf(o.get("kind").getAsString().toUpperCase(Locale.ROOT)),
                o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(), o.get("radius").getAsDouble(),
                o.has("capacity") ? o.get("capacity").getAsInt() : 4, periods, o.has("owner") ? o.get("owner").getAsString() : null, waypoints);
    }
}
