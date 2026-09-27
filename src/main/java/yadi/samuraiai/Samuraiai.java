package yadi.samuraiai;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import yadi.samuraiai.ai.AIRequestQueue;
import yadi.samuraiai.config.SamuraiForgeConfig;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.integration.IntegrationLoader;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.memory.MemoryManager;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.npc.lifecycle.NPCLifecycleManager;
import yadi.samuraiai.npc.relationship.RelationshipService;
import yadi.samuraiai.runtime.*;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.foundation.FoundationBootstrap;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

@Mod(Samuraiai.MODID)
public final class Samuraiai {
    public static final String MODID = "samuraiai";
    public Samuraiai(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SamuraiForgeConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.navigation.world.NavigationConfig.SPEC, "samuraiai-navigation.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.perception.world.PerceptionConfig.SPEC, "samuraiai-perception.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.scheduler.world.SchedulerConfig.SPEC, "samuraiai-scheduler.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.cognition.world.CognitionConfig.COGNITION_SPEC, "samuraiai-cognition.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.cognition.world.CognitionConfig.MEMORY_SPEC, "samuraiai-memory.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.cognition.world.CognitionConfig.RELATIONSHIP_SPEC, "samuraiai-relationship.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.cognition.world.CognitionConfig.EMOTION_SPEC, "samuraiai-emotion.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.ai.cognition.world.CognitionConfig.KNOWLEDGE_SPEC, "samuraiai-knowledge.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.living.server.LivingConfig.LIVING_SPEC, "samuraiai-living.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.living.server.LivingConfig.CALENDAR_SPEC, "samuraiai-calendar.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.living.server.LivingConfig.WORLD_SPEC, "samuraiai-world.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.living.server.LivingConfig.VILLAGE_SPEC, "samuraiai-village.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.living.server.LivingConfig.ECONOMY_SPEC, "samuraiai-economy.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.living.server.LivingConfig.QUEST_SPEC, "samuraiai-quest.toml");
        context.registerConfig(ModConfig.Type.COMMON, yadi.samuraiai.living.server.LivingConfig.FAMILY_SPEC, "samuraiai-family.toml");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> yadi.samuraiai.client.ClientBootstrap.register(context));
        context.getModEventBus().addListener(SamuraiForgeConfig::onLoad);
        context.getModEventBus().addListener(SamuraiForgeConfig::onReload);
        context.getModEventBus().addListener(yadi.samuraiai.ai.navigation.world.NavigationConfig::onLoad);
        context.getModEventBus().addListener(yadi.samuraiai.ai.navigation.world.NavigationConfig::onReload);
        context.getModEventBus().addListener(yadi.samuraiai.ai.perception.world.PerceptionConfig::onLoad);
        context.getModEventBus().addListener(yadi.samuraiai.ai.perception.world.PerceptionConfig::onReload);
        context.getModEventBus().addListener(yadi.samuraiai.ai.scheduler.world.SchedulerConfig::onLoad);
        context.getModEventBus().addListener(yadi.samuraiai.ai.scheduler.world.SchedulerConfig::onReload);
        context.getModEventBus().addListener(yadi.samuraiai.ai.cognition.world.CognitionConfig::onLoad);
        context.getModEventBus().addListener(yadi.samuraiai.ai.cognition.world.CognitionConfig::onReload);
        context.getModEventBus().addListener(yadi.samuraiai.living.server.LivingConfig::onLoad);
        context.getModEventBus().addListener(yadi.samuraiai.living.server.LivingConfig::onReload);
        context.getModEventBus().addListener(FoundationBootstrap::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent public void starting(ServerStartingEvent event) {
        if (!FoundationBootstrap.allowed("core")) {
            SamuraiLogger.CORE.error("Foundation BLOCKER: SamuraiAI runtime will not start; consult config/samuraiai/foundation");
            return;
        }
        var server = event.getServer();
        ServerScheduler.getInstance().bind(server::execute, server::isSameThread);
        AIRequestQueue.getInstance().resume();
        NPCSpawnService.getInstance().setController(IntegrationLoader.createController());
        RuntimeEvents.install();
        yadi.samuraiai.ai.perception.world.PerceptionService.getInstance().install();
        yadi.samuraiai.emotion.PerceptionEmotionBridge.install();
        int zones = yadi.samuraiai.ai.scheduler.world.ZoneStore.load(server, yadi.samuraiai.ai.scheduler.world.SchedulerService.getInstance().scheduler().zoneRegistry());
        SamuraiLogger.CORE.info("Loaded {} scheduler zones", zones);
        yadi.samuraiai.ai.cognition.world.CognitionService.getInstance().install(server);
        try { yadi.samuraiai.living.server.LivingService.getInstance().install(server); }
        catch (RuntimeException error) { SamuraiLogger.CORE.error("Living world could not start; the rest of SamuraiAI keeps running", error); }
        var persistence = yadi.samuraiai.npc.persistence.PersistenceBootstrap.getInstance();
        var snapshots = persistence.loadWorld(server);
        int restored = 0;
        for (var snapshot : snapshots) {
            if (NPCSpawnService.getInstance().restore(snapshot).success()) { persistence.forget(snapshot.id(), server); restored++; }
        }
        SamuraiLogger.PERSISTENCE.info("Loaded {} persisted NPC snapshots; restored {}", snapshots.size(), restored);
        var delivery = ServerScheduler.getInstance().executor();
        new yadi.samuraiai.ollama.OllamaDiagnostics().check().thenAccept(result -> delivery.execute(() ->
                SamuraiLogger.AI.info("startup ollama={} model={} detail={}", result.status(), result.model(), result.detail())));
        SamuraiLogger.CORE.info("SamuraiAI server session started");
    }
    @SubscribeEvent public void stopping(ServerStoppingEvent event) {
        if (!ServerScheduler.getInstance().isRunning()) return;
        yadi.samuraiai.npc.persistence.PersistenceBootstrap.getInstance().saveWorld(event.getServer());
        NPCSpawnService.getInstance().removeAll("server stopped", false);
        yadi.samuraiai.living.server.LivingService.getInstance().shutdown();
        yadi.samuraiai.ai.cognition.world.CognitionService.getInstance().shutdown();
        yadi.samuraiai.ai.navigation.world.NavigationService.getInstance().reset();
        yadi.samuraiai.ai.perception.world.PerceptionService.getInstance().reset();
        yadi.samuraiai.ai.scheduler.world.ZoneStore.save(event.getServer(), yadi.samuraiai.ai.scheduler.world.SchedulerService.getInstance().scheduler().zoneRegistry());
        yadi.samuraiai.ai.scheduler.world.SchedulerService.getInstance().reset();
        DialogueService.getInstance().clear();
        AIRequestQueue.getInstance().pause();
        NPCSpawnService.getInstance().close();
        NPCManager.getInstance().clear();
        MemoryManager.getInstance().clearAll();
        RelationshipService.getInstance().clear();
        NPCLifecycleManager.getInstance().clear();
        NPCEventBus.getInstance().clear();
        yadi.samuraiai.npc.persistence.PersistenceBootstrap.getInstance().clear();
        DialogueRouter.reset(); NPCTickService.reset();
        ServerScheduler.getInstance().close();
        SamuraiLogger.CORE.info("SamuraiAI server session stopped");
    }
}
