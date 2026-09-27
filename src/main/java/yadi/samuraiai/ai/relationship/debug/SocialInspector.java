package yadi.samuraiai.ai.relationship.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import yadi.samuraiai.ai.relationship.affinity.AffinityEngine;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.factions.FactionRelations;
import yadi.samuraiai.ai.relationship.honor.HonorSystem;
import yadi.samuraiai.ai.relationship.model.PromiseRecord;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.ReputationRecord;
import yadi.samuraiai.ai.relationship.respect.RespectEngine;
import yadi.samuraiai.ai.relationship.rivalry.RivalryEngine;
import yadi.samuraiai.ai.relationship.trust.TrustEngine;
import yadi.samuraiai.ai.cognition.model.EntityKind;

/** Text views over relationships for the debugger: one line per relationship, the full detail of one, its timeline, promises and reputation. */
public final class SocialInspector {
    private SocialInspector() { }

    public static String line(RelationshipRecord r, RelationshipSettings s) {
        if (r.target().kind() == EntityKind.FACTION)
            return r.target().label() + " [facción] " + FactionRelations.standing(r, s) + " puntuación=" + Math.round(FactionRelations.score(r));
        return String.format(Locale.ROOT, "%s [%s/%s/%s] confianza=%.0f(%s) respeto=%.0f(%s) honor=%.0f(%s) afinidad=%.0f miedo=%.0f lealtad=%.0f rivalidad=%.0f(%s) amistad=%s",
                r.target().label(), r.target().kind(), r.type(), r.state(), r.trust(), TrustEngine.level(r, s), r.respect(), RespectEngine.level(r, s), r.honor(), HonorSystem.category(r, s),
                r.affinity(), r.fear(), r.loyalty(), r.rivalry(), RivalryEngine.level(r, s), r.stage());
    }

    public static List<String> detail(RelationshipRecord r, RelationshipSettings s, long now) {
        List<String> lines = new ArrayList<>();
        lines.add(line(r, s));
        lines.add("  afinidad=" + AffinityEngine.level(r, s) + " interacciones=" + r.interactions() + " (+" + r.positive() + "/-" + r.negative() + ") juramentos cumplidos=" + r.oathsKept() + " rotos=" + r.oathsBroken()
                + " lealtad " + r.loyaltyKind() + (r.loyaltyBroken() ? " ROTA" : "") + " version=" + r.version());
        lines.add("  recuerdos asociados=" + r.memories().size() + " eventos=" + r.events().size());
        return lines;
    }

    public static List<String> timeline(RelationshipRecord r, int limit) {
        List<String> lines = new ArrayList<>();
        var history = r.history();
        for (int i = history.size() - 1; i >= 0 && lines.size() < limit; i--) {
            var h = history.get(i);
            lines.add("t=" + h.at() + " " + h.kind() + String.format(Locale.ROOT, " confianza%+.1f respeto%+.1f honor%+.1f", h.trust(), h.respect(), h.honor()) + (h.note().isEmpty() ? "" : " (" + h.note() + ")"));
        }
        return lines;
    }

    public static List<String> promises(java.util.Collection<PromiseRecord> promises) {
        List<String> lines = new ArrayList<>();
        for (PromiseRecord p : promises) lines.add(p.status() + " " + p.kind() + " " + p.promiser().label() + " -> " + p.promisee().label() + (p.subject().isEmpty() ? "" : " (" + p.subject() + ")") + (p.publicPromise() ? " [pública]" : ""));
        return lines;
    }

    public static List<String> reputation(java.util.Collection<ReputationRecord> records) {
        List<String> lines = new ArrayList<>();
        for (ReputationRecord r : records) lines.add(r.subject().label() + " en " + r.scope() + (r.scopeId().isEmpty() ? "" : ":" + r.scopeId()) + " = " + r.dominant() + String.format(Locale.ROOT, " (confianza %.2f, directo %d, rumores %d)", r.confidence(), r.direct(), r.hearsay()));
        return lines;
    }
}
