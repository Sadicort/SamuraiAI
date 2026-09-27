package yadi.samuraiai.ai.navigation.world;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.navigation.debug.NavigationInspector;
import yadi.samuraiai.ai.navigation.engine.NavigationOptions;
import yadi.samuraiai.ai.navigation.engine.NavigationSession;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.movement.MovementBody;
import yadi.samuraiai.ai.navigation.zones.DangerZone;
import yadi.samuraiai.ai.navigation.zones.HazardType;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.SpawnLocation;

/** {@code /samuraiai nav ...}: inspector, metrics, manual routing and the debug overlay. Operators only. */
public final class NavigationCommand {
    private NavigationCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> node(int permission) {
        return Commands.literal("nav").requires(source -> source.hasPermission(permission))
                .then(Commands.literal("status").executes(NavigationCommand::status))
                .then(Commands.literal("metrics")
                        .then(Commands.literal("reset").executes(ctx -> {
                            NavigationService.getInstance().metrics().reset();
                            ctx.getSource().sendSuccess(Component.literal("Navigation metrics reset."), true);
                            return 1;
                        })))
                .then(Commands.literal("debug").executes(NavigationCommand::debug))
                .then(Commands.literal("inspect").then(npcArgument().executes(NavigationCommand::inspect)))
                .then(Commands.literal("cancel").then(npcArgument().executes(NavigationCommand::cancel)))
                .then(Commands.literal("goto").then(npcArgument().then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(NavigationCommand::go))))
                .then(Commands.literal("terrain").then(npcArgument().executes(NavigationCommand::terrain)))
                .then(Commands.literal("danger")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.5D, 32.0D))
                                        .then(Commands.argument("score", DoubleArgumentType.doubleArg(1.0D, 500.0D))
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600)).executes(NavigationCommand::danger))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> npcArgument() {
        return Commands.argument("npc", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                NPCManager.getInstance().listSorted().stream().map(NPCRuntime::getName).toList(), builder));
    }

    private static Optional<NPCRuntime> npc(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "npc");
        Optional<NPCRuntime> found = NPCManager.getInstance().findByName(name);
        if (found.isEmpty()) ctx.getSource().sendFailure(Component.literal("No hay ningun NPC activo llamado " + name + "."));
        return found;
    }

    private static int lines(CommandContext<CommandSourceStack> ctx, List<String> text) {
        for (String line : text) ctx.getSource().sendSuccess(Component.literal(line), false);
        return text.size();
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        var service = NavigationService.getInstance();
        var first = service.dimensions().stream().findFirst();
        if (first.isEmpty()) {
            ctx.getSource().sendSuccess(Component.literal("Navegacion " + (service.enabled() ? "activa" : "desactivada") + ": sin sesiones aun."), false);
            return 1;
        }
        return lines(ctx, NavigationInspector.summary(first.get().engine()));
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Solo un jugador puede activar el overlay."));
            return 0;
        }
        boolean on = NavigationService.getInstance().toggleDebug(player.getUUID());
        ctx.getSource().sendSuccess(Component.literal("Navigation overlay " + (on ? "ON" : "OFF")
                + " (verde=ruta, amarillo=nodo actual, rojo=destino, magenta=bloqueado, azul=peligro)"), false);
        return 1;
    }

    private static int inspect(CommandContext<CommandSourceStack> ctx) {
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return 0;
        Optional<NavigationSession> session = NavigationService.getInstance().session(npc.get().getId());
        if (session.isEmpty()) {
            ctx.getSource().sendSuccess(Component.literal(npc.get().getName() + ": sin sesion de navegacion."), false);
            return 1;
        }
        MovementBody body = npc.get().getController().movementBody(npc.get().getInstance()).orElse(null);
        var engine = NavigationService.getInstance().dimensions().stream().map(NavigationService.Dimension::engine).findFirst().orElseThrow();
        return lines(ctx, NavigationInspector.inspect(session.get(), engine, body));
    }

    private static int cancel(CommandContext<CommandSourceStack> ctx) {
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return 0;
        NavigationService.getInstance().cancel(npc.get().getId(), "cancelled by command");
        ctx.getSource().sendSuccess(Component.literal("Navegacion de " + npc.get().getName() + " cancelada."), true);
        return 1;
    }

    private static int go(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return 0;
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        String dimension = ctx.getSource().getLevel().dimension().location().toString();
        var handle = NavigationService.getInstance().navigate(npc.get(), new SpawnLocation(dimension, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F),
                NavigationOptions.walk("command"));
        if (handle.done()) {
            ctx.getSource().sendFailure(Component.literal("No se pudo iniciar: " + handle.failure() + " " + handle.failureDetail()));
            return 0;
        }
        ctx.getSource().sendSuccess(Component.literal(npc.get().getName() + " camina hacia " + pos.toShortString() + " (ver: /samuraiai nav inspect)"), true);
        return 1;
    }

    private static int terrain(CommandContext<CommandSourceStack> ctx) {
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return 0;
        var body = npc.get().getController().movementBody(npc.get().getInstance());
        if (body.isEmpty()) { ctx.getSource().sendFailure(Component.literal("El NPC no tiene cuerpo fisico.")); return 0; }
        var level = ctx.getSource().getLevel();
        var service = NavigationService.getInstance();
        var dimension = service.dimensions().stream().filter(d -> d.level() == level).findFirst();
        if (dimension.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Aun no hay motor de navegacion en esta dimension.")); return 0; }
        var snapshot = dimension.get().scanner().scan(body.get().block(), yadi.samuraiai.ai.navigation.engine.NavigationSettings.current().scanRadius(), service.currentTick());
        ctx.getSource().sendSuccess(Component.literal("Terreno (radio " + snapshot.radius() + "): celdas=" + snapshot.standableCells()
                + " peligro medio=" + String.format("%.1f", snapshot.averageDanger()) + " max=" + String.format("%.0f", snapshot.maxDanger())
                + " " + snapshot.counts()), false);
        return 1;
    }

    private static int danger(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        var level = ctx.getSource().getLevel();
        var service = NavigationService.getInstance();
        var dimension = service.dimensions().stream().filter(d -> d.level() == level).findFirst();
        if (dimension.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Aun no hay motor de navegacion en esta dimension.")); return 0; }
        double radius = DoubleArgumentType.getDouble(ctx, "radius"), score = DoubleArgumentType.getDouble(ctx, "score");
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        dimension.get().dangers().add(new DangerZone(dimension.get().key(), new NavPos(pos.getX(), pos.getY(), pos.getZ()), radius, score,
                service.currentTick() + seconds * 20L, HazardType.CUSTOM, "command"));
        ctx.getSource().sendSuccess(Component.literal("Zona de peligro creada en " + pos.toShortString() + " durante " + seconds + "s."), true);
        return 1;
    }
}
