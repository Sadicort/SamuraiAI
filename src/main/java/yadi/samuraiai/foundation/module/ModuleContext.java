package yadi.samuraiai.foundation.module;

import java.util.Map;
import yadi.samuraiai.foundation.audit.AuditContext;

public record ModuleContext(AuditContext.Side side, Map<String, String> versions,
                            Map<String, String> environment) {
    public ModuleContext {
        versions = Map.copyOf(versions);
        environment = Map.copyOf(environment);
    }
}
