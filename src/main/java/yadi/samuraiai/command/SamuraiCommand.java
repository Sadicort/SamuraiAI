package yadi.samuraiai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import yadi.samuraiai.ai.AIRequestQueue;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.ollama.OllamaClient;
import yadi.samuraiai.ollama.OllamaConfig;
import yadi.samuraiai.registry.NPCTypeRegistry;
import yadi.samuraiai.runtime.DialogueRouter;
import yadi.samuraiai.spawn.NPCSpawnRequest;
import yadi.samuraiai.spawn.NPCSpawnResult;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

import java.util.List;
import java.util.Optional;

/**
 * The {@code /samuraiai} command tree.
 *
 * <p>This class only translates a player input into a request. The actual
 * work belongs to {@link NPCSpawnService} and {@link DialogueRouter}.
 *
 * <p>Note the two-argument sendSuccess calls below. The previous version used
 * the Supplier overload, which only exists from Minecraft 1.20 onward; against
 * this project declared 1.19.2 target that does not compile, so the command
 * could never have been registered at all.
 */
public final class SamuraiCommand {

    /** Operator level required for anything that changes the world. */
    private static final int ADMIN_LEVEL = 2;

    private static final OllamaClient PROBE_CLIENT = new OllamaClient();

    private SamuraiCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("samuraiai")

                .then(Commands.literal("spawn")
                        .requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        NPCTypeRegistry.getInstance().typeNames(), builder))
                                .executes(ctx -> spawn(ctx, null))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> spawn(ctx,
                                                StringArgumentType.getString(ctx, "name"))))))

                .then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        activeNames(), builder))
                                .executes(SamuraiCommand::remove)))

                .then(Commands.literal("removeall")
                        .requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .executes(SamuraiCommand::removeAll))

                .then(Commands.literal("talk")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        activeNames(), builder))
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(SamuraiCommand::talk))))

                .then(Commands.literal("list").executes(SamuraiCommand::list))

                .then(Commands.literal("types").executes(SamuraiCommand::types))

                .then(yadi.samuraiai.ai.navigation.world.NavigationCommand.node(ADMIN_LEVEL))

                .then(yadi.samuraiai.ai.perception.world.PerceptionCommand.node(ADMIN_LEVEL))
                .then(yadi.samuraiai.ai.scheduler.world.SchedulerCommand.node(ADMIN_LEVEL))
                .then(yadi.samuraiai.ai.cognition.world.CognitionCommand.mind(ADMIN_LEVEL))
                .then(yadi.samuraiai.ai.cognition.world.CognitionCommand.memory(ADMIN_LEVEL))
                .then(yadi.samuraiai.ai.cognition.world.CognitionCommand.relationship(ADMIN_LEVEL))
                .then(yadi.samuraiai.ai.cognition.world.CognitionCommand.emotion(ADMIN_LEVEL))
                .then(yadi.samuraiai.ai.cognition.world.CognitionCommand.knowledge(ADMIN_LEVEL))
                .then(yadi.samuraiai.ai.cognition.world.CognitionCommand.society(ADMIN_LEVEL))
                .then(yadi.samuraiai.living.server.LivingCommand.node(ADMIN_LEVEL))

                .then(Commands.literal("status")
                        .requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .executes(SamuraiCommand::status)));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, String requestedName) {

        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando solo puede ejecutarlo un jugador."));
            return 0;
        }

        String type = StringArgumentType.getString(ctx, "type");
        Vec3 pos = player.position();

        SpawnLocation location = new SpawnLocation(
                ServerWorlds.dimensionKeyOf(player.getLevel()),
                pos.x, pos.y, pos.z,
                player.getYRot());

        NPCSpawnResult result = NPCSpawnService.getInstance()
                .spawn(new NPCSpawnRequest(NPCTypeId.of(type), requestedName, location));

        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(Component.literal(result.message()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {

        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        Optional<NPCRuntime> runtime = NPCManager.getInstance().findByName(name);

        if (runtime.isEmpty()) {
            source.sendFailure(Component.literal("No hay ningun NPC activo llamado " + name + "."));
            return 0;
        }

        NPCSpawnService.getInstance().remove(runtime.get().getId(), "comando", true);

        source.sendSuccess(Component.literal("NPC " + name + " eliminado.")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int removeAll(CommandContext<CommandSourceStack> ctx) {

        int removed = NPCSpawnService.getInstance().removeAll("comando removeall", true);

        ctx.getSource().sendSuccess(
                Component.literal("Se eliminaron " + removed + " NPCs.").withStyle(ChatFormatting.YELLOW),
                true);

        return removed;
    }

    private static int talk(CommandContext<CommandSourceStack> ctx) {

        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando solo puede ejecutarlo un jugador."));
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        String message = StringArgumentType.getString(ctx, "message");

        Optional<NPCRuntime> runtime = NPCManager.getInstance().findByName(name);

        if (runtime.isEmpty()) {
            source.sendFailure(Component.literal("No hay ningun NPC activo llamado " + name + "."));
            return 0;
        }

        DialogueRouter.speakTo(runtime.get(), player, message);

        // No confirmation message: the NPC own reply is the feedback, and
        // echoing here would show the exchange twice to the player who spoke.
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {

        CommandSourceStack source = ctx.getSource();
        List<NPCRuntime> active = NPCManager.getInstance().listSorted();

        if (active.isEmpty()) {
            source.sendSuccess(Component.literal("No hay NPCs activos."), false);
            return 0;
        }

        source.sendSuccess(Component.literal("NPCs activos (" + active.size() + "):")
                .withStyle(ChatFormatting.AQUA), false);

        for (NPCRuntime runtime : active) {

            SpawnLocation location = runtime.getInstance().getLocation();

            String line = "- " + runtime.getName()
                    + " (" + runtime.getInstance().getIdentity().type().value() + ") "
                    + runtime.getState()
                    + (runtime.getCurrentGoal() == null ? "" : " objetivo=" + runtime.getCurrentGoal())
                    + (location == null ? "" : " en " + location.toShortString());

            source.sendSuccess(Component.literal(line), false);
        }

        return active.size();
    }

    private static int types(CommandContext<CommandSourceStack> ctx) {

        List<String> names = NPCTypeRegistry.getInstance().typeNames();

        ctx.getSource().sendSuccess(
                Component.literal("Tipos registrados: " + String.join(", ", names))
                        .withStyle(ChatFormatting.AQUA),
                false);

        return names.size();
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {

        CommandSourceStack source = ctx.getSource();
        AIRequestQueue.Stats stats = AIRequestQueue.getInstance().stats();

        source.sendSuccess(Component.literal("SamuraiAI").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(Component.literal("- NPCs activos: "
                + NPCManager.getInstance().count() + "/" + SamuraiSettings.maxActiveNpcs()), false);
        source.sendSuccess(Component.literal("- Modelo: " + OllamaConfig.model()
                + " en " + OllamaConfig.host()), false);
        source.sendSuccess(Component.literal("- Peticiones: " + stats.succeeded() + " ok, "
                + stats.failed() + " fallidas, " + stats.dropped() + " descartadas, "
                + stats.inFlight() + "/" + stats.capacity() + " en curso, " + stats.queued() + " en cola, "
                + stats.cancelled() + " canceladas, " + stats.timeouts() + " timeout, media "
                + String.format(java.util.Locale.ROOT, "%.1fms", stats.averageMillis())), false);

        probeOllama(source);

        return 1;
    }

    /**
     * The reachability check is network I/O, so it runs off-thread and reports
     * back through server.execute, which queues the reply onto the server
     * thread. Sending a chat component straight from an HTTP callback would
     * touch server state from the wrong thread.
     */
    private static void probeOllama(CommandSourceStack source) {

        var delivery = yadi.samuraiai.runtime.ServerScheduler.getInstance().executor();
        new yadi.samuraiai.ollama.OllamaDiagnostics().check().thenAccept(result -> delivery.execute(() ->
                source.sendSuccess(Component.literal("- Ollama: " + result.status() + " (" + result.detail() + ")"), false)));
    }

    private static List<String> activeNames() {
        return NPCManager.getInstance().listSorted().stream()
                .map(NPCRuntime::getName)
                .toList();
    }
}
