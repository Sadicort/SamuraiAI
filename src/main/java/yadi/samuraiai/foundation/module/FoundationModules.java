package yadi.samuraiai.foundation.module;

import java.util.*;
import yadi.samuraiai.foundation.audit.AuditContext;

public final class FoundationModules {
    private static final String VERSION = "1.7";
    public static ModuleLoader load(AuditContext.Side side, Map<String, String> versions,
                                    Map<String, String> environment) {
        ModuleLoader loader = new ModuleLoader(new ModuleContext(side, versions, environment));
        register(loader, descriptor("common", ModuleSide.COMMON, 100), List.of());
        register(loader, descriptor("client", ModuleSide.CLIENT, 95), List.of(ModuleDependency.required("common")));
        register(loader, descriptor("brain", ModuleSide.COMMON, 90), List.of(ModuleDependency.required("common")));
        register(loader, descriptor("dialogue", ModuleSide.COMMON, 80), List.of(ModuleDependency.required("brain")));
        register(loader, descriptor("ollama", ModuleSide.COMMON, 70), List.of(ModuleDependency.required("common")));
        register(loader, descriptor("voice", ModuleSide.CLIENT, 60), List.of(ModuleDependency.required("client")));
        register(loader, descriptor("debug", ModuleSide.COMMON, 20), List.of(ModuleDependency.required("common")));
        register(loader, descriptor("developer_tools", ModuleSide.COMMON, 10), List.of(ModuleDependency.required("debug")));
        if (versions.containsKey("customnpcs"))
            register(loader, descriptor("customnpcs", ModuleSide.COMMON, 50), List.of(ModuleDependency.required("common")));
        loader.loadAll();
        return loader;
    }
    private static ModuleDescriptor descriptor(String id, ModuleSide side, int priority) {
        return new ModuleDescriptor(id, humanize(id), VERSION, "SamuraiAI", List.of(), side, priority);
    }
    private static void register(ModuleLoader loader, ModuleDescriptor base, List<ModuleDependency> dependencies) {
        loader.register(new ModuleDescriptor(base.id(), base.name(), base.version(), base.author(), dependencies,
                base.side(), base.priority()), ignored -> () -> { });
    }
    private static String humanize(String id) {
        return Arrays.stream(id.split("_"))
                .map(value -> Character.toUpperCase(value.charAt(0)) + value.substring(1)).reduce((a,b) -> a + " " + b).orElse(id);
    }
    private FoundationModules() { }
}
