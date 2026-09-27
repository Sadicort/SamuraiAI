package yadi.samuraiai.living.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Which Minecraft items stand for each economy resource, so goods can cross between the abstract economy and a player's
 * inventory: a quest reward in rice becomes wheat in the hand, a delivery of lumber takes planks from the inventory. The table
 * is data ({@code resourceItems} in {@code samuraiai-living.toml}: {@code resource=item|item|...}); the first item of a line
 * is what is given, any of them is accepted. Resources without items (water, silver) cannot cross: nothing is invented to
 * stand for them. One item is one unit.
 */
final class ResourceItems {
    static final List<String> BUILT_IN = List.of(
            "rice=minecraft:wheat",
            "wheat=minecraft:wheat",
            "fish=minecraft:cod|minecraft:salmon|minecraft:cooked_cod|minecraft:cooked_salmon",
            "meat=minecraft:beef|minecraft:porkchop|minecraft:mutton|minecraft:chicken|minecraft:rabbit|minecraft:cooked_beef|minecraft:cooked_porkchop",
            "meal=minecraft:bread|minecraft:baked_potato|minecraft:mushroom_stew",
            "wood=minecraft:oak_log|minecraft:spruce_log|minecraft:birch_log|minecraft:jungle_log|minecraft:acacia_log|minecraft:dark_oak_log|minecraft:mangrove_log",
            "coal=minecraft:coal|minecraft:charcoal",
            "iron=minecraft:iron_ingot",
            "stone=minecraft:cobblestone|minecraft:stone",
            "cloth=minecraft:white_wool|minecraft:string",
            "leather=minecraft:leather",
            "bamboo=minecraft:bamboo",
            "herbs=minecraft:fern|minecraft:dandelion|minecraft:poppy|minecraft:sweet_berries",
            "clay=minecraft:clay_ball",
            "tools=minecraft:iron_pickaxe|minecraft:iron_axe|minecraft:iron_shovel|minecraft:iron_hoe",
            "lumber=minecraft:oak_planks|minecraft:spruce_planks|minecraft:birch_planks|minecraft:jungle_planks|minecraft:acacia_planks|minecraft:dark_oak_planks",
            "gold=minecraft:gold_ingot");

    private final Map<String, List<ResourceLocation>> table = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    ResourceItems(List<String> lines) { load(lines == null || lines.isEmpty() ? BUILT_IN : lines); }

    private void load(List<String> lines) {
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            int eq = line.indexOf('=');
            if (eq <= 0) { if (!line.isEmpty()) problems.add("sin '=': " + line); continue; }
            String resource = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            List<ResourceLocation> items = new ArrayList<>();
            for (String id : line.substring(eq + 1).split("\\|")) {
                ResourceLocation loc = ResourceLocation.tryParse(id.trim().toLowerCase(Locale.ROOT));
                if (loc == null) problems.add(resource + ": id inválido " + id); else items.add(loc);
            }
            if (!items.isEmpty()) table.put(resource, List.copyOf(items));
        }
    }

    List<String> problems() { return List.copyOf(problems); }
    Map<String, List<ResourceLocation>> table() { return Map.copyOf(table); }
    boolean crosses(String resource) { return table.containsKey(resource); }

    private static Optional<Item> item(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == Items.AIR ? Optional.empty() : Optional.of(item);
    }

    /** The resource an item stands for (the first line that lists it), if any. */
    Optional<String> resourceOf(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return Optional.empty();
        for (var e : table.entrySet()) if (e.getValue().contains(id)) return Optional.of(e.getKey());
        return Optional.empty();
    }

    /** Puts whole units into the player's inventory (the rest drops at their feet). Returns units given. */
    int give(ServerPlayer player, String resource, double quantity) {
        List<ResourceLocation> ids = table.get(resource);
        int units = (int) Math.floor(quantity + 1e-9);
        if (ids == null || units <= 0) return 0;
        Optional<Item> item = item(ids.get(0));
        if (item.isEmpty()) return 0;
        int left = units;
        while (left > 0) {
            int n = Math.min(left, item.get().getMaxStackSize());
            ItemStack stack = new ItemStack(item.get(), n);
            if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
            left -= n;
        }
        return units;
    }

    /** How many units of a resource the player carries. */
    int count(ServerPlayer player, String resource) {
        List<ResourceLocation> ids = table.get(resource);
        if (ids == null) return 0;
        int n = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack s = player.getInventory().getItem(slot);
            if (!s.isEmpty() && ids.contains(ForgeRegistries.ITEMS.getKey(s.getItem()))) n += s.getCount();
        }
        return n;
    }

    /** Takes up to {@code units} of a resource from the player's inventory. Returns units taken. */
    int take(ServerPlayer player, String resource, int units) {
        List<ResourceLocation> ids = table.get(resource);
        if (ids == null || units <= 0) return 0;
        int taken = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && taken < units; slot++) {
            ItemStack s = player.getInventory().getItem(slot);
            if (s.isEmpty() || !ids.contains(ForgeRegistries.ITEMS.getKey(s.getItem()))) continue;
            int n = Math.min(units - taken, s.getCount());
            s.shrink(n);
            taken += n;
        }
        if (taken > 0) player.getInventory().setChanged();
        return taken;
    }
}
