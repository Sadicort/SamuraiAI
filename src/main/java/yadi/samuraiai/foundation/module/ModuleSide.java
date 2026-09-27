package yadi.samuraiai.foundation.module;

import yadi.samuraiai.foundation.audit.AuditContext;

public enum ModuleSide {
    COMMON, CLIENT, DEDICATED_SERVER;

    public boolean supports(AuditContext.Side side) {
        return this == COMMON || this == CLIENT && side == AuditContext.Side.CLIENT ||
                this == DEDICATED_SERVER && side == AuditContext.Side.DEDICATED_SERVER;
    }
}
