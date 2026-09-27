package yadi.samuraiai.ai.cognition.world;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import yadi.samuraiai.ai.cognition.debug.CognitiveInspector;
import yadi.samuraiai.ai.cognition.engine.CognitionEngine;
import yadi.samuraiai.ai.cognition.engine.CognitionOutcome;
import yadi.samuraiai.ai.cognition.engine.ExperienceInput;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.trace.TraceStep;
import yadi.samuraiai.ai.emotion.debug.EmotionInspector;
import yadi.samuraiai.ai.emotion.model.EmotionTrigger;
import yadi.samuraiai.ai.emotion.model.RecoverySource;
import yadi.samuraiai.ai.emotion.model.Technique;
import yadi.samuraiai.ai.emotion.model.TriggerSource;
import yadi.samuraiai.ai.knowledge.debug.KnowledgeInspector;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.society.CommunityKind;
import yadi.samuraiai.ai.memory.debug.MemoryInspector;
import yadi.samuraiai.ai.memory.diagnostics.MemoryDiagnostics;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.relationship.debug.SocialInspector;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.PromiseRecord;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.ServerWorlds;

/**
 * The cognitive commands: {@code /samuraiai mind|memory|relationship|emotion|knowledge|society ...}. They inspect any NPC's memories,
 * relationships, feelings, beliefs and communities, explain why they are what they are, and let an operator create experiences,
 * promises, communities and lessons by hand to see the consequences. Operators only.
 */
public final class CognitionCommand {
    private CognitionCommand() { }

    private static CognitionService service() { return CognitionService.getInstance(); }
    private static CognitionEngine engine() { return service().engine(); }
    private static long now() { return service().now(); }

    private static List<String> names() { return NPCManager.getInstance().listSorted().stream().map(NPCRuntime::getName).toList(); }
    private static List<String> everyone() {
        List<String> all = new ArrayList<>(names());
        ServerWorlds.onlinePlayers().forEach(p -> all.add(p.getGameProfile().getName()));
        return all;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> npcArg() {
        return Commands.argument("npc", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> otherArg(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(everyone(), b));
    }

    private static <E extends Enum<E>> RequiredArgumentBuilder<CommandSourceStack, String> enumArg(String name, Class<E> type) {
        return Commands.argument(name, StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(type.getEnumConstants()).map(Enum::name).toList(), b));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, boolean ok, String message) {
        if (ok) ctx.getSource().sendSuccess(Component.literal(message), true); else ctx.getSource().sendFailure(Component.literal(message));
        return ok ? 1 : 0;
    }

    private static int lines(CommandContext<CommandSourceStack> ctx, List<String> lines) {
        if (lines.isEmpty()) return reply(ctx, true, "(sin datos)");
        int shown = 0;
        for (String line : lines) { if (shown++ >= 60) { ctx.getSource().sendSuccess(Component.literal("... (" + (lines.size() - 60) + " líneas más)"), false); break; } ctx.getSource().sendSuccess(Component.literal(line), false); }
        return lines.size();
    }

    private static Optional<NPCRuntime> npc(CommandContext<CommandSourceStack> ctx) { return NPCManager.getInstance().findByName(StringArgumentType.getString(ctx, "npc")); }

    private static Optional<EntityRef> other(String name) {
        Optional<ServerPlayer> player = ServerWorlds.playerByName(name);
        if (player.isPresent()) return Optional.of(EntityRef.player(player.get().getUUID(), player.get().getGameProfile().getName()));
        return NPCManager.getInstance().findByName(name).map(n -> EntityRef.npc(n.getId(), n.getName()));
    }

    private static <E extends Enum<E>> Optional<E> parse(Class<E> type, String text) {
        try { return Optional.of(Enum.valueOf(type, text.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    private static boolean ready(CommandContext<CommandSourceStack> ctx) {
        if (service().enabled()) return true;
        reply(ctx, false, "La capa cognitiva está desactivada o el servidor aún no la ha iniciado.");
        return false;
    }

    // ------------------------------------------------------------------ mind

    public static LiteralArgumentBuilder<CommandSourceStack> mind(int permission) {
        return Commands.literal("mind").requires(s -> s.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, status())))
                .then(Commands.literal("inspect").then(npcArg().executes(CognitionCommand::inspect)))
                .then(Commands.literal("trace").then(npcArg().executes(ctx -> npc(ctx).map(n -> lines(ctx, trace(n.getId()))).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("experience").then(npcArg().then(enumArg("kind", ExperienceKind.class).executes(ctx -> experience(ctx, null))
                        .then(otherArg("actor").executes(ctx -> experience(ctx, StringArgumentType.getString(ctx, "actor")))))))
                .then(Commands.literal("why").then(npcArg().then(Commands.argument("what", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                        List.of("trust", "respect", "honor", "affinity", "rivalry", "fear", "loyalty", "emotion", "knowledge"), b)).then(Commands.argument("about", StringArgumentType.greedyString()).executes(CognitionCommand::why)))))
                .then(Commands.literal("save").executes(ctx -> ready(ctx) ? reply(ctx, true, "Guardados " + engine().saveAll(now()) + " archivos cognitivos.") : 0))
                .then(Commands.literal("debug").executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return reply(ctx, false, "Solo un jugador puede activar el overlay.");
                    return reply(ctx, true, service().toggleDebug(player.getUUID()) ? "Overlay cognitivo activado." : "Overlay cognitivo desactivado.");
                }));
    }

    private static List<String> status() {
        CognitionEngine e = engine();
        List<String> out = new ArrayList<>();
        var c = e.metrics().snapshot();
        out.add("Capa cognitiva: " + (service().enabled() ? "activa" : "inactiva") + ", mentes cargadas=" + e.loadedCount() + ", tick de mente " + fmt(c.tickMicros()) + " µs, experiencia " + fmt(c.experienceMicros()) + " µs");
        out.add("Experiencias=" + c.experiences() + " descartadas=" + c.discarded() + " rumores iniciados=" + c.rumorsStarted() + " evoluciones de personalidad=" + c.evolutions() + " ecos=" + c.echoes());
        var m = e.memory().metrics().snapshot();
        out.add("Memoria: recuerdos=" + e.memory().totalMemories() + " creados=" + m.created() + " reforzados=" + m.reinforced() + " fusionados=" + m.merged() + " olvidados=" + m.forgotten() + " comprimidos=" + m.compressed()
                + " consolidados=" + m.consolidated() + " búsqueda " + fmt(m.retrievalMicros()) + " µs, caché " + Math.round(m.cacheHitRate() * 100) + "%, compresión x" + fmt(m.compressionRatio()));
        var r = e.relationships().metrics().snapshot();
        out.add("Relaciones: " + e.relationships().totalRelationships() + " (confianza media " + fmt(e.relationships().averageTrust()) + ", respeto medio " + fmt(e.relationships().averageRespect()) + ") promesas activas=" + e.relationships().activePromises()
                + " rotas=" + r.promisesBroken() + " grafo: " + e.relationships().graph().nodes() + " nodos/" + e.relationships().graph().edges() + " aristas");
        var em = e.emotions().metrics().snapshot();
        out.add("Emociones: activas=" + e.emotions().activeEmotions() + " traumas activos=" + e.emotions().activeTraumas() + " cambios de ánimo=" + em.moodChanges() + " recuperados=" + em.traumasRecovered() + " contagios=" + em.contagions()
                + " tiempo feliz=" + em.happyTicks() + " triste=" + em.sadTicks());
        var k = e.knowledge().metrics().snapshot();
        out.add("Conocimiento: registros=" + e.knowledge().totalRecords() + " aprendidos=" + k.learned() + " enseñados=" + k.taught() + " descubrimientos=" + k.discoveries() + " validados=" + k.validated() + " consulta " + fmt(k.queryMicros()) + " µs");
        out.add("Sociedad: comunidades=" + e.society().communityCount() + " miembros=" + e.society().totalMembers() + " rumores activos=" + e.society().activeRumors() + " (creados " + k.rumorsCreated() + ", propagados " + k.rumorsSpread()
                + ", confirmados " + k.rumorsConfirmed() + ", olvidados " + k.rumorsForgotten() + ") cola=" + e.society().queue().size() + " historia=" + e.society().worldTimeline().size());
        if (e.storage() != null) out.add("Persistencia: " + (e.storage().totalBytes() / 1024) + " KB en disco, guardados=" + c.saves() + " (" + fmt(c.saveMillis()) + " ms), cargas=" + c.loads() + " (" + fmt(c.loadMillis()) + " ms), recuperaciones=" + c.recoveries() + " fallos=" + (c.saveFailures() + c.loadFailures()));
        if (!e.metrics().lastError.isEmpty()) out.add("Último error: " + e.metrics().lastError);
        return out;
    }

    private static String fmt(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    private static int inspect(CommandContext<CommandSourceStack> ctx) {
        if (!ready(ctx)) return 0;
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return reply(ctx, false, "No hay ningún NPC con ese nombre.");
        Map<String, String> extras = new LinkedHashMap<>();
        extras.put("NPC", npc.get().getName());
        extras.put("Objetivo actual", npc.get().getCurrentGoal() == null ? "-" : npc.get().getCurrentGoal().getType().name());
        var advice = SchedulerService.getInstance().adviceFor(npc.get().getId());
        extras.put("Rutina", advice.map(a -> a.label() + " (" + a.reason() + ")").orElse("-"));
        var cognitive = service().adviceFor(npc.get().getId());
        extras.put("Consejo cognitivo", cognitive.map(a -> a.mood() + "/" + a.blend() + " velocidad x" + fmt(a.speedScale()) + " expresión " + a.expression() + (a.dangerNearby() > 0 ? " peligro conocido cerca " + fmt(a.dangerNearby()) : "")).orElse("-"));
        return lines(ctx, CognitiveInspector.inspect(engine(), npc.get().getId(), now(), extras));
    }

    private static List<String> trace(UUID npc) {
        List<String> out = new ArrayList<>();
        for (TraceStep s : engine().trace().recent(npc, 14)) out.add("[" + s.at() + "] " + s.stage() + " " + s.detail() + " (traza " + s.traceId().toString().substring(0, 8) + ")");
        return out;
    }

    private static int experience(CommandContext<CommandSourceStack> ctx, String actorName) {
        if (!ready(ctx)) return 0;
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return reply(ctx, false, "No hay ningún NPC con ese nombre.");
        Optional<ExperienceKind> kind = parse(ExperienceKind.class, StringArgumentType.getString(ctx, "kind"));
        if (kind.isEmpty()) return reply(ctx, false, "Tipo de experiencia desconocido.");
        EntityRef actor = null;
        if (actorName != null) { Optional<EntityRef> found = other(actorName); if (found.isEmpty()) return reply(ctx, false, "No conozco a " + actorName + "."); actor = found.get(); }
        else if (ctx.getSource().getEntity() instanceof ServerPlayer p) actor = EntityRef.player(p.getUUID(), p.getGameProfile().getName());
        CognitionOutcome out = service().experience(npc.get().getId(), ExperienceInput.of(npc.get().getId(), kind.get(), now()).actor(actor).place(service().engine().facts().placeOf(npc.get().getId())).source("command"));
        if (!out.kept()) return reply(ctx, true, "La experiencia no merecía recordarse (o falló): " + engine().metrics().lastError);
        StringBuilder sb = new StringBuilder("Recuerdo " + out.memory().result() + " " + out.memory().record().summary());
        if (out.emotion() != null) sb.append("; emociones=").append(out.emotion().records().stream().map(r -> r.kind() + ":" + Math.round(r.intensity())).toList());
        if (out.emotion() != null && out.emotion().trauma() != null) sb.append("; TRAUMA");
        if (out.relationship() != null) sb.append("; confianza ").append(Math.round(out.relationship().record().trust())).append(" (").append(out.relationship().trustAfter()).append(")");
        if (!out.knowledge().isEmpty()) sb.append("; aprendió ").append(out.knowledge().size());
        if (out.rumor() != null) sb.append("; rumor ").append(out.rumor().id().toString(), 0, 8);
        return reply(ctx, true, sb.toString());
    }

    private static int why(CommandContext<CommandSourceStack> ctx) {
        if (!ready(ctx)) return 0;
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return reply(ctx, false, "No hay ningún NPC con ese nombre.");
        String what = StringArgumentType.getString(ctx, "what").toLowerCase(Locale.ROOT), about = StringArgumentType.getString(ctx, "about").trim();
        UUID id = npc.get().getId();
        switch (what) {
            case "emotion" -> {
                Optional<EmotionKind> kind = EmotionKind.parse(about);
                return kind.isEmpty() ? reply(ctx, false, "Emoción desconocida.") : lines(ctx, engine().emotions().explain(id, kind.get()));
            }
            case "knowledge" -> {
                List<KnowledgeRecord> found = engine().knowledge().encyclopedia(id).lookup(about);
                if (found.isEmpty()) return reply(ctx, true, "No sabe nada de \"" + about + "\".");
                List<String> out = new ArrayList<>();
                for (KnowledgeRecord r : found.subList(0, Math.min(3, found.size()))) out.addAll(engine().knowledge().explain(id, r.id()));
                return lines(ctx, out);
            }
            default -> {
                Optional<Dimension> dim = parse(Dimension.class, what);
                Optional<EntityRef> target = other(about);
                if (dim.isEmpty() || target.isEmpty()) return reply(ctx, false, "Uso: why <npc> trust|respect|honor|affinity|rivalry|fear|loyalty <jugador o NPC>");
                return lines(ctx, engine().relationships().explain(id, target.get().id(), dim.get()));
            }
        }
    }

    // ------------------------------------------------------------------ memory

    private static List<MemoryRecord> sorted(UUID npc) {
        List<MemoryRecord> all = engine().memory().peek(npc).map(r -> r.all()).orElse(new ArrayList<>());
        all.sort((a, b) -> Long.compare(b.stamp().gameTime(), a.stamp().gameTime()));
        return all;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> memory(int permission) {
        return Commands.literal("memory").requires(s -> s.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, status().subList(2, 3))))
                .then(Commands.literal("metrics").then(Commands.literal("reset").executes(ctx -> { engine().memory().metrics().reset(); return reply(ctx, true, "Métricas de memoria a cero."); })))
                .then(Commands.literal("list").then(npcArg().executes(ctx -> memList(ctx, 12)).then(Commands.argument("limit", IntegerArgumentType.integer(1, 60)).executes(ctx -> memList(ctx, IntegerArgumentType.getInteger(ctx, "limit"))))))
                .then(Commands.literal("search").then(npcArg().then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> npc(ctx).map(n -> {
                    var mem = engine().memory().peek(n.getId());
                    if (mem.isEmpty()) return reply(ctx, true, "Sin memoria.");
                    List<String> out = new ArrayList<>();
                    for (MemoryRecord r : MemoryInspector.search(mem.get(), StringArgumentType.getString(ctx, "text"), 20)) out.add(r.summary());
                    return lines(ctx, out);
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))))
                .then(Commands.literal("show").then(npcArg().then(Commands.argument("index", IntegerArgumentType.integer(1, 5000)).executes(ctx -> npc(ctx).map(n -> {
                    List<MemoryRecord> all = sorted(n.getId());
                    int i = IntegerArgumentType.getInteger(ctx, "index");
                    return i > all.size() ? reply(ctx, false, "Solo hay " + all.size() + " recuerdos.") : lines(ctx, MemoryInspector.describe(all.get(i - 1), now()));
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))))
                .then(Commands.literal("forget").then(npcArg().then(Commands.argument("index", IntegerArgumentType.integer(1, 5000)).executes(ctx -> npc(ctx).map(n -> {
                    List<MemoryRecord> all = sorted(n.getId());
                    int i = IntegerArgumentType.getInteger(ctx, "index");
                    return i > all.size() ? reply(ctx, false, "No existe ese recuerdo.") : reply(ctx, engine().memory().remove(n.getId(), all.get(i - 1).id(), "administrador"), "Recuerdo eliminado.");
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))))
                .then(Commands.literal("protect").then(npcArg().then(Commands.argument("index", IntegerArgumentType.integer(1, 5000)).executes(ctx -> npc(ctx).map(n -> {
                    List<MemoryRecord> all = sorted(n.getId());
                    int i = IntegerArgumentType.getInteger(ctx, "index");
                    if (i > all.size()) return reply(ctx, false, "No existe ese recuerdo.");
                    engine().memory().protect(n.getId(), all.get(i - 1).id(), true);
                    return reply(ctx, true, "Recuerdo protegido.");
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))))
                .then(Commands.literal("graph").then(npcArg().executes(ctx -> npc(ctx).map(n -> lines(ctx, engine().memory().peek(n.getId()).map(r -> MemoryInspector.graph(r, 15)).orElse(List.of()))).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("index").then(npcArg().executes(ctx -> npc(ctx).map(n -> lines(ctx, engine().memory().peek(n.getId()).map(MemoryInspector::indexReport).orElse(List.of()))).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("export").then(npcArg().executes(CognitionCommand::exportMemory)));
    }

    private static int memList(CommandContext<CommandSourceStack> ctx, int limit) {
        if (!ready(ctx)) return 0;
        return npc(ctx).map(n -> {
            var mem = engine().memory().peek(n.getId());
            if (mem.isEmpty()) return reply(ctx, true, "Sin memoria cargada.");
            List<String> out = new ArrayList<>(MemoryInspector.timeline(mem.get(), limit, now()));
            var d = MemoryDiagnostics.snapshot(mem.get());
            out.add(0, "recuerdos=" + d.count() + " temporales=" + d.temporary() + " protegidos=" + d.protectedCount() + " comprimidos=" + d.compressed());
            return lines(ctx, out);
        }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."));
    }

    private static int exportMemory(CommandContext<CommandSourceStack> ctx) {
        if (!ready(ctx)) return 0;
        Optional<NPCRuntime> npc = npc(ctx);
        if (npc.isEmpty()) return reply(ctx, false, "No hay ningún NPC con ese nombre.");
        var mem = engine().memory().peek(npc.get().getId());
        if (mem.isEmpty()) return reply(ctx, false, "Sin memoria cargada.");
        try {
            Path dir = ctx.getSource().getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve("samuraiai").resolve("exports");
            Files.createDirectories(dir);
            Path file = dir.resolve(npc.get().getId() + "-cognitive-snapshot.json");
            Files.writeString(file, MemoryDiagnostics.export(mem.get()).toString(), StandardCharsets.UTF_8);
            return reply(ctx, true, "Snapshot cognitivo exportado a " + file);
        } catch (java.io.IOException | RuntimeException error) {
            return reply(ctx, false, "No se pudo exportar: " + error.getMessage());
        }
    }

    // ------------------------------------------------------------------ relationship

    private static List<RelationshipRecord> relationships(UUID npc) { return engine().relationships().relationships(npc); }

    public static LiteralArgumentBuilder<CommandSourceStack> relationship(int permission) {
        return Commands.literal("relationship").requires(s -> s.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, status().subList(3, 4))))
                .then(Commands.literal("list").then(npcArg().executes(ctx -> npc(ctx).map(n -> {
                    List<String> out = new ArrayList<>();
                    for (RelationshipRecord r : relationships(n.getId())) out.add(SocialInspector.line(r, engine().relationships().settings()));
                    return lines(ctx, out);
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("show").then(npcArg().then(otherArg("target").executes(ctx -> relationshipTarget(ctx, false)))))
                .then(Commands.literal("timeline").then(npcArg().then(otherArg("target").executes(ctx -> relationshipTarget(ctx, true)))))
                .then(Commands.literal("reputation").then(npcArg().then(otherArg("target").executes(ctx -> npc(ctx).flatMap(n -> other(StringArgumentType.getString(ctx, "target")).map(t ->
                        lines(ctx, SocialInspector.reputation(engine().relationships().reputationOf(n.getId(), t.id()))))).orElseGet(() -> reply(ctx, false, "NPC o persona desconocidos."))))))
                .then(Commands.literal("graph").then(otherArg("target").executes(ctx -> other(StringArgumentType.getString(ctx, "target")).map(t -> {
                    var g = engine().relationships().graph();
                    return lines(ctx, List.of("Quién la conoce: " + labels(g.whoKnows(t.id())), "Quién la respeta (>=60): " + labels(g.whoRespects(t.id(), 60)), "Quién confía (>=60): " + labels(g.whoTrusts(t.id(), 60)),
                            "Quién la teme (>=40): " + labels(g.whoFears(t.id(), 40))));
                }).orElseGet(() -> reply(ctx, false, "Persona desconocida.")))))
                .then(Commands.literal("promise").then(npcArg().then(otherArg("promiser").then(enumArg("kind", PromiseRecord.Kind.class).then(Commands.argument("subject", StringArgumentType.greedyString()).executes(CognitionCommand::promise))))))
                .then(Commands.literal("promises").then(npcArg().executes(ctx -> npc(ctx).map(n -> lines(ctx, SocialInspector.promises(engine().relationships().runtime(n.getId()).promises().values()))).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("fulfill").then(npcArg().then(Commands.argument("index", IntegerArgumentType.integer(1, 500)).executes(ctx -> resolvePromise(ctx, true)))))
                .then(Commands.literal("break").then(npcArg().then(Commands.argument("index", IntegerArgumentType.integer(1, 500)).executes(ctx -> resolvePromise(ctx, false)))));
    }

    private static String labels(List<UUID> ids) {
        List<String> names = new ArrayList<>();
        for (UUID id : ids) names.add(service().ref(id).label());
        return names.isEmpty() ? "nadie" : String.join(", ", names);
    }

    private static int relationshipTarget(CommandContext<CommandSourceStack> ctx, boolean timeline) {
        return npc(ctx).flatMap(n -> other(StringArgumentType.getString(ctx, "target")).map(t -> {
            var found = engine().relationships().find(n.getId(), t.id());
            if (found.isEmpty()) return reply(ctx, true, n.getName() + " no tiene relación con " + t.label() + ".");
            return lines(ctx, timeline ? SocialInspector.timeline(found.get(), 15) : SocialInspector.detail(found.get(), engine().relationships().settings(), now()));
        })).orElseGet(() -> reply(ctx, false, "NPC o persona desconocidos."));
    }

    private static int promise(CommandContext<CommandSourceStack> ctx) {
        if (!ready(ctx)) return 0;
        Optional<NPCRuntime> npc = npc(ctx);
        Optional<EntityRef> promiser = other(StringArgumentType.getString(ctx, "promiser"));
        Optional<PromiseRecord.Kind> kind = parse(PromiseRecord.Kind.class, StringArgumentType.getString(ctx, "kind"));
        if (npc.isEmpty() || promiser.isEmpty() || kind.isEmpty()) return reply(ctx, false, "NPC, persona o tipo desconocidos.");
        var p = engine().relationships().promise(npc.get().getId(), promiser.get(), EntityRef.npc(npc.get().getId(), npc.get().getName()), kind.get(), StringArgumentType.getString(ctx, "subject"), now(), 0, true, 0.8D, null);
        return reply(ctx, true, "Promesa " + p.kind() + " registrada: " + promiser.get().label() + " -> " + npc.get().getName() + ".");
    }

    private static int resolvePromise(CommandContext<CommandSourceStack> ctx, boolean fulfil) {
        if (!ready(ctx)) return 0;
        return npc(ctx).map(n -> {
            List<PromiseRecord> all = new ArrayList<>(engine().relationships().runtime(n.getId()).promises().values());
            int i = IntegerArgumentType.getInteger(ctx, "index");
            if (i > all.size()) return reply(ctx, false, "No existe esa promesa.");
            var outcome = fulfil ? engine().relationships().fulfill(n.getId(), all.get(i - 1).id(), now(), null) : engine().relationships().breakPromise(n.getId(), all.get(i - 1).id(), now(), null);
            if (outcome.isEmpty()) return reply(ctx, false, "Esa promesa ya estaba resuelta.");
            service().project(n);
            if (outcome.get().update() != null) service().projectRelationship(n.getId(), outcome.get().update().record().target().id());
            return reply(ctx, true, "Promesa " + (fulfil ? "cumplida" : "rota") + (outcome.get().update() == null ? "." : ": confianza " + Math.round(outcome.get().update().record().trust()) + ", honor " + Math.round(outcome.get().update().record().honor()) + "."));
        }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."));
    }

    // ------------------------------------------------------------------ emotion

    public static LiteralArgumentBuilder<CommandSourceStack> emotion(int permission) {
        return Commands.literal("emotion").requires(s -> s.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, status().subList(4, 5))))
                .then(Commands.literal("metrics").then(Commands.literal("reset").executes(ctx -> { engine().emotions().metrics().reset(); return reply(ctx, true, "Métricas de emoción a cero."); })))
                .then(Commands.literal("show").then(npcArg().executes(ctx -> npc(ctx).map(n -> {
                    var rt = engine().emotions().peek(n.getId());
                    if (rt.isEmpty()) return reply(ctx, true, "Sin emociones cargadas.");
                    List<String> out = new ArrayList<>(EmotionInspector.summary(rt.get(), now()));
                    out.addAll(EmotionInspector.chart(rt.get()));
                    out.addAll(EmotionInspector.traumas(rt.get()));
                    return lines(ctx, out);
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("history").then(npcArg().executes(ctx -> npc(ctx).map(n -> lines(ctx, engine().emotions().peek(n.getId()).map(r -> EmotionInspector.timeline(r, 15)).orElse(List.of()))).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("tone").then(npcArg().executes(ctx -> npc(ctx).map(n -> {
                    var e = engine().emotions();
                    var t = e.tone(n.getId());
                    var p = e.physiology(n.getId());
                    return lines(ctx, List.of("Expresión: " + e.expression(n.getId()), "Cuerpo: velocidad x" + fmt(p.speedScale()) + " giro x" + fmt(p.turnScale()) + " mirada x" + fmt(p.gazeScale()) + " retardo " + p.reactionDelayTicks() + " ticks postura " + fmt(p.posture()),
                            "Habla: formalidad " + fmt(t.formality()) + " verbosidad " + fmt(t.verbosity()) + " ritmo " + fmt(t.pace()) + " silencio " + fmt(t.silence()) + " preguntas " + fmt(t.questionRate()) + " calidez " + fmt(t.warmth()) + " voz " + t.voiceTone()));
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("trigger").then(npcArg().then(enumArg("kind", EmotionKind.class).then(Commands.argument("intensity", DoubleArgumentType.doubleArg(1, 100)).executes(ctx -> {
                    if (!ready(ctx)) return 0;
                    Optional<NPCRuntime> npc = npc(ctx);
                    Optional<EmotionKind> kind = parse(EmotionKind.class, StringArgumentType.getString(ctx, "kind"));
                    if (npc.isEmpty() || kind.isEmpty()) return reply(ctx, false, "NPC o emoción desconocidos.");
                    var r = engine().emotions().trigger(EmotionTrigger.simple(npc.get().getId(), TriggerSource.ADMIN, kind.get(), DoubleArgumentType.getDouble(ctx, "intensity"), now()));
                    service().project(npc.get());
                    return reply(ctx, true, "Emoción " + kind.get() + " a intensidad " + (r.any() ? Math.round(r.records().get(0).intensity()) : 0) + (r.trauma() != null ? " (trauma)" : "") + ".");
                })))))
                .then(Commands.literal("recover").then(npcArg().executes(ctx -> npc(ctx).map(n -> reply(ctx, true, "Recuperación aplicada a " + engine().emotions().recover(n.getId(), RecoverySource.CONVERSATION, now()) + " traumas.")).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("regulate").then(npcArg().then(enumArg("technique", Technique.class).executes(ctx -> npc(ctx).map(n -> {
                    Optional<Technique> t = parse(Technique.class, StringArgumentType.getString(ctx, "technique"));
                    if (t.isEmpty()) return reply(ctx, false, "Técnica desconocida.");
                    engine().emotions().regulate(n.getId(), t.get(), now(), 1200);
                    return reply(ctx, true, n.getName() + " aplica " + t.get() + " durante 1 minuto.");
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))));
    }

    // ------------------------------------------------------------------ knowledge

    public static LiteralArgumentBuilder<CommandSourceStack> knowledge(int permission) {
        return Commands.literal("knowledge").requires(s -> s.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, status().subList(5, 6))))
                .then(Commands.literal("metrics").then(Commands.literal("reset").executes(ctx -> { engine().knowledge().metrics().reset(); return reply(ctx, true, "Métricas de conocimiento a cero."); })))
                .then(Commands.literal("list").then(npcArg().executes(ctx -> npc(ctx).map(n -> {
                    var rt = engine().knowledge().peek(n.getId());
                    if (rt.isEmpty()) return reply(ctx, true, "Sin conocimiento cargado.");
                    List<String> out = new ArrayList<>(KnowledgeInspector.summary(rt.get()));
                    out.addAll(KnowledgeInspector.records(rt.get(), 20));
                    return lines(ctx, out);
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("places").then(npcArg().executes(ctx -> npc(ctx).map(n -> {
                    var rt = engine().knowledge().peek(n.getId());
                    List<String> out = new ArrayList<>();
                    if (rt.isPresent()) for (KnowledgeRecord r : yadi.samuraiai.ai.knowledge.places.PlaceKnowledge.known(rt.get()))
                        out.add(r.category() + " " + r.name() + " @" + (int) r.place().x() + "," + (int) r.place().y() + "," + (int) r.place().z() + " [" + r.state() + " " + Math.round(r.confidence() * 100) + "%]" + (r.source() == null ? "" : " de " + r.source().label()));
                    return lines(ctx, out);
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre.")))))
                .then(Commands.literal("ask").then(npcArg().then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> npc(ctx).map(n ->
                        lines(ctx, engine().knowledge().encyclopedia(n.getId()).describe(StringArgumentType.getString(ctx, "name")))).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))))
                .then(Commands.literal("graph").then(npcArg().then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> npc(ctx).map(n -> {
                    var found = engine().knowledge().encyclopedia(n.getId()).lookup(StringArgumentType.getString(ctx, "name"));
                    if (found.isEmpty()) return reply(ctx, true, "No conoce eso.");
                    return lines(ctx, KnowledgeInspector.graph(engine().knowledge().runtime(n.getId()), found.get(0).subject().id(), 20));
                }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))))
                .then(Commands.literal("teach").then(npcArg().then(Commands.argument("student", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b))
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(CognitionCommand::teach)))));
    }

    private static int teach(CommandContext<CommandSourceStack> ctx) {
        if (!ready(ctx)) return 0;
        Optional<NPCRuntime> teacher = npc(ctx), student = NPCManager.getInstance().findByName(StringArgumentType.getString(ctx, "student"));
        if (teacher.isEmpty() || student.isEmpty()) return reply(ctx, false, "NPC desconocido.");
        var found = engine().knowledge().encyclopedia(teacher.get().getId()).lookup(StringArgumentType.getString(ctx, "name"));
        if (found.isEmpty()) return reply(ctx, false, teacher.get().getName() + " no sabe nada de eso.");
        double respect = engine().relationships().find(student.get().getId(), teacher.get().getId()).map(r -> r.respect() / 100.0D).orElse(0.3D);
        double trust = engine().relationships().find(student.get().getId(), teacher.get().getId()).map(r -> r.trust() / 100.0D).orElse(0.35D);
        var result = engine().knowledge().teach(teacher.get().getId(), student.get().getId(), found.stream().map(KnowledgeRecord::id).limit(3).toList(), respect, trust, 0.9D,
                (s, level) -> level.visibleTo(engine().society().bestRank(s)), now(), null);
        return reply(ctx, result.learnedCount() > 0, "Lección: calidad " + fmt(result.quality()) + ", aprendidos " + result.learnedCount() + "/" + result.topics().size() + ".");
    }

    // ------------------------------------------------------------------ society

    public static LiteralArgumentBuilder<CommandSourceStack> society(int permission) {
        return Commands.literal("society").requires(s -> s.hasPermission(permission))
                .then(Commands.literal("status").executes(ctx -> lines(ctx, status().subList(6, 7))))
                .then(Commands.literal("communities").executes(ctx -> {
                    List<String> out = new ArrayList<>();
                    for (var c : engine().society().communities()) out.addAll(KnowledgeInspector.community(c));
                    return lines(ctx, out);
                }))
                .then(Commands.literal("create").then(Commands.argument("id", StringArgumentType.word()).then(enumArg("kind", CommunityKind.class)
                        .executes(ctx -> createCommunity(ctx, null)).then(Commands.argument("culture", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                                engine().society().cultures().all().stream().map(cu -> cu.id()).toList(), b)).executes(ctx -> createCommunity(ctx, StringArgumentType.getString(ctx, "culture")))))))
                .then(Commands.literal("join").then(npcArg().then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                        engine().society().communities().stream().map(cm -> cm.id()).toList(), b)).executes(ctx -> join(ctx, AccessLevel.MEMBERS))
                        .then(enumArg("rank", AccessLevel.class).executes(ctx -> join(ctx, parse(AccessLevel.class, StringArgumentType.getString(ctx, "rank")).orElse(AccessLevel.MEMBERS)))))))
                .then(Commands.literal("leave").then(npcArg().then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> npc(ctx).map(n ->
                        reply(ctx, engine().society().leave(n.getId(), StringArgumentType.getString(ctx, "id")), "Salió de la comunidad.")).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."))))))
                .then(Commands.literal("history").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> lines(ctx, KnowledgeInspector.history(engine().society().history(StringArgumentType.getString(ctx, "id")), 20)))))
                .then(Commands.literal("timeline").executes(ctx -> lines(ctx, KnowledgeInspector.history(engine().society().worldTimeline(), 25))))
                .then(Commands.literal("rumors").executes(ctx -> {
                    List<String> out = new ArrayList<>();
                    for (var r : engine().society().rumors()) { out.addAll(KnowledgeInspector.rumor(r)); if (out.size() > 50) break; }
                    return lines(ctx, out);
                }))
                .then(Commands.literal("legends").executes(ctx -> {
                    List<String> out = new ArrayList<>();
                    engine().society().worldMemory().heroes(8).forEach(l -> out.add("Héroe: " + l.subject().label() + " (" + fmt(l.heroism()) + ")"));
                    engine().society().worldMemory().traitors(8).forEach(l -> out.add("Traidor: " + l.subject().label() + " (" + fmt(l.treachery()) + ")"));
                    return lines(ctx, out);
                }))
                .then(Commands.literal("cultures").executes(ctx -> {
                    List<String> out = new ArrayList<>();
                    for (var c : engine().society().cultures().all()) {
                        out.add(c.id() + ": " + c.name() + " normas=" + c.norms());
                        c.traditions().forEach(t -> out.add("  " + t.name() + " " + t.kind() + " " + t.period() + "/" + t.zoneKind() + " fuerza " + fmt(t.strength())));
                    }
                    return lines(ctx, out);
                }));
    }

    private static int createCommunity(CommandContext<CommandSourceStack> ctx, String culture) {
        if (!ready(ctx)) return 0;
        Optional<CommunityKind> kind = parse(CommunityKind.class, StringArgumentType.getString(ctx, "kind"));
        if (kind.isEmpty()) return reply(ctx, false, "Tipo de comunidad desconocido.");
        var pos = ctx.getSource().getPosition();
        String dimension = ctx.getSource().getLevel().dimension().location().toString();
        try {
            var c = engine().society().create(StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "id"), kind.get(), culture, new yadi.samuraiai.ai.cognition.model.PlaceRef(dimension, pos.x, pos.y, pos.z, ""), 48.0D);
            return reply(ctx, true, "Comunidad " + c.id() + " (" + c.kind() + ", cultura " + c.cultureId() + ") creada en tu posición.");
        } catch (IllegalStateException error) {
            return reply(ctx, false, error.getMessage());
        }
    }

    private static int join(CommandContext<CommandSourceStack> ctx, AccessLevel rank) {
        if (!ready(ctx)) return 0;
        return npc(ctx).map(n -> {
            boolean ok = engine().society().join(n.getId(), StringArgumentType.getString(ctx, "id"), rank);
            return reply(ctx, ok || engine().society().communitiesOf(n.getId()).contains(StringArgumentType.getString(ctx, "id").toLowerCase(Locale.ROOT)), ok ? n.getName() + " ahora pertenece a la comunidad como " + rank + "." : "No existe la comunidad, o ya pertenecía a ella.");
        }).orElseGet(() -> reply(ctx, false, "No hay ningún NPC con ese nombre."));
    }
}
