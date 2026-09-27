package yadi.samuraiai.ai.perception.world;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.perception.debug.PerceptionInspector;
import yadi.samuraiai.ai.perception.hearing.SoundCategory;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;

/** {@code /samuraiai perception ...}: metrics, inspector, overlay and a test sound generator. Operators only. */
public final class PerceptionCommand {
    private PerceptionCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> node(int permission) {
        return Commands.literal("perception").requires(source -> source.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, PerceptionInspector.summary(PerceptionService.getInstance().metrics().snapshot()))))
                .then(Commands.literal("metrics").then(Commands.literal("reset").executes(ctx -> {
                    PerceptionService.getInstance().metrics().reset();
                    ctx.getSource().sendSuccess(Component.literal("Perception metrics reset."), true);
                    return 1;
                })))
                .then(Commands.literal("debug").executes(PerceptionCommand::debug))
                .then(Commands.literal("inspect").then(Commands.argument("npc", StringArgumentType.word()).suggests((ctx, builder) ->
                        SharedSuggestionProvider.suggest(NPCManager.getInstance().listSorted().stream().map(NPCRuntime::getName).toList(), builder)).executes(PerceptionCommand::inspect)))
                .then(Commands.literal("sound").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("category", StringArgumentType.word()).suggests((ctx, builder) ->
                                SharedSuggestionProvider.suggest(Arrays.stream(SoundCategory.values()).map(Enum::name).toList(), builder))
                                .then(Commands.argument("loudness", DoubleArgumentType.doubleArg(0.0D, 1.0D)).executes(PerceptionCommand::sound)))));
    }

    private static int lines(CommandContext<CommandSourceStack> ctx, List<String> text) {
        for (String line : text) ctx.getSource().sendSuccess(Component.literal(line), false);
        return text.size();
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Solo un jugador puede activar el overlay."));
            return 0;
        }
        boolean on = PerceptionService.getInstance().toggleDebug(player.getUUID());
        ctx.getSource().sendSuccess(Component.literal("Perception overlay " + (on ? "ON" : "OFF") + " (amarillo=campo visual, verde=visto, naranja=tapado, gris=memoria, cian=sonido, rojo=amenaza, dorado=investigar)"), false);
        return 1;
    }

    private static int inspect(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "npc");
        Optional<NPCRuntime> npc = NPCManager.getInstance().findByName(name);
        if (npc.isEmpty()) { ctx.getSource().sendFailure(Component.literal("No hay ningun NPC activo llamado " + name + ".")); return 0; }
        var state = PerceptionService.getInstance().state(npc.get().getId());
        if (state.isEmpty() || state.get().snapshot == null) {
            ctx.getSource().sendSuccess(Component.literal(name + ": aun sin percepcion (espera unos ticks)."), false);
            return 1;
        }
        return lines(ctx, PerceptionInspector.inspect(state.get().snapshot, state.get()));
    }

    private static int sound(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        SoundCategory category;
        try { category = SoundCategory.valueOf(StringArgumentType.getString(ctx, "category").toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException error) { ctx.getSource().sendFailure(Component.literal("Categoria desconocida.")); return 0; }
        PerceptionService.getInstance().sound(ctx.getSource().getLevel(), category, null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, DoubleArgumentType.getDouble(ctx, "loudness"));
        ctx.getSource().sendSuccess(Component.literal("Sonido " + category + " emitido en " + pos.toShortString() + "."), true);
        return 1;
    }
}
