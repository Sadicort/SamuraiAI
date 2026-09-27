package yadi.samuraiai.living.quest.debug;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.living.quest.campaigns.Campaign;
import yadi.samuraiai.living.quest.engine.QuestEngine;
import yadi.samuraiai.living.quest.memory.QuestHistory;
import yadi.samuraiai.living.quest.metrics.QuestMetrics;
import yadi.samuraiai.living.quest.objectives.QuestObjective;
import yadi.samuraiai.living.quest.runtime.Quest;

/** The Quest Inspector: origin, cause, template, variables, objectives, decisions, consequences, related NPCs and world conditions of a quest. */
public final class QuestInspector {
    private QuestInspector() { }

    public static List<String> list(QuestEngine e) {
        List<String> out = new ArrayList<>();
        for (Quest q : e.quests())
            out.add(String.format("%s %s [%s/%s] de %s — %s", q.id().toString().substring(0, 8), q.title(), q.state(), q.stage(), q.giverName(), q.originKind()));
        if (out.isEmpty()) out.add("No hay misiones.");
        return out;
    }

    public static List<String> inspect(QuestEngine e, Quest q) {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s «%s» — plantilla %s (%s), estado %s, etapa %s", q.id(), q.title(), q.template(), q.category(), q.state(), q.stage()));
        out.add(String.format("Origen: %s [%s] gravedad %.2f, causa %s", q.originKind(), q.originKey(), q.severity(), q.cause() == null ? "-" : q.cause().label()));
        out.add(String.format("Quién la da: %s (%s); jugadores: %s; camino: %s", q.giverName(), q.giverProfession(), q.players().values(), q.path() == null ? "sin elegir" : q.path()));
        out.add("Texto: " + q.text());
        for (QuestObjective o : q.objectives())
            out.add(String.format("  [%s] %s %s — %s %.0f/%.0f%s%s", o.state(), o.type(), o.optional() ? "(opcional)" : "", o.description(), o.progress(), o.required(),
                    o.branch() == null ? "" : " vía " + o.branch(), o.placed() ? String.format(" en %.0f,%.0f r=%.0f", o.x(), o.z(), o.radius()) : ""));
        out.add("Variables: " + q.variables());
        if (!q.decisions().isEmpty()) out.add("Decisiones: " + q.decisions().stream().map(Quest.Decision::choice).toList());
        if (!q.given().isEmpty()) out.add("Recompensas dadas: " + q.given());
        if (!q.applied().isEmpty()) out.add("Consecuencias aplicadas: " + q.applied());
        out.add(String.format("Ofrecida hasta %s, aceptada %s, vence %s", e.clock().date(q.offeredUntil()).shortDate(), q.acceptedAt() == 0 ? "-" : e.clock().date(q.acceptedAt()).shortDate(),
                q.deadline() == 0 ? "-" : e.clock().date(q.deadline()).shortDate()));
        if (q.campaign() != null) out.add("Campaña " + q.campaign().toString().substring(0, 8) + ", etapa " + q.campaignStage());
        if (!q.log().isEmpty()) out.add("Historial: " + String.join(" | ", q.log()));
        return out;
    }

    public static List<String> campaigns(QuestEngine e) {
        List<String> out = new ArrayList<>();
        for (Campaign c : e.campaigns()) out.add(String.format("%s «%s» %s etapa %d/%d: %s", c.type(), c.title(), c.state(), c.stage() + 1, c.stages().size(), c.currentTemplate()));
        if (out.isEmpty()) out.add("No hay campañas.");
        return out;
    }

    public static List<String> history(QuestEngine e, java.util.UUID player) {
        List<String> out = new ArrayList<>();
        for (QuestHistory.Entry h : player == null ? e.history().world(20) : e.history().of(player))
            out.add(String.format("%s %s — %s%s", e.clock().date(h.minute()).shortDate(), h.title(), h.outcome(), h.path().isEmpty() ? "" : " (vía " + h.path() + ")"));
        if (out.isEmpty()) out.add("Sin historial.");
        return out;
    }

    public static List<String> metrics(QuestEngine e) {
        QuestMetrics.Snapshot m = e.metrics().snapshot();
        return List.of(
                String.format("Condiciones %d (ignoradas %d), generadas %d, aceptadas %d, completadas %d, fallidas %d, caducadas %d, abandonadas %d, resueltas por el mundo %d",
                        m.conditions(), m.ignored(), m.generated(), m.accepted(), m.completed(), m.failed(), m.expired(), m.abandoned(), m.resolvedByWorld()),
                String.format("Giros %d, fusiones %d, campañas %d, cadenas %d, consecuencias %d, recompensas %d, rechazos por desconfianza %d, duración media %.1f días; caminos %s",
                        m.twists(), m.merges(), m.campaigns(), m.chains(), m.consequences(), m.rewards(), m.refusals(), m.averageDays(), m.paths()));
    }
}
