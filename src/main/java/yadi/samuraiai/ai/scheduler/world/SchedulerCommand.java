package yadi.samuraiai.ai.scheduler.world;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.scheduler.debug.SchedulerInspector;
import yadi.samuraiai.ai.scheduler.formation.FormationType;
import yadi.samuraiai.ai.scheduler.group.GroupType;
import yadi.samuraiai.ai.scheduler.personality.DriftCause;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.ServerWorlds;

/** {@code /samuraiai scheduler ...}: status, per-NPC inspector, zones, groups, overlay and experiences. Operators only. */
public final class SchedulerCommand {
    private SchedulerCommand() { }

    private static SchedulerService service() { return SchedulerService.getInstance(); }

    private static List<String> names() { return NPCManager.getInstance().listSorted().stream().map(NPCRuntime::getName).toList(); }

    public static LiteralArgumentBuilder<CommandSourceStack> node(int permission) {
        return Commands.literal("scheduler").requires(source -> source.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, SchedulerInspector.summary(service().scheduler().metrics().snapshot(), service().scheduler()))))
                .then(Commands.literal("metrics").then(Commands.literal("reset").executes(ctx -> {
                    service().scheduler().metrics().reset();
                    ctx.getSource().sendSuccess(Component.literal("Scheduler metrics reset."), true);
                    return 1;
                })))
                .then(Commands.literal("debug").executes(SchedulerCommand::debug))
                .then(Commands.literal("inspect").then(Commands.argument("npc", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b)).executes(SchedulerCommand::inspect)))
                .then(Commands.literal("groups").executes(ctx -> lines(ctx, SchedulerInspector.groups(service().scheduler()))))
                .then(Commands.literal("zones").executes(ctx -> lines(ctx, SchedulerInspector.zones(service().scheduler(), service().currentTick()))))
                .then(zoneNode())
                .then(groupNode())
                .then(Commands.literal("experience").then(Commands.argument("npc", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b))
                        .then(Commands.argument("cause", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(DriftCause.values()).map(Enum::name).toList(), b))
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D, 100.0D)).executes(SchedulerCommand::experience)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> zoneNode() {
        return Commands.literal("zone")
                .then(Commands.literal("add").then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("kind", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(ZoneKind.values()).map(Enum::name).toList(), b))
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0D, 128.0D))
                                        .executes(ctx -> addZone(ctx, 4))
                                        .then(Commands.argument("capacity", IntegerArgumentType.integer(1, 64)).executes(ctx -> addZone(ctx, IntegerArgumentType.getInteger(ctx, "capacity"))))))))
                .then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                        service().scheduler().zoneRegistry().all().stream().map(Zone::id).toList(), b)).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    boolean removed = service().scheduler().zoneRegistry().remove(id);
                    if (removed) saveZones(ctx);
                    return reply(ctx, removed, removed ? "Zona " + id + " eliminada." : "No existe la zona " + id + ".");
                })))
                .then(Commands.literal("claim").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("owner", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b)).executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id"), ownerName = StringArgumentType.getString(ctx, "owner");
                            Optional<NPCRuntime> npc = NPCManager.getInstance().findByName(ownerName);
                            String owner = npc.map(n -> n.getId().toString()).orElse(ownerName);
                            boolean claimed = service().scheduler().zoneRegistry().claim(id, owner);
                            if (claimed) saveZones(ctx);
                            return reply(ctx, claimed, claimed ? "Zona " + id + " reclamada por " + ownerName + "." : "No existe la zona " + id + ".");
                        }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> groupNode() {
        return Commands.literal("group")
                .then(Commands.literal("create").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(GroupType.values()).map(Enum::name).toList(), b)).executes(ctx -> {
                            Optional<GroupType> type = GroupType.parse(StringArgumentType.getString(ctx, "type"));
                            if (type.isEmpty()) return reply(ctx, false, "Tipo de grupo desconocido.");
                            service().scheduler().groupCoordinator().create(StringArgumentType.getString(ctx, "id"), type.get());
                            return reply(ctx, true, "Grupo " + StringArgumentType.getString(ctx, "id") + " creado.");
                        }))))
                .then(Commands.literal("add").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("npc", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b)).executes(ctx -> {
                            Optional<NPCRuntime> npc = NPCManager.getInstance().findByName(StringArgumentType.getString(ctx, "npc"));
                            if (npc.isEmpty()) return reply(ctx, false, "No hay ningun NPC con ese nombre.");
                            boolean joined = service().scheduler().groupCoordinator().join(StringArgumentType.getString(ctx, "id"), npc.get().getId(), service().currentTick());
                            return reply(ctx, joined, joined ? "Anadido al grupo." : "El grupo no existe o esta lleno.");
                        }))))
                .then(Commands.literal("disband").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                    service().scheduler().groupCoordinator().disband(StringArgumentType.getString(ctx, "id"));
                    return reply(ctx, true, "Grupo disuelto.");
                })))
                .then(Commands.literal("formation").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("shape", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(FormationType.values()).map(Enum::name).toList(), b)).executes(ctx -> {
                            var group = service().scheduler().groupCoordinator().group(StringArgumentType.getString(ctx, "id"));
                            if (group.isEmpty()) return reply(ctx, false, "No existe ese grupo.");
                            try { group.get().forceFormation(FormationType.valueOf(StringArgumentType.getString(ctx, "shape").toUpperCase(Locale.ROOT))); }
                            catch (IllegalArgumentException error) { return reply(ctx, false, "Formacion desconocida."); }
                            return reply(ctx, true, "Formacion fijada.");
                        }))));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, boolean ok, String message) {
        if (ok) ctx.getSource().sendSuccess(Component.literal(message), true); else ctx.getSource().sendFailure(Component.literal(message));
        return ok ? 1 : 0;
    }

    private static int lines(CommandContext<CommandSourceStack> ctx, List<String> text) {
        for (String line : text) ctx.getSource().sendSuccess(Component.literal(line), false);
        return text.size();
    }

    private static void saveZones(CommandContext<CommandSourceStack> ctx) {
        ZoneStore.save(ctx.getSource().getServer(), service().scheduler().zoneRegistry());
    }

    private static int addZone(CommandContext<CommandSourceStack> ctx, int capacity) {
        ZoneKind kind;
        try { kind = ZoneKind.valueOf(StringArgumentType.getString(ctx, "kind").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { return reply(ctx, false, "Tipo de zona desconocido."); }
        var pos = ctx.getSource().getPosition();
        String dimension = ServerWorlds.dimensionKeyOf(ctx.getSource().getLevel());
        String id = StringArgumentType.getString(ctx, "id");
        service().scheduler().zoneRegistry().add(new Zone(id, dimension, kind, pos.x, pos.y, pos.z, DoubleArgumentType.getDouble(ctx, "radius"), capacity, null, null, null));
        saveZones(ctx);
        return reply(ctx, true, "Zona " + id + " (" + kind + ") creada en " + Math.round(pos.x) + "," + Math.round(pos.y) + "," + Math.round(pos.z) + ".");
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return reply(ctx, false, "Solo un jugador puede activar el overlay.");
        boolean on = service().toggleDebug(player.getUUID());
        return reply(ctx, true, "Scheduler overlay " + (on ? "ON" : "OFF") + " (anillos=zonas por tipo, rojo=alerta, dorado=destino de la rutina, blanco=lider, cian=puesto en formacion)");
    }

    private static int inspect(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "npc");
        Optional<NPCRuntime> npc = NPCManager.getInstance().findByName(name);
        if (npc.isEmpty()) return reply(ctx, false, "No hay ningun NPC activo llamado " + name + ".");
        var schedule = service().scheduler().schedule(npc.get().getId());
        if (schedule.isEmpty()) return reply(ctx, true, name + ": aun sin horario (espera unos ticks).");
        return lines(ctx, SchedulerInspector.inspect(schedule.get(), service().currentTick()));
    }

    private static int experience(CommandContext<CommandSourceStack> ctx) {
        Optional<NPCRuntime> npc = NPCManager.getInstance().findByName(StringArgumentType.getString(ctx, "npc"));
        if (npc.isEmpty()) return reply(ctx, false, "No hay ningun NPC con ese nombre.");
        DriftCause cause;
        try { cause = DriftCause.valueOf(StringArgumentType.getString(ctx, "cause").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { return reply(ctx, false, "Experiencia desconocida."); }
        service().scheduler().experience(npc.get().getId(), cause, DoubleArgumentType.getDouble(ctx, "amount"));
        return reply(ctx, true, "Experiencia " + cause + " aplicada.");
    }
}
