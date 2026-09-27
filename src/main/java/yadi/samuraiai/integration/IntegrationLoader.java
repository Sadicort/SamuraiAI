package yadi.samuraiai.integration;

import net.minecraftforge.fml.ModList;
import yadi.samuraiai.controller.*;
import yadi.samuraiai.logging.SamuraiLogger;

/** Core only knows the controller contract. Optional implementation is loaded by name. */
public final class IntegrationLoader {
    public record Compatibility(boolean installed, String version, boolean compatible, String detail) {}
    private static Compatibility status = new Compatibility(false, "", true, "Not checked");
    public static Compatibility status() { return status; }
    public static boolean supports(String version) { return "1.19.2.20250701".equals(version); }
    public static NPCController createController() {
        if (!yadi.samuraiai.foundation.FoundationBootstrap.allowed("customnpcs")) {
            status = new Compatibility(false, "", false, "Foundation disabled the optional adapter");
            SamuraiLogger.CUSTOM_NPCS.warn("Foundation disabled CustomNPCs integration; chat backend selected");
            return new ChatNPCController();
        }
        var mod = ModList.get().getModContainerById("customnpcs");
        if (mod.isEmpty()) {
            status = new Compatibility(false, "", true, "Chat-only backend");
            SamuraiLogger.CUSTOM_NPCS.info("CustomNPCs absent; chat backend selected");
            return new ChatNPCController();
        }
        String version = mod.get().getModInfo().getVersion().toString();
        if (!supports(version)) {
            status = new Compatibility(true, version, false, "Untested version; chat backend");
            SamuraiLogger.CUSTOM_NPCS.warn("CustomNPCs version={} unsupported by adapter", version);
            return new ChatNPCController();
        }
        try {
            NPCController controller = (NPCController) Class.forName(
                    "yadi.samuraiai.integration.customnpcs.CustomNPCsController").getConstructor().newInstance();
            status = new Compatibility(true, version, true, "API ready");
            SamuraiLogger.CUSTOM_NPCS.info("CustomNPCs version={} adapter ready", version);
            return controller;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            status = new Compatibility(true, version, false, "API unavailable");
            SamuraiLogger.CUSTOM_NPCS.error("CustomNPCs API unavailable; chat backend selected", error);
            return new ChatNPCController();
        }
    }
    private IntegrationLoader() {}
}
