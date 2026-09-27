package yadi.samuraiai.living.quest.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.quest.branching.BranchSpec;
import yadi.samuraiai.living.quest.branching.Path;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.consequences.ConsequenceSpec;
import yadi.samuraiai.living.quest.objectives.QuestObjective;
import yadi.samuraiai.living.quest.rewards.RewardSpec;
import yadi.samuraiai.living.quest.templates.QuestTemplate;

/**
 * The Quest Runtime: one quest from offer to history. It knows why it exists ({@link #cause()}, the condition key it came
 * from), who offers it and where, its story stage and state, its objectives, the paths it allows and the one chosen, its
 * rewards and consequences, its variables, when it was offered, accepted and must end, the players on it, the decisions taken
 * and the consequences applied, and whether it is part of a chain or a campaign.
 */
public final class Quest {
    public enum State { OFFERED, ACTIVE, COMPLETED, FAILED, EXPIRED, ABANDONED, RESOLVED_BY_WORLD, MERGED }
    public record Decision(long minute, UUID player, String choice) { }

    private final UUID id;
    private final String template;
    private final QuestTemplate.Category category;
    private String title;
    private final Map<QuestTemplate.StoryStage, String> story = new LinkedHashMap<>();
    private QuestTemplate.StoryStage stage = QuestTemplate.StoryStage.PROLOGUE;
    private State state = State.OFFERED;
    private final ConditionKind originKind;
    private final String originKey;
    private double severity;
    private final Provenance cause;
    private final UUID giver, settlement, region;
    private final String giverName, giverProfession;
    private final List<QuestObjective> objectives = new ArrayList<>();
    private final List<BranchSpec> branches = new ArrayList<>();
    private Path path;
    private final List<RewardSpec> rewards = new ArrayList<>();
    private final List<ConsequenceSpec> consequences = new ArrayList<>();
    private final Map<String, String> variables = new LinkedHashMap<>();
    private final long created;
    private long offeredUntil, acceptedAt, deadline, endedAt;
    private final int durationDays;
    private final Map<UUID, String> players = new LinkedHashMap<>();
    private final List<Decision> decisions = new ArrayList<>();
    private final List<String> applied = new ArrayList<>(), given = new ArrayList<>(), log = new ArrayList<>();
    private UUID campaign, parent, reservedFor;
    private int campaignStage = -1, merged;
    private final Set<String> tags = new LinkedHashSet<>();

    public Quest(UUID id, String template, QuestTemplate.Category category, String title, ConditionKind originKind, String originKey, double severity, Provenance cause,
                 UUID giver, String giverName, String giverProfession, UUID settlement, UUID region, long created, long offeredUntil, int durationDays) {
        this.id = id; this.template = template; this.category = category; this.title = title; this.originKind = originKind; this.originKey = originKey; this.severity = severity;
        this.cause = cause; this.giver = giver; this.giverName = giverName == null ? "" : giverName; this.giverProfession = giverProfession == null ? "" : giverProfession;
        this.settlement = settlement; this.region = region; this.created = created; this.offeredUntil = offeredUntil; this.durationDays = Math.max(1, durationDays);
    }

    public UUID id() { return id; }
    public String template() { return template; }
    public QuestTemplate.Category category() { return category; }
    public String title() { return title; }
    public void title(String t) { title = t; }
    public Map<QuestTemplate.StoryStage, String> story() { return story; }
    public String text() { String t = story.get(stage); return t == null || t.isEmpty() ? story.getOrDefault(QuestTemplate.StoryStage.DEVELOPMENT, "") : t; }
    public QuestTemplate.StoryStage stage() { return stage; }
    public void stage(QuestTemplate.StoryStage s) { stage = s; log("etapa " + s); }
    public State state() { return state; }
    public void state(State s) { state = s; log("estado " + s); }
    public boolean open() { return state == State.OFFERED || state == State.ACTIVE; }
    public ConditionKind originKind() { return originKind; }
    public String originKey() { return originKey; }
    public double severity() { return severity; }
    public void severity(double v) { severity = v; }
    public Provenance cause() { return cause; }
    public UUID giver() { return giver; }
    public String giverName() { return giverName; }
    public String giverProfession() { return giverProfession; }
    public UUID settlement() { return settlement; }
    public UUID region() { return region; }
    public List<QuestObjective> objectives() { return objectives; }
    public List<BranchSpec> branches() { return branches; }
    public Path path() { return path; }
    public void path(Path p) { path = p; }
    public List<RewardSpec> rewards() { return rewards; }
    public List<ConsequenceSpec> consequences() { return consequences; }
    public Map<String, String> variables() { return variables; }
    public long created() { return created; }
    public long offeredUntil() { return offeredUntil; }
    public long acceptedAt() { return acceptedAt; }
    public long deadline() { return deadline; }
    public long endedAt() { return endedAt; }
    public int durationDays() { return durationDays; }
    public void accepted(long at, int minutesPerDay) { acceptedAt = at; deadline = at + (long) durationDays * minutesPerDay; }
    public void ended(long at) { endedAt = at; }
    public void extendDeadline(long minutes) { deadline += minutes; }
    public Map<UUID, String> players() { return players; }
    public List<Decision> decisions() { return decisions; }
    public List<String> applied() { return applied; }
    public List<String> given() { return given; }
    public List<String> log() { return log; }
    public void log(String line) { log.add(line); while (log.size() > 40) log.remove(0); }
    public UUID campaign() { return campaign; }
    public int campaignStage() { return campaignStage; }
    public void campaign(UUID c, int stage) { campaign = c; campaignStage = stage; }
    public UUID parent() { return parent; }
    public void parent(UUID p) { parent = p; }
    public UUID reservedFor() { return reservedFor; }
    public void reservedFor(UUID p) { reservedFor = p; }
    public int merged() { return merged; }
    public void mergedOnce() { merged++; }
    public Set<String> tags() { return tags; }

    /** Objectives that count now: common ones and those of the chosen path. */
    public List<QuestObjective> relevant() {
        List<QuestObjective> out = new ArrayList<>();
        for (QuestObjective o : objectives) if (o.branch() == null || o.branch() == path) out.add(o);
        return out;
    }

    public boolean requiredDone() {
        boolean any = false;
        for (QuestObjective o : relevant()) { if (o.optional()) continue; any = true; if (!o.done()) return false; }
        return any;
    }

    public double progressShare() {
        double total = 0, done = 0;
        for (QuestObjective o : relevant()) { if (o.optional()) continue; total += 1; done += o.progress() / o.required(); }
        return total == 0 ? 0 : done / total;
    }

    public void restoreTimes(long until, long accepted, long dl, long ended) { offeredUntil = until; acceptedAt = accepted; deadline = dl; endedAt = ended; }
    public void restoreMerged(int m) { merged = m; }
}
