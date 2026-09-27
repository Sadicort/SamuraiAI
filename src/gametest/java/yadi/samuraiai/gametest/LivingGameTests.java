package yadi.samuraiai.gametest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.server.LivingService;
import yadi.samuraiai.living.sim.LivingSettings;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.spawn.NPCSpawnRequest;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.world.SpawnLocation;

/**
 * The living world inside a real server: an NPC that appears in a village becomes a citizen with a bed (which becomes its
 * home), a family and a timetable the Behavior Scheduler weighs; a scheduler zone inside the village becomes one of its
 * buildings and is marked abandoned when the zone goes; the Deiliora calendar moves with the server clock; everything is
 * written to the world's data folder; the dialogue prompt learns the date, the village and the family. The living world is
 * dormant in the GameTest server (so other engines' tests measure those engines alone); this batch wakes it and puts it back.
 */
@GameTestHolder("samuraiai")
@PrefixGameTestTemplate(false)
public final class LivingGameTests {
    private static final int FLOOR = 1;

    private LivingGameTests() { }

    private static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }

    private static LivingSettings saved;

    /** The batch's tests run side by side: the living world is woken once for all of them and put back to sleep after. */
    @BeforeBatch(batch = "living")
    public static void wakeTheLivingWorld(ServerLevel level) {
        saved = LivingSettings.current();
        LivingSettings.apply(LivingSettings.builder().set("zoneImportTicks", 20).build());
        LivingService.getInstance().force(true);
    }

    @AfterBatch(batch = "living")
    public static void putItBackToSleep(ServerLevel level) {
        LivingService.getInstance().force(false);
        if (saved != null) LivingSettings.apply(saved);
    }

    private static LivingWorld wake() {
        return LivingService.getInstance().world().orElseThrow(() -> new GameTestAssertException("the living world was not installed at server start"));
    }

    private static BlockPos patch(GameTestHelper helper, ServerLevel level, int offset) {
        BlockPos origin = helper.absolutePos(new BlockPos(0, FLOOR, 0)).offset(offset, 0, 0);
        TestTerrain.flatten(level, origin, 24, 16);
        return origin;
    }

    private static String dim(ServerLevel level) { return level.dimension().location().toString(); }

    private static Settlement village(LivingWorld w, ServerLevel level, BlockPos origin, String name) {
        return w.foundSettlement(name, SettlementType.VILLAGE, dim(level), origin.getX() + 12, origin.getY() + 1, origin.getZ() + 8, 40, false, true,
                Provenance.of("gametest", name, "prueba", w.calendar().now()));
    }

    /** The GameTest world persists between runs and a village founded where one exists is that village: reuse its house there. */
    private static Building house(LivingWorld w, Village v, ServerLevel level, BlockPos at, String name, double radius) {
        for (Building b : v.buildings().values())
            if (b.kind() == BuildingKind.HOUSE && Math.abs(b.x() - at.getX()) < 0.5 && Math.abs(b.z() - at.getZ()) < 0.5 && Math.abs(b.y() - at.getY()) < 0.5) return b;
        return w.villages().registerBuilding(v.id(), BuildingKind.HOUSE, name, dim(level), at.getX(), at.getY(), at.getZ(), radius, Building.State.BUILT, "");
    }

    private static NPCRuntime spawn(ServerLevel level, BlockPos origin, String type, String name, int dx, int dz) {
        var result = NPCSpawnService.getInstance().spawn(new NPCSpawnRequest(NPCTypeId.of(type), name,
                new SpawnLocation(dim(level), origin.getX() + dx + 0.5D, origin.getY() + 1, origin.getZ() + dz + 0.5D, 0.0F)));
        if (!result.success()) throw new AssertionError("spawn failed: " + result.message());
        return NPCManager.getInstance().find(result.instance().getIdentity().id()).orElseThrow();
    }

    private static void remove(NPCRuntime... npcs) {
        for (NPCRuntime npc : npcs) if (NPCManager.getInstance().find(npc.getId()).isPresent()) NPCSpawnService.getInstance().remove(npc.getId(), "gametest", true);
    }

    @GameTest(template = "empty", batch = "living", timeoutTicks = 300)
    public static void anNpcBecomesACitizenWithABedAFamilyAndATimetable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = patch(helper, level, 31000);
        LivingWorld w = wake();
        Settlement s = village(w, level, origin, "Prueba no Sato");
        Village v = w.villages().village(s.id()).orElseThrow();
        house(w, v, level, origin.offset(6, 1, 6), "casa de prueba", 3);
        NPCRuntime npc = spawn(level, origin, "merchant", "liv_citizen", 10, 8);
        helper.runAfterDelay(30, () -> {
            try {
                var citizen = w.villages().citizen(npc.getId());
                check(citizen.isPresent() && citizen.get().present() && v.id().equals(citizen.get().village()), "the NPC is a citizen of the village it appeared in");
                HomeRecord home = w.villages().home(npc.getId()).orElse(null);
                check(home != null, "it got a bed in the registered house");
                SpawnLocation h = npc.getInstance().getHome();
                check(h != null && Math.abs(h.x() - home.bedX()) < 1e-6 && Math.abs(h.z() - home.bedZ()) < 1e-6, "the bed became the NPC's home: " + h);
                check(w.families().person(npc.getId()).isPresent() && w.families().familyOf(npc.getId()).isPresent(), "it has a person record and a family");
                check(SchedulerService.getInstance().scheduler().biasSources().contains("living"), "the living world is a routine bias source");
                Map<RoutineType, Double> bias = SchedulerService.getInstance().scheduler().externalBias(npc.getId());
                check(!bias.isEmpty(), "and it has an opinion about this citizen's routine now");
                List<String> lines = LivingService.getInstance().promptLines(npc.getId(), null);
                check(lines.stream().anyMatch(l -> l.contains("Prueba no Sato")), "the prompt knows the village: " + lines);
                check(LivingService.getInstance().timeOfDay(npc.getId()).orElse("").contains("Año"), "and the Deiliora date");
                System.out.println("PHASE5_LIVING_CITIZEN_OK");
                helper.succeed();
            } finally { remove(npc); }
        });
    }

    @GameTest(template = "empty", batch = "living", timeoutTicks = 300)
    public static void schedulerZonesBecomeBuildingsAndAreAbandonedWhenRemoved(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = patch(helper, level, 32000);
        LivingWorld w = wake();
        Settlement s = village(w, level, origin, "Zona no Sato");
        var zones = SchedulerService.getInstance().scheduler().zoneRegistry();
        String id = "liv_temple_zone";
        zones.add(new Zone(id, dim(level), ZoneKind.TEMPLE, origin.getX() + 12, origin.getY() + 1, origin.getZ() + 8, 5, 10, EnumSet.allOf(DayPeriod.class), "", List.of()));
        int[] phase = {0};
        helper.succeedWhen(() -> {
            Village v = w.villages().village(s.id()).orElseThrow();
            Building b = v.buildings().values().stream().filter(x -> id.equals(x.zoneId())).findFirst().orElse(null);
            if (phase[0] == 0) {
                if (b == null) throw new GameTestAssertException("zone not imported yet");
                check(b.kind() == BuildingKind.TEMPLE && b.state() == Building.State.BUILT, "the temple zone is a built temple: " + b.kind() + " " + b.state());
                zones.remove(id);
                phase[0] = 1;
                throw new GameTestAssertException("waiting for the removal to be seen");
            }
            check(b != null, "the building is kept for history");
            if (b.state() != Building.State.ABANDONED) throw new GameTestAssertException("not yet abandoned");
            System.out.println("PHASE5_LIVING_ZONES_OK");
        });
    }

    @GameTest(template = "empty", batch = "living", timeoutTicks = 300)
    public static void aHousesRealBedBecomesItsResidentsHomeAndItsCampfireIsKnown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = patch(helper, level, 33000);
        LivingWorld w = wake();
        Settlement s = village(w, level, origin, "Cama no Sato");
        Village v = w.villages().village(s.id()).orElseThrow();
        BlockPos houseAt = origin.offset(6, 1, 6);
        Building house = house(w, v, level, houseAt, "casa con cama", 4);
        BlockPos foot = houseAt.offset(2, 0, 1), head = foot.north();
        level.setBlock(foot, net.minecraft.world.level.block.Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), 3);
        level.setBlock(head, net.minecraft.world.level.block.Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), 3);
        BlockPos fire = houseAt.offset(-2, 0, -2);
        level.setBlock(fire, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 3);
        NPCRuntime npc = spawn(level, origin, "merchant", "liv_sleeper", 12, 10);
        helper.succeedWhen(() -> {
            HomeRecord home = w.villages().home(npc.getId()).orElse(null);
            if (home == null || home.house() == null || !home.house().equals(house.id())) throw new GameTestAssertException("no home in the house yet");
            if (Math.abs(home.bedX() - (head.getX() + 0.5D)) > 1e-6 || Math.abs(home.bedZ() - (head.getZ() + 0.5D)) > 1e-6) throw new GameTestAssertException("the house was not scanned yet");
            SpawnLocation h = npc.getInstance().getHome();
            check(h != null && Math.abs(h.x() - home.bedX()) < 1e-6 && Math.abs(h.z() - home.bedZ()) < 1e-6, "the NPC's home followed the real bed: " + h);
            var points = w.world().interactions().points(v.id());
            check(points.stream().anyMatch(p -> p.kind() == yadi.samuraiai.living.world.environment.InteractionPoint.Kind.FIRE && p.x() == fire.getX() && p.z() == fire.getZ()), "the campfire is an interaction point: " + points);
            check(points.stream().filter(p -> p.kind() == yadi.samuraiai.living.world.environment.InteractionPoint.Kind.BED).count() == 1, "one bed (its head), not two");
            remove(npc);
            System.out.println("PHASE5_LIVING_BED_OK");
        });
    }

    @GameTest(template = "empty", batch = "living", timeoutTicks = 200)
    public static void theLivingCommandsAnswer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        wake();
        var commands = level.getServer().getCommands();
        var source = level.getServer().createCommandSourceStack().withPermission(4);
        helper.runAfterDelay(5, () -> {
            for (String command : List.of("samuraiai living status", "samuraiai living calendar", "samuraiai living calendar festivals", "samuraiai living calendar timeline",
                    "samuraiai living world", "samuraiai living world settlements", "samuraiai living world roads", "samuraiai living world events",
                    "samuraiai living village list", "samuraiai living economy merchants", "samuraiai living economy caravans", "samuraiai living economy routes",
                    "samuraiai living economy contracts", "samuraiai living quest all", "samuraiai living quest campaigns", "samuraiai living family audit", "samuraiai living metrics",
                    "samuraiai living family names ashen 3", "samuraiai living family clan list"))
                check(commands.performPrefixedCommand(source, command) > 0, command);
            check(commands.performPrefixedCommand(source, "samuraiai living calendar advance 1") > 0, "advance a day");
            System.out.println("PHASE5_LIVING_COMMANDS_OK");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "living", timeoutTicks = 300)
    public static void theCalendarFollowsTheServerAndEverythingIsWrittenToTheWorld(GameTestHelper helper) {
        LivingWorld w = wake();
        long before = w.calendar().now();
        helper.runAfterDelay(120, () -> {
            try {
                long after = w.calendar().now();
                check(after >= before, "time never goes back");
                boolean sunMoves = helper.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
                if (sunMoves) check(after > before, "with the daylight cycle on, Deiliora time moved: " + before + " -> " + after);
                int files = w.save(true);
                check(files > 5, "the engines wrote their files: " + files);
                Path root = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve("samuraiai").resolve("living");
                check(Files.exists(root.resolve("calendar").resolve("clock.json")) || Files.exists(root.resolve("calendar").resolve("clock.json.gz")), "the clock is on disk under " + root);
                System.out.println("PHASE5_LIVING_PERSISTENCE_OK");
                helper.succeed();
            } catch (java.io.UncheckedIOException error) {
                throw new GameTestAssertException("save failed: " + error);
            }
        });
    }
}
