package yadi.samuraiai.foundation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.LoggerFactory;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.foundation.audit.*;
import yadi.samuraiai.foundation.async.AsyncEngine;
import yadi.samuraiai.foundation.module.*;
import yadi.samuraiai.foundation.resource.FoundationResourceManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Common startup checks on Forge's setup worker, before client/server runtime initialization. */
public final class FoundationBootstrap {
    private static volatile AuditSummary summary;
    private static volatile ModuleLoader modules;
    private static volatile FoundationResourceManager resources;
    public static void setup(FMLCommonSetupEvent event) {
        Map<String, String> versions = new LinkedHashMap<>();
        FMLLoader.getLoadingModList().getMods().forEach(mod -> versions.put(mod.getModId(), mod.getVersion().toString()));
        var config = SamuraiSettings.snapshot();
        Map<String, String> environment = new LinkedHashMap<>(Map.of(
                "java.feature", Integer.toString(Runtime.version().feature()),
                "java.version", System.getProperty("java.version", "unknown"),
                "os", System.getProperty("os.name", "unknown"),
                "architecture", System.getProperty("os.arch", "unknown"),
                "processors", Integer.toString(Runtime.getRuntime().availableProcessors()),
                "maxHeapBytes", Long.toString(Runtime.getRuntime().maxMemory()),
                "auditThread", Thread.currentThread().getName(),
                "queue.size", Integer.toString(config.queueSize()),
                "queue.concurrency", Integer.toString(config.maxConcurrentRequests()),
                "brain.tickInterval", Integer.toString(config.brainTickInterval())));
        var side = FMLLoader.getDist() == Dist.CLIENT ? AuditContext.Side.CLIENT : AuditContext.Side.DEDICATED_SERVER;
        environment.put("async.workers", Integer.toString(Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() / 2))));
        environment.put("async.capacity", "256");
        try {
            resources = new FoundationResourceManager(FMLPaths.CONFIGDIR.get().resolve("samuraiai"));
            resources.prepare();
            environment.put("resources.status", "READY");
        } catch (java.io.IOException error) {
            environment.put("resources.status", "FAILED:" + error.getClass().getSimpleName());
        }
        try {
            modules = FoundationModules.load(side, versions, environment);
            environment.put("modules.status", "READY");
            environment.put("modules.ready", modules.snapshot().stream()
                    .filter(module -> module.state() == ModuleState.READY).map(ModuleSnapshot::id).sorted().collect(Collectors.joining(",")));
        } catch (RuntimeException error) {
            environment.put("modules.status", "FAILED:" + error.getClass().getSimpleName());
            environment.put("modules.ready", modules == null ? "" : modules.snapshot().stream()
                    .filter(module -> module.state() == ModuleState.READY).map(ModuleSnapshot::id).sorted().collect(Collectors.joining(",")));
        }
        AsyncEngine.global();
        summary = new FoundationAuditEngine().run(new AuditContext(side, versions, environment, FoundationBootstrap.class.getClassLoader()));
        var logger = LoggerFactory.getLogger("SamuraiAI/Foundation");
        logger.info("startup audit status={} startupAllowed={} disabledModules={} checks={}",
                summary.foundation(), summary.startupAllowed(), summary.disabledModules(), summary.results().size());
        try {
            var reportDirectory = FMLPaths.CONFIGDIR.get().resolve("samuraiai/foundation");
            new AuditReporter().write(summary, reportDirectory);
            new yadi.samuraiai.foundation.thread.ThreadReportWriter().write(AsyncEngine.global(), reportDirectory);
        }
        catch (java.io.IOException error) { logger.error("Cannot write Foundation startup report", error); }
    }
    public static AuditSummary summary() { return summary; }
    public static java.util.List<ModuleSnapshot> modules() { return modules == null ? java.util.List.of() : modules.snapshot(); }
    public static FoundationResourceManager resources() { return resources; }
    public static boolean allowed(String module) {
        var report = summary;
        if (report == null || !report.startupAllowed() || report.disabledModules().contains(module)) return false;
        ModuleLoader loader = modules;
        return loader == null || loader.find(module).map(snapshot -> snapshot.state() == ModuleState.READY).orElse(true);
    }
    private FoundationBootstrap() { }
}
