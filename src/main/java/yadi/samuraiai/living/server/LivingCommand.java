package yadi.samuraiai.living.server;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.living.calendar.debug.CalendarInspector;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.economy.debug.EconomyInspector;
import yadi.samuraiai.living.family.debug.FamilyInspector;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.quest.branching.BranchSpec;
import yadi.samuraiai.living.quest.branching.Path;
import yadi.samuraiai.living.quest.debug.QuestInspector;
import yadi.samuraiai.living.quest.engine.QuestEngine;
import yadi.samuraiai.living.quest.runtime.Quest;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.debug.VillageInspector;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.debug.WorldInspector;
import yadi.samuraiai.living.world.events.WorldEventType;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;

/**
 * {@code /samuraiai living ...}: the living world from inside the game. Players read the calendar and their village, trade
 * with its market, see their coins, and take on, steer, deliver and abandon quests; operators inspect every engine, move time
 * forward, force weather, found villages, register buildings, start world events, register births, save and toggle the
 * particle overlay. Everything goes through the engines' public operations: a command never writes state by hand.
 */
public final class LivingCommand {
    private LivingCommand() { }

    private static LivingService service() { return LivingService.getInstance(); }

    private static Optional<LivingWorld> world(CommandContext<CommandSourceStack> ctx) {
        if (service().enabled()) return service().world();
        reply(ctx, false, "El mundo vivo está desactivado o el servidor aún no lo ha iniciado.");
        return Optional.empty();
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, boolean ok, String message) {
        if (ok) ctx.getSource().sendSuccess(Component.literal(message), false); else ctx.getSource().sendFailure(Component.literal(message));
        return ok ? 1 : 0;
    }

    private static int lines(CommandContext<CommandSourceStack> ctx, List<String> lines) {
        if (lines.isEmpty()) return reply(ctx, true, "(sin datos)");
        int shown = 0;
        for (String line : lines) {
            if (shown++ >= 60) { ctx.getSource().sendSuccess(Component.literal("... (" + (lines.size() - 60) + " líneas más)"), false); break; }
            ctx.getSource().sendSuccess(Component.literal(line), false);
        }
        return lines.size();
    }

    private static Optional<ServerPlayer> player(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer p) return Optional.of(p);
        reply(ctx, false, "Este comando solo puede ejecutarlo un jugador.");
        return Optional.empty();
    }

    private static String dimension(ServerPlayer p) { return p.level.dimension().location().toString(); }

    /** The village the player stands in, or the nearest one within the join radius. */
    private static Optional<Village> villageHere(LivingWorld w, ServerPlayer p) {
        return w.villages().villageAt(dimension(p), p.getX(), p.getZ()).or(() -> w.villages().nearest(dimension(p), p.getX(), p.getZ(), 48));
    }

    private static List<String> villageNames() {
        return service().world().map(w -> w.villages().villages().stream().map(v -> v.name().replace(' ', '_')).toList()).orElse(List.of());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> villageArg() {
        return Commands.argument("village", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(villageNames(), b));
    }

    private static Optional<Village> village(LivingWorld w, CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "village").replace('_', ' ');
        Optional<Village> v = w.villages().find(name);
        if (v.isEmpty()) reply(ctx, false, "No conozco la aldea " + name + ".");
        return v;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> npcArg() {
        return Commands.argument("npc", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(NPCManager.getInstance().listSorted().stream().map(NPCRuntime::getName).toList(), b));
    }

    private static Optional<UUID> npc(CommandContext<CommandSourceStack> ctx, String arg) {
        String name = StringArgumentType.getString(ctx, arg);
        Optional<UUID> id = NPCManager.getInstance().findByName(name).map(NPCRuntime::getId);
        if (id.isEmpty()) id = service().world().flatMap(w -> w.families().people().stream().filter(p -> p.name().full().equalsIgnoreCase(name.replace('_', ' ')) || p.id().toString().startsWith(name)).map(Person::id).findFirst());
        if (id.isEmpty()) reply(ctx, false, "No conozco a " + name + ".");
        return id;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> resourceArg() {
        return Commands.argument("resource", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(service().items().table().keySet(), b));
    }

    private static <E extends Enum<E>> RequiredArgumentBuilder<CommandSourceStack, String> enumArg(String name, Class<E> type) {
        return Commands.argument(name, StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(type.getEnumConstants()).map(e -> e.name().toLowerCase(Locale.ROOT)).toList(), b));
    }

    private static <E extends Enum<E>> Optional<E> parse(Class<E> type, String text) {
        try { return Optional.of(Enum.valueOf(type, text.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> questArg() {
        return Commands.argument("quest", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                service().world().map(w -> w.quests().open().stream().map(q -> q.id().toString().substring(0, 8)).toList()).orElse(List.of()), b));
    }

    private static Optional<Quest> quest(LivingWorld w, CommandContext<CommandSourceStack> ctx) {
        Optional<Quest> q = w.quests().find(StringArgumentType.getString(ctx, "quest"));
        if (q.isEmpty()) reply(ctx, false, "No hay ninguna misión con ese identificador.");
        return q;
    }

    private static String playerName(ServerPlayer p) { return p.getGameProfile().getName(); }

    // ------------------------------------------------------------------ the tree

    public static LiteralArgumentBuilder<CommandSourceStack> node(int admin) {
        return Commands.literal("living")
                .then(Commands.literal("status").executes(LivingCommand::status))
                .then(Commands.literal("hud").executes(ctx -> player(ctx).map(p -> reply(ctx, true, service().toggleHud(p.getUUID()) ? "Calendario en pantalla activado." : "Calendario en pantalla desactivado.")).orElse(0)))
                .then(calendar(admin))
                .then(worldNode(admin))
                .then(villageNode(admin))
                .then(economyNode(admin))
                .then(trade())
                .then(contracts())
                .then(Commands.literal("coins").executes(LivingCommand::coins))
                .then(questNode(admin))
                .then(familyNode(admin))
                .then(Commands.literal("save").requires(s -> s.hasPermission(admin)).executes(ctx -> world(ctx).map(w -> reply(ctx, true, "Guardados " + w.save(true) + " archivos del mundo vivo.")).orElse(0)))
                .then(Commands.literal("debug").requires(s -> s.hasPermission(admin)).executes(ctx -> player(ctx).map(p -> reply(ctx, true, service().toggleDebug(p.getUUID()) ? "Vista de depuración del mundo vivo activada." : "Vista de depuración desactivada.")).orElse(0)))
                .then(Commands.literal("metrics").requires(s -> s.hasPermission(admin)).executes(LivingCommand::metrics));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        if (w.isEmpty()) return 0;
        List<String> out = new ArrayList<>();
        String cell = ctx.getSource().getEntity() instanceof ServerPlayer p ? service().cellAt(dimension(p), p.getX(), p.getZ()) : CalendarEngine.WORLD_CELL;
        out.addAll(CalendarInspector.overview(w.get().calendar(), cell));
        out.add(String.format("Mundo: %d regiones, %d asentamientos, %d aldeas, %d personas, %d misiones abiertas", w.get().world().regionCount(), w.get().world().settlements().size(),
                w.get().villages().villages().size(), w.get().families().people().size(), w.get().quests().open().size()));
        if (!service().lastError().isEmpty()) out.add("Último error: " + service().lastError());
        return lines(ctx, out);
    }

    private static int metrics(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        if (w.isEmpty()) return 0;
        List<String> out = new ArrayList<>();
        var m = w.get().metrics();
        out.add(String.format("Hub: %d ticks, %d días, %d eventos, %d reacciones fallidas, %d descartadas, %d problemas de genealogía, último error: %s",
                w.get().tickCount(), m.days.get(), m.events.get(), m.reactionFailures.get(), m.droppedReactions.get(), m.auditProblems, m.lastError == null ? "-" : m.lastError));
        out.add(service().interactionStats());
        out.addAll(CalendarInspector.metrics(w.get().calendar()));
        out.addAll(WorldInspector.metrics(w.get().world()));
        out.addAll(VillageInspector.metrics(w.get().villages()));
        out.addAll(EconomyInspector.metrics(w.get().economy()));
        out.addAll(QuestInspector.metrics(w.get().quests()));
        out.addAll(FamilyInspector.metrics(w.get().families()));
        return lines(ctx, out);
    }

    // ------------------------------------------------------------------ calendar

    private static LiteralArgumentBuilder<CommandSourceStack> calendar(int admin) {
        return Commands.literal("calendar")
                .executes(ctx -> world(ctx).map(w -> lines(ctx, CalendarInspector.overview(w.calendar(), cellOf(ctx)))).orElse(0))
                .then(Commands.literal("weather").executes(ctx -> world(ctx).map(w -> lines(ctx, CalendarInspector.weather(w.calendar(), cellOf(ctx)))).orElse(0))
                        .then(enumArg("kind", WeatherKind.class).requires(s -> s.hasPermission(admin)).then(Commands.argument("hours", IntegerArgumentType.integer(1, 240)).executes(LivingCommand::forceWeather))))
                .then(Commands.literal("festivals").executes(ctx -> world(ctx).map(w -> lines(ctx, CalendarInspector.festivals(w.calendar()))).orElse(0)))
                .then(Commands.literal("agriculture").executes(ctx -> world(ctx).map(w -> lines(ctx, CalendarInspector.agriculture(w.calendar(), cellOf(ctx)))).orElse(0)))
                .then(Commands.literal("timeline").executes(ctx -> world(ctx).map(w -> lines(ctx, CalendarInspector.timeline(w.calendar(), "", 30))).orElse(0)))
                .then(Commands.literal("advance").requires(s -> s.hasPermission(admin)).then(Commands.argument("days", IntegerArgumentType.integer(1, 365)).executes(ctx -> world(ctx).map(w -> {
                    long moved = w.calendar().advanceMinutes((long) IntegerArgumentType.getInteger(ctx, "days") * w.calendar().minutesPerDay(), "command");
                    return reply(ctx, moved > 0, "El tiempo avanza " + moved / w.calendar().minutesPerDay() + " días. Hoy es " + w.calendar().today().describe() + ".");
                }).orElse(0))));
    }

    private static String cellOf(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getEntity() instanceof ServerPlayer p ? service().cellAt(dimension(p), p.getX(), p.getZ()) : CalendarEngine.WORLD_CELL;
    }

    private static int forceWeather(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        if (w.isEmpty()) return 0;
        Optional<WeatherKind> kind = parse(WeatherKind.class, StringArgumentType.getString(ctx, "kind"));
        if (kind.isEmpty()) return reply(ctx, false, "Tiempo desconocido.");
        String cell = cellOf(ctx);
        w.get().calendar().forceWeather(cell, kind.get(), 0.8D, IntegerArgumentType.getInteger(ctx, "hours") * 60L);
        return reply(ctx, true, "Tiempo en " + cell + ": " + kind.get().label() + ".");
    }

    // ------------------------------------------------------------------ world

    private static LiteralArgumentBuilder<CommandSourceStack> worldNode(int admin) {
        return Commands.literal("world").requires(s -> s.hasPermission(admin))
                .executes(ctx -> world(ctx).map(w -> lines(ctx, WorldInspector.overview(w.world()))).orElse(0))
                .then(Commands.literal("region").executes(ctx -> world(ctx).flatMap(w -> player(ctx).map(p -> {
                    Region r = w.world().ensureRegion(dimension(p), p.getX(), p.getZ());
                    return lines(ctx, WorldInspector.region(w.world(), r));
                })).orElse(0)))
                .then(Commands.literal("settlements").executes(ctx -> world(ctx).map(w -> lines(ctx, WorldInspector.settlements(w.world()))).orElse(0)))
                .then(Commands.literal("roads").executes(ctx -> world(ctx).map(w -> lines(ctx, WorldInspector.roads(w.world()))).orElse(0)))
                .then(Commands.literal("events").executes(ctx -> world(ctx).map(w -> lines(ctx, WorldInspector.events(w.world(), false))).orElse(0))
                        .then(Commands.literal("archive").executes(ctx -> world(ctx).map(w -> lines(ctx, WorldInspector.events(w.world(), true))).orElse(0))))
                .then(Commands.literal("event").then(enumArg("type", WorldEventType.class).then(Commands.argument("severity", DoubleArgumentType.doubleArg(0.05D, 1.0D)).executes(LivingCommand::startEvent))))
                .then(Commands.literal("resolve").then(Commands.argument("event", StringArgumentType.word()).then(Commands.argument("success", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("si", "no"), b)).executes(ctx -> world(ctx).map(w -> {
                            var e = WorldInspector.event(w.world(), StringArgumentType.getString(ctx, "event"));
                            if (e.isEmpty()) return reply(ctx, false, "No hay ningún evento con ese identificador.");
                            boolean ok = w.world().resolveEvent(e.get().id(), StringArgumentType.getString(ctx, "success").startsWith("s"), ctx.getSource().getTextName(), "resuelto por comando");
                            return reply(ctx, ok, ok ? "Evento resuelto." : "El evento ya había terminado.");
                        }).orElse(0)))));
    }

    private static int startEvent(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        Optional<WorldEventType> type = parse(WorldEventType.class, StringArgumentType.getString(ctx, "type"));
        if (type.isEmpty()) return reply(ctx, false, "Tipo de evento desconocido.");
        Region region = w.get().world().ensureRegion(dimension(p.get()), p.get().getX(), p.get().getZ());
        UUID settlement = w.get().world().nearestSettlement(dimension(p.get()), p.get().getX(), p.get().getZ(), 256).map(Settlement::id).orElse(null);
        String title = type.get().name().toLowerCase(Locale.ROOT) + " en " + region.name();
        var event = w.get().world().scheduleEvent(type.get(), title, region.id(), settlement, DoubleArgumentType.getDouble(ctx, "severity"),
                Provenance.of("command", p.get().getUUID().toString(), playerName(p.get()), w.get().calendar().now()));
        return reply(ctx, event.isPresent(), event.map(e -> "Evento programado: " + e.title() + " (" + e.id().toString().substring(0, 8) + ").").orElse("No se pudo programar el evento (ya hay uno igual o está desactivado)."));
    }

    // ------------------------------------------------------------------ villages

    private static LiteralArgumentBuilder<CommandSourceStack> villageNode(int admin) {
        return Commands.literal("village")
                .executes(ctx -> world(ctx).flatMap(w -> player(ctx).map(p -> villageHere(w, p).map(v -> lines(ctx, VillageInspector.overview(w.villages(), v)))
                        .orElseGet(() -> reply(ctx, false, "No estás en ninguna aldea.")))).orElse(0))
                .then(Commands.literal("list").executes(ctx -> world(ctx).map(w -> lines(ctx, w.villages().villages().stream()
                        .map(v -> String.format("%s — %d habitantes, %s, renombre %.0f", v.name(), w.villages().census(v.id()).residents(), v.security().state().name().toLowerCase(Locale.ROOT), v.renown())).toList())).orElse(0)))
                .then(Commands.literal("info").then(villageArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx).map(v -> lines(ctx, VillageInspector.overview(w.villages(), v)))).orElse(0))))
                .then(Commands.literal("citizens").requires(s -> s.hasPermission(admin)).then(villageArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx).map(v -> lines(ctx, VillageInspector.citizens(w.villages(), v)))).orElse(0))))
                .then(Commands.literal("buildings").requires(s -> s.hasPermission(admin)).then(villageArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx).map(v -> lines(ctx, VillageInspector.buildings(v)))).orElse(0))))
                .then(Commands.literal("visitors").requires(s -> s.hasPermission(admin)).then(villageArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx).map(v -> lines(ctx, VillageInspector.visitors(v)))).orElse(0))))
                .then(Commands.literal("citizen").requires(s -> s.hasPermission(admin)).then(npcArg().executes(ctx -> world(ctx).flatMap(w -> npc(ctx, "npc").map(id -> {
                    List<String> out = new ArrayList<>(VillageInspector.citizen(w.villages(), id));
                    var bias = w.villages().routineBias(id);
                    out.add("Sesgo de rutina ahora: " + bias.bias() + " — " + String.join("; ", bias.reasons()));
                    return lines(ctx, out);
                })).orElse(0))))
                .then(Commands.literal("found").requires(s -> s.hasPermission(admin)).then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> found(ctx, 64, false))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(24, 512)).executes(ctx -> found(ctx, IntegerArgumentType.getInteger(ctx, "radius"), false))
                                .then(Commands.literal("plan").executes(ctx -> found(ctx, IntegerArgumentType.getInteger(ctx, "radius"), true))))))
                .then(Commands.literal("building").requires(s -> s.hasPermission(admin)).then(enumArg("kind", BuildingKind.class)
                        .executes(ctx -> building(ctx, 6)).then(Commands.argument("radius", IntegerArgumentType.integer(2, 64)).executes(ctx -> building(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static int found(CommandContext<CommandSourceStack> ctx, int radius, boolean plan) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        String name = StringArgumentType.getString(ctx, "name");
        if (w.get().villages().villageAt(dimension(p.get()), p.get().getX(), p.get().getZ()).isPresent()) return reply(ctx, false, "Ya estás dentro de una aldea.");
        Settlement s = w.get().foundSettlement(name, SettlementType.VILLAGE, dimension(p.get()), p.get().getX(), p.get().getY(), p.get().getZ(), radius, plan, false,
                Provenance.of("command", p.get().getUUID().toString(), playerName(p.get()), w.get().calendar().now()));
        return reply(ctx, true, "Fundada " + s.name() + " (radio " + radius + (plan ? ", con plano de distritos" : "") + ").");
    }

    private static int building(CommandContext<CommandSourceStack> ctx, int radius) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        Optional<BuildingKind> kind = parse(BuildingKind.class, StringArgumentType.getString(ctx, "kind"));
        if (kind.isEmpty()) return reply(ctx, false, "Tipo de edificio desconocido.");
        Optional<Village> v = w.get().villages().villageAt(dimension(p.get()), p.get().getX(), p.get().getZ());
        if (v.isEmpty()) return reply(ctx, false, "Tienes que estar dentro de una aldea.");
        Building b = w.get().villages().registerBuilding(v.get().id(), kind.get(), kind.get().name().toLowerCase(Locale.ROOT), dimension(p.get()), p.get().getX(), p.get().getY(), p.get().getZ(), radius, Building.State.BUILT, "");
        return reply(ctx, true, "Registrado " + b.name() + " en " + v.get().name() + ".");
    }

    // ------------------------------------------------------------------ economy and trade

    private static LiteralArgumentBuilder<CommandSourceStack> economyNode(int admin) {
        return Commands.literal("economy")
                .executes(ctx -> world(ctx).flatMap(w -> player(ctx).map(p -> villageHere(w, p).map(v -> lines(ctx, EconomyInspector.prices(w.economy(), v.id())))
                        .orElseGet(() -> reply(ctx, false, "No estás en ninguna aldea.")))).orElse(0))
                .then(Commands.literal("settlement").requires(s -> s.hasPermission(admin)).then(villageArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx).map(v -> lines(ctx, EconomyInspector.settlement(w.economy(), v.id())))).orElse(0))))
                .then(Commands.literal("prices").then(villageArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx).map(v -> lines(ctx, EconomyInspector.prices(w.economy(), v.id())))).orElse(0))))
                .then(Commands.literal("provenance").requires(s -> s.hasPermission(admin)).then(villageArg().then(resourceArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx)
                        .map(v -> lines(ctx, EconomyInspector.provenance(w.economy(), v.id(), StringArgumentType.getString(ctx, "resource"))))).orElse(0)))))
                .then(Commands.literal("ledger").requires(s -> s.hasPermission(admin)).then(villageArg().executes(ctx -> world(ctx).flatMap(w -> village(w, ctx).map(v -> lines(ctx, EconomyInspector.ledger(w.economy(), v.id(), 30)))).orElse(0))))
                .then(Commands.literal("merchants").requires(s -> s.hasPermission(admin)).executes(ctx -> world(ctx).map(w -> lines(ctx, EconomyInspector.merchants(w.economy()))).orElse(0)))
                .then(Commands.literal("caravans").requires(s -> s.hasPermission(admin)).executes(ctx -> world(ctx).map(w -> lines(ctx, EconomyInspector.caravans(w.economy()))).orElse(0)))
                .then(Commands.literal("routes").requires(s -> s.hasPermission(admin)).executes(ctx -> world(ctx).map(w -> lines(ctx, EconomyInspector.routes(w.economy()))).orElse(0)))
                .then(Commands.literal("contracts").requires(s -> s.hasPermission(admin)).executes(ctx -> world(ctx).map(w -> lines(ctx, EconomyInspector.contracts(w.economy()))).orElse(0)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> trade() {
        return Commands.literal("trade")
                .then(Commands.literal("sell").then(resourceArg().then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304)).executes(LivingCommand::sell))))
                .then(Commands.literal("buy").then(resourceArg().then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304)).executes(LivingCommand::buy))));
    }

    private static int sell(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        Optional<Village> v = villageHere(w.get(), p.get());
        if (v.isEmpty()) return reply(ctx, false, "Solo puedes comerciar en una aldea.");
        String resource = StringArgumentType.getString(ctx, "resource").toLowerCase(Locale.ROOT);
        ResourceItems items = service().items();
        if (!items.crosses(resource)) return reply(ctx, false, "Ese recurso no tiene objeto con el que comerciar.");
        int carried = Math.min(IntegerArgumentType.getInteger(ctx, "amount"), items.count(p.get(), resource));
        if (carried <= 0) return reply(ctx, false, "No llevas nada de eso.");
        double unit = w.get().economy().playerSellPrice(v.get().id(), resource, p.get().getUUID());
        var treasury = w.get().economy().treasury(v.get().id());
        int affordable = treasury == null || unit <= 0 ? 0 : (int) Math.floor(treasury.coins() / unit + 1e-9);
        int units = Math.min(carried, affordable);
        if (units <= 0) return reply(ctx, false, v.get().name() + " no tiene monedas para comprarte eso ahora.");
        double paid = w.get().economy().sellFromPlayer(v.get().id(), p.get().getUUID(), playerName(p.get()), resource, units, 0.7D);
        if (paid <= 0) return reply(ctx, false, "La venta no se pudo hacer.");
        items.take(p.get(), resource, units);
        return reply(ctx, true, String.format("Vendes %d de %s por %.1f monedas.", units, resource, paid));
    }

    private static int buy(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        Optional<Village> v = villageHere(w.get(), p.get());
        if (v.isEmpty()) return reply(ctx, false, "Solo puedes comerciar en una aldea.");
        String resource = StringArgumentType.getString(ctx, "resource").toLowerCase(Locale.ROOT);
        ResourceItems items = service().items();
        if (!items.crosses(resource)) return reply(ctx, false, "Ese recurso no tiene objeto con el que comerciar.");
        double unit = w.get().economy().playerBuyPrice(v.get().id(), resource, p.get().getUUID());
        double coins = w.get().economy().wealth().coins(p.get().getUUID());
        int units = (int) Math.floor(Math.min(Math.min(IntegerArgumentType.getInteger(ctx, "amount"), w.get().economy().stock(v.get().id(), resource)), unit <= 0 ? 0 : coins / unit) + 1e-9);
        if (units <= 0) return reply(ctx, false, "No hay existencias o no tienes monedas suficientes.");
        double bought = w.get().economy().buyForPlayer(v.get().id(), p.get().getUUID(), playerName(p.get()), resource, units);
        int given = items.give(p.get(), resource, bought);
        return reply(ctx, given > 0, String.format("Compras %d de %s.", given, resource));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> contracts() {
        return Commands.literal("contract")
                .then(Commands.literal("list").executes(ctx -> world(ctx).flatMap(w -> player(ctx).map(p -> villageHere(w, p).map(v -> {
                    List<String> out = new ArrayList<>();
                    for (var c : w.economy().contracts())
                        if (c.open() && c.settlement().equals(v.id()))
                            out.add(String.format("%s %s: %.0f de %s a %.1f monedas/u (quedan %.0f), hasta %s — %s", c.id().toString().substring(0, 8), c.kind().name().toLowerCase(Locale.ROOT),
                                    c.quantity(), c.resource(), c.unitPrice(), c.remaining(), w.calendar().date(c.deadline()).shortDate(), c.reason()));
                    if (out.isEmpty()) out.add(v.name() + " no ofrece contratos ahora.");
                    return lines(ctx, out);
                }).orElseGet(() -> reply(ctx, false, "No estás en ninguna aldea.")))).orElse(0)))
                .then(Commands.literal("deliver").then(Commands.argument("contract", StringArgumentType.word()).then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304)).executes(LivingCommand::deliverContract))));
    }

    private static int deliverContract(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        String prefix = StringArgumentType.getString(ctx, "contract");
        var contract = w.get().economy().contracts().stream().filter(c -> c.open() && c.id().toString().startsWith(prefix)).findFirst();
        if (contract.isEmpty()) return reply(ctx, false, "No hay ningún contrato abierto con ese identificador.");
        Optional<Village> v = villageHere(w.get(), p.get());
        if (v.isEmpty() || !v.get().id().equals(contract.get().settlement())) return reply(ctx, false, "Tienes que entregarlo en la aldea que lo pidió.");
        ResourceItems items = service().items();
        String resource = contract.get().resource();
        int units = (int) Math.min(Math.min(IntegerArgumentType.getInteger(ctx, "amount"), items.count(p.get(), resource)), Math.floor(contract.get().remaining() + 1e-9));
        if (units <= 0) return reply(ctx, false, "No llevas " + resource + " o el contrato ya está completo.");
        int taken = items.take(p.get(), resource, units);
        double before = w.get().economy().wealth().coins(p.get().getUUID());
        double delivered = w.get().economy().deliverToContract(contract.get().id(), p.get().getUUID(), playerName(p.get()), taken, 0.7D,
                Provenance.of("player", p.get().getUUID().toString(), playerName(p.get()), w.get().calendar().now()));
        double paid = w.get().economy().wealth().coins(p.get().getUUID()) - before;
        return reply(ctx, delivered > 0, String.format("Entregas %.0f de %s; cobras %.1f monedas.", delivered, resource, paid));
    }

    private static int coins(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        return p.map(pl -> reply(ctx, true, String.format("Tienes %.1f monedas.", w.get().economy().wealth().coins(pl.getUUID())))).orElse(0);
    }

    // ------------------------------------------------------------------ quests

    private static LiteralArgumentBuilder<CommandSourceStack> questNode(int admin) {
        return Commands.literal("quest")
                .then(Commands.literal("list").executes(LivingCommand::questList))
                .then(Commands.literal("info").then(questArg().executes(ctx -> world(ctx).flatMap(w -> quest(w, ctx).map(q -> lines(ctx, QuestInspector.inspect(w.quests(), q)))).orElse(0))))
                .then(Commands.literal("accept").then(questArg().executes(ctx -> questAction(ctx, (e, q, p) -> e.accept(q.id(), p.getUUID(), playerName(p))))))
                .then(Commands.literal("abandon").then(questArg().executes(ctx -> questAction(ctx, (e, q, p) -> e.abandon(q.id(), p.getUUID())))))
                .then(Commands.literal("paths").then(questArg().executes(ctx -> world(ctx).flatMap(w -> quest(w, ctx).flatMap(q -> player(ctx).map(p -> {
                    List<String> out = new ArrayList<>();
                    for (BranchSpec b : w.quests().paths(q, p.getUUID())) out.add(b.path().name().toLowerCase(Locale.ROOT) + " — " + b.label());
                    return lines(ctx, out);
                }))).orElse(0))))
                .then(Commands.literal("choose").then(questArg().then(enumArg("path", Path.class).executes(ctx -> questAction(ctx, (e, q, p) -> {
                    Optional<Path> path = Path.parse(StringArgumentType.getString(ctx, "path"));
                    return path.isEmpty() ? new QuestEngine.Result(false, "Camino desconocido.", q) : e.choose(q.id(), p.getUUID(), path.get());
                })))))
                .then(Commands.literal("deliver").then(resourceArg().then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304)).executes(LivingCommand::deliver))))
                .then(Commands.literal("history").executes(ctx -> world(ctx).flatMap(w -> player(ctx).map(p -> lines(ctx, QuestInspector.history(w.quests(), p.getUUID())))).orElse(0)))
                .then(Commands.literal("all").requires(s -> s.hasPermission(admin)).executes(ctx -> world(ctx).map(w -> lines(ctx, QuestInspector.list(w.quests()))).orElse(0)))
                .then(Commands.literal("campaigns").requires(s -> s.hasPermission(admin)).executes(ctx -> world(ctx).map(w -> lines(ctx, QuestInspector.campaigns(w.quests()))).orElse(0)));
    }

    @FunctionalInterface
    private interface QuestAction { QuestEngine.Result apply(QuestEngine engine, Quest quest, ServerPlayer player); }

    private static int questAction(CommandContext<CommandSourceStack> ctx, QuestAction action) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        Optional<Quest> q = quest(w.get(), ctx);
        if (q.isEmpty()) return 0;
        QuestEngine.Result r = action.apply(w.get().quests(), q.get(), p.get());
        return reply(ctx, r.ok(), r.message());
    }

    private static int questList(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        List<String> out = new ArrayList<>();
        for (Quest q : w.get().quests().active(p.get().getUUID())) out.add("§a[activa] §f" + q.id().toString().substring(0, 8) + " " + q.title() + " — " + q.text());
        villageHere(w.get(), p.get()).ifPresent(v -> {
            for (Quest q : w.get().quests().offeredIn(v.id()))
                out.add("§6[ofrecida] §f" + q.id().toString().substring(0, 8) + " " + q.title() + (q.giverName() == null || q.giverName().isEmpty() ? "" : " (" + q.giverName() + ")"));
        });
        if (out.isEmpty()) out.add("No tienes misiones activas y nadie aquí pide ayuda.");
        return lines(ctx, out);
    }

    private static int deliver(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<ServerPlayer> p = w.isEmpty() ? Optional.empty() : player(ctx);
        if (p.isEmpty()) return 0;
        Optional<Village> v = villageHere(w.get(), p.get());
        if (v.isEmpty()) return reply(ctx, false, "Tienes que entregarlo en una aldea.");
        String resource = StringArgumentType.getString(ctx, "resource").toLowerCase(Locale.ROOT);
        ResourceItems items = service().items();
        if (!items.crosses(resource)) return reply(ctx, false, "Ese recurso no se puede entregar como objeto.");
        int units = Math.min(IntegerArgumentType.getInteger(ctx, "amount"), items.count(p.get(), resource));
        if (units <= 0) return reply(ctx, false, "No llevas nada de eso.");
        int taken = items.take(p.get(), resource, units);
        double counted = w.get().deliver(p.get().getUUID(), playerName(p.get()), v.get().id(), resource, taken, 0.7D);
        return reply(ctx, true, String.format("Entregas %d de %s en %s; cuentan %.0f para tus misiones.", taken, resource, v.get().name(), counted));
    }

    // ------------------------------------------------------------------ families

    private static LiteralArgumentBuilder<CommandSourceStack> familyNode(int admin) {
        return Commands.literal("family").requires(s -> s.hasPermission(admin))
                .then(Commands.literal("person").then(npcArg().executes(ctx -> world(ctx).flatMap(w -> npc(ctx, "npc").map(id -> lines(ctx, FamilyInspector.person(w.families(), id)))).orElse(0))))
                .then(Commands.literal("tree").then(npcArg().executes(ctx -> world(ctx).flatMap(w -> npc(ctx, "npc").map(id -> w.families().familyOf(id)
                        .map(f -> lines(ctx, FamilyInspector.tree(w.families(), f))).orElseGet(() -> reply(ctx, false, "No tiene familia registrada.")))).orElse(0))))
                .then(Commands.literal("info").then(npcArg().executes(ctx -> world(ctx).flatMap(w -> npc(ctx, "npc").map(id -> w.families().familyOf(id)
                        .map(f -> lines(ctx, FamilyInspector.family(w.families(), f))).orElseGet(() -> reply(ctx, false, "No tiene familia registrada.")))).orElse(0))))
                .then(Commands.literal("lineage").then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> world(ctx).map(w -> w.families().findLineage(StringArgumentType.getString(ctx, "name"))
                        .map(l -> lines(ctx, FamilyInspector.lineageTree(w.families(), l))).orElseGet(() -> reply(ctx, false, "No conozco ese linaje."))).orElse(0))))
                .then(Commands.literal("heirloom")
                        .then(Commands.literal("show").then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> world(ctx).map(w -> w.families().findHeirloom(StringArgumentType.getString(ctx, "name"))
                                .map(h -> lines(ctx, FamilyInspector.heirloom(w.families(), h))).orElseGet(() -> reply(ctx, false, "No conozco esa reliquia."))).orElse(0))))
                        .then(Commands.literal("create").then(npcArg().then(Commands.argument("kind", StringArgumentType.word()).then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(LivingCommand::createHeirloom)))))
                        .then(Commands.literal("give").then(Commands.argument("heirloom", StringArgumentType.word()).then(Commands.argument("to", StringArgumentType.word()).executes(LivingCommand::giveHeirloom))))
                        .then(Commands.literal("lost").then(Commands.argument("heirloom", StringArgumentType.word()).executes(ctx -> heirloomLost(ctx, true))))
                        .then(Commands.literal("found").then(Commands.argument("heirloom", StringArgumentType.word()).executes(ctx -> heirloomLost(ctx, false)))))
                .then(Commands.literal("partners").then(npcArg().then(Commands.argument("other", StringArgumentType.word()).executes(LivingCommand::partners))))
                .then(Commands.literal("mentor").then(npcArg().then(Commands.argument("disciple", StringArgumentType.word())
                        .then(enumArg("type", yadi.samuraiai.living.family.mentorship.Mentorship.Type.class).executes(LivingCommand::mentor)))))
                .then(Commands.literal("technique").then(npcArg().then(Commands.argument("key", StringArgumentType.word()).then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(LivingCommand::technique)))))
                .then(Commands.literal("teach").then(Commands.argument("key", StringArgumentType.word()).then(Commands.argument("from", StringArgumentType.word())
                        .then(Commands.argument("to", StringArgumentType.word()).executes(LivingCommand::teach)))))
                .then(Commands.literal("lineage").then(Commands.literal("create").then(enumArg("type", yadi.samuraiai.living.family.lineage.Lineage.Type.class)
                        .then(npcArg().then(Commands.argument("name", StringArgumentType.greedyString()).executes(LivingCommand::createLineage))))))
                .then(Commands.literal("designate").then(npcArg().executes(LivingCommand::designate)))
                .then(Commands.literal("succession").then(npcArg().executes(LivingCommand::succession)))
                .then(Commands.literal("branch").then(npcArg().executes(LivingCommand::branch)))
                .then(Commands.literal("suggest").then(npcArg().executes(LivingCommand::suggest)))
                .then(Commands.literal("legacy").then(npcArg().executes(LivingCommand::legacy)))
                .then(Commands.literal("audit").executes(ctx -> world(ctx).map(w -> { List<String> problems = w.families().audit(); return problems.isEmpty() ? reply(ctx, true, "Genealogía coherente.") : lines(ctx, problems); }).orElse(0)))
                .then(Commands.literal("birth").then(npcArg().then(Commands.argument("other", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(NPCManager.getInstance().listSorted().stream().map(NPCRuntime::getName).toList(), b))
                        .then(Commands.argument("given", StringArgumentType.word()).executes(LivingCommand::birth)))))
                .then(Commands.literal("identity").then(npcArg().executes(ctx -> world(ctx).flatMap(w -> npc(ctx, "npc").map(id -> lines(ctx, FamilyInspector.identity(w.families(), id)))).orElse(0))))
                .then(Commands.literal("names").then(enumArg("culture", yadi.samuraiai.living.family.naming.cultures.NameCulture.class)
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 40)).executes(LivingCommand::sampleNames))))
                .then(Commands.literal("clan")
                        .then(Commands.literal("list").executes(ctx -> world(ctx).map(w -> {
                            var clans = w.families().clans();
                            return clans.isEmpty() ? reply(ctx, true, "No hay clanes fundados.")
                                    : lines(ctx, clans.stream().map(c -> String.format("%s [%s] — %d familia(s)", c.name(), c.status(), c.memberFamilies().size())).toList());
                        }).orElse(0)))
                        .then(Commands.literal("info").then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> world(ctx).map(w -> w.families().findClan(StringArgumentType.getString(ctx, "name"))
                                .map(c -> lines(ctx, FamilyInspector.clan(w.families(), c))).orElseGet(() -> reply(ctx, false, "No conozco ese clan."))).orElse(0))))
                        .then(Commands.literal("create").then(npcArg().then(Commands.argument("name", StringArgumentType.greedyString()).executes(LivingCommand::createClan))))
                        .then(Commands.literal("join").then(Commands.argument("clan", StringArgumentType.word()).then(npcArg().executes(ctx -> clanMembership(ctx, true)))))
                        .then(Commands.literal("leave").then(Commands.argument("clan", StringArgumentType.word()).then(npcArg().executes(ctx -> clanMembership(ctx, false))))));
    }

    private static int sampleNames(CommandContext<CommandSourceStack> ctx) {
        var culture = parse(yadi.samuraiai.living.family.naming.cultures.NameCulture.class, StringArgumentType.getString(ctx, "culture"))
                .orElse(yadi.samuraiai.living.family.naming.cultures.NameCulture.YAMATO);
        int count = IntegerArgumentType.getInteger(ctx, "count");
        return lines(ctx, FamilyInspector.sampleNames(culture, System.nanoTime(), count));
    }

    private static int createClan(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> founder = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (founder.isEmpty()) return 0;
        Optional<yadi.samuraiai.living.family.registry.FamilyRecord> f = w.get().families().familyOf(founder.get());
        if (f.isEmpty()) return reply(ctx, false, "Ese NPC no tiene familia registrada.");
        var clan = w.get().families().createClan(StringArgumentType.getString(ctx, "name"), f.get().id());
        return reply(ctx, true, "Clan " + clan.name() + " fundado.");
    }

    private static int clanMembership(CommandContext<CommandSourceStack> ctx, boolean join) {
        Optional<LivingWorld> w = world(ctx);
        if (w.isEmpty()) return 0;
        var clan = w.get().families().findClan(StringArgumentType.getString(ctx, "clan"));
        if (clan.isEmpty()) return reply(ctx, false, "No conozco ese clan.");
        Optional<UUID> member = npc(ctx, "npc");
        if (member.isEmpty()) return 0;
        Optional<yadi.samuraiai.living.family.registry.FamilyRecord> f = w.get().families().familyOf(member.get());
        if (f.isEmpty()) return reply(ctx, false, "Ese NPC no tiene familia registrada.");
        boolean ok = join ? w.get().families().joinClan(clan.get().id(), f.get().id()) : w.get().families().leaveClan(clan.get().id(), f.get().id(), "por decisión de la familia");
        return reply(ctx, ok, ok ? (join ? "La familia se une al clan." : "La familia deja el clan.") : "No se pudo completar la operación.");
    }

    private static int createHeirloom(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> owner = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (owner.isEmpty()) return 0;
        var h = w.get().families().registerHeirloom(UUID.randomUUID(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "kind"), owner.get());
        return reply(ctx, true, h.name() + " es ahora una reliquia familiar (" + h.itemId().toString().substring(0, 8) + ").");
    }

    private static int giveHeirloom(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        if (w.isEmpty()) return 0;
        var h = w.get().families().findHeirloom(StringArgumentType.getString(ctx, "heirloom"));
        if (h.isEmpty()) return reply(ctx, false, "No conozco esa reliquia.");
        Optional<UUID> to = npc(ctx, "to");
        if (to.isEmpty()) return 0;
        return reply(ctx, w.get().families().transferHeirloom(h.get().itemId(), to.get(), "entregada por comando"), h.get().name() + " cambia de manos.");
    }

    private static int heirloomLost(CommandContext<CommandSourceStack> ctx, boolean lost) {
        Optional<LivingWorld> w = world(ctx);
        if (w.isEmpty()) return 0;
        var h = w.get().families().findHeirloom(StringArgumentType.getString(ctx, "heirloom"));
        if (h.isEmpty()) return reply(ctx, false, "No conozco esa reliquia.");
        w.get().families().heirloomLost(h.get().itemId(), lost);
        return reply(ctx, true, h.get().name() + (lost ? " se ha perdido." : " ha aparecido."));
    }

    private static int partners(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> a = w.isEmpty() ? Optional.empty() : npc(ctx, "npc"), b = a.isEmpty() ? Optional.empty() : npc(ctx, "other");
        if (b.isEmpty()) return 0;
        Optional<String> refused = w.get().families().addPartners(a.get(), b.get());
        return refused.map(why -> reply(ctx, false, "Rechazado: " + why)).orElseGet(() -> reply(ctx, true, "Ahora son pareja."));
    }

    /** The school a person leads, if any: a master's disciples and techniques belong to it. */
    private static UUID schoolOf(LivingWorld w, UUID leader) {
        return w.families().lineages().stream().filter(l -> leader.equals(l.leader())).map(l -> l.id()).findFirst().orElse(null);
    }

    private static int mentor(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> m = w.isEmpty() ? Optional.empty() : npc(ctx, "npc"), d = m.isEmpty() ? Optional.empty() : npc(ctx, "disciple");
        if (d.isEmpty()) return 0;
        var type = parse(yadi.samuraiai.living.family.mentorship.Mentorship.Type.class, StringArgumentType.getString(ctx, "type")).orElse(yadi.samuraiai.living.family.mentorship.Mentorship.Type.CRAFT);
        UUID school = schoolOf(w.get(), m.get());
        w.get().families().mentor(m.get(), d.get(), type, school, false);
        return reply(ctx, true, "Empieza el aprendizaje" + (school == null ? "." : " dentro de su escuela."));
    }

    private static int technique(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> id = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (id.isEmpty()) return 0;
        var t = w.get().families().technique(StringArgumentType.getString(ctx, "key"), StringArgumentType.getString(ctx, "name"), id.get(), schoolOf(w.get(), id.get()), 0.6);
        return reply(ctx, true, "Técnica registrada: " + t.name() + ".");
    }

    private static int teach(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> a = w.isEmpty() ? Optional.empty() : npc(ctx, "from"), b = a.isEmpty() ? Optional.empty() : npc(ctx, "to");
        if (b.isEmpty()) return 0;
        boolean ok = w.get().families().teach(StringArgumentType.getString(ctx, "key"), a.get(), b.get(), yadi.samuraiai.living.family.knowledge.Technique.Via.ORAL);
        return reply(ctx, ok, ok ? "Enseñanza registrada." : "No se pudo: quien enseña no conoce esa técnica o quien aprende ya la sabe.");
    }

    private static int createLineage(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> id = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (id.isEmpty()) return 0;
        var type = parse(yadi.samuraiai.living.family.lineage.Lineage.Type.class, StringArgumentType.getString(ctx, "type")).orElse(yadi.samuraiai.living.family.lineage.Lineage.Type.CUSTOM);
        String trade = w.get().villages().citizen(id.get()).map(c -> c.profession()).orElse("");
        var l = w.get().families().createLineage(StringArgumentType.getString(ctx, "name"), type, id.get(), trade, null);
        return reply(ctx, true, "Fundado el linaje " + l.name() + ".");
    }

    private static int designate(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> id = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (id.isEmpty()) return 0;
        var f = w.get().families().familyOf(id.get());
        if (f.isEmpty()) return reply(ctx, false, "No tiene familia.");
        return reply(ctx, w.get().families().designateHeir(f.get().id(), id.get()), "Designado heredero de la familia " + f.get().name() + ".");
    }

    private static int succession(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> id = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (id.isEmpty()) return 0;
        var f = w.get().families().familyOf(id.get());
        if (f.isEmpty()) return reply(ctx, false, "No tiene familia.");
        List<String> out = new ArrayList<>();
        for (var r : w.get().families().successionRanking(f.get().id())) out.add(String.format("%s %.2f — %s", r.name(), r.score(), String.join(", ", r.reasons())));
        return lines(ctx, out);
    }

    private static int branch(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> id = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (id.isEmpty()) return 0;
        var f = w.get().families().familyOf(id.get());
        if (f.isEmpty()) return reply(ctx, false, "No tiene familia.");
        var b = w.get().families().branch(f.get().id(), id.get(), "funda su propia rama", w.get().villages().villageOf(id.get()).map(Village::id).orElse(f.get().village()));
        return reply(ctx, true, "Nueva rama: " + b.name() + ".");
    }

    private static int suggest(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> id = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (id.isEmpty()) return 0;
        return lines(ctx, w.get().families().professionSuggestions(id.get()).stream()
                .map(sg -> String.format("%s %.2f — %s", sg.profession(), sg.score(), String.join(", ", sg.reasons()))).toList());
    }

    private static int legacy(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        Optional<UUID> id = w.isEmpty() ? Optional.empty() : npc(ctx, "npc");
        if (id.isEmpty()) return 0;
        var l = w.get().families().legacy(id.get());
        List<String> out = new ArrayList<>();
        out.add(String.format("Legado total %.2f", l.total()));
        for (var c : l.causes()) out.add(String.format("  %s %.2f — %s", c.kind(), c.weight(), c.text()));
        return lines(ctx, out);
    }

    /** Registers a birth on purpose (births are never simulated): the child is a person of the family, without a body yet. */
    private static int birth(CommandContext<CommandSourceStack> ctx) {
        Optional<LivingWorld> w = world(ctx);
        if (w.isEmpty()) return 0;
        Optional<UUID> a = npc(ctx, "npc"), b = a.isEmpty() ? Optional.empty() : npc(ctx, "other");
        if (a.isEmpty() || b.isEmpty()) return 0;
        UUID village = w.get().villages().villageOf(a.get()).map(Village::id).orElse(null);
        try {
            Person child = w.get().families().birth(a.get(), b.get(), StringArgumentType.getString(ctx, "given"), village);
            return reply(ctx, true, "Nace " + child.name().full() + ".");
        } catch (IllegalArgumentException | IllegalStateException refused) {
            return reply(ctx, false, "No se puede registrar ese nacimiento: " + refused.getMessage());
        }
    }
}
