package yadi.samuraiai.ai.cognition.catalog;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.knowledge.history.HistoryType;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.memory.model.Category;
import yadi.samuraiai.ai.memory.model.EpisodeKind;
import yadi.samuraiai.ai.memory.model.Outcome;
import yadi.samuraiai.ai.memory.semantic.Aspect;

/**
 * The meaning of every {@link ExperienceKind}. Built-in defaults describe a sensible world; a configuration line
 * {@code KIND|magnitude|danger|EMOTION:intensity;EMOTION:intensity|trust,respect,affinity,fear,loyalty,rivalry,honor} overrides
 * the numbers of one kind (empty fields keep the default), so the world's designers own the tuning, not the code.
 */
public final class ExperienceCatalog {
    private final Map<ExperienceKind, ExperienceProfile> profiles = new EnumMap<>(ExperienceKind.class);

    public ExperienceCatalog(List<String> overrides) {
        defaults();
        for (String line : overrides) applyOverride(line);
    }

    public ExperienceProfile get(ExperienceKind kind) { return profiles.get(kind); }
    public int size() { return profiles.size(); }

    private ExperienceProfile add(ExperienceProfile p) { profiles.put(p.kind, p); return p; }

    private void applyOverride(String line) {
        try {
            String[] f = line.split("\\|", -1);
            if (f.length < 1) return;
            ExperienceProfile p = profiles.get(ExperienceKind.valueOf(f[0].trim().toUpperCase(Locale.ROOT)));
            if (p == null) return;
            if (f.length > 1 && !f[1].isBlank()) p.mag(Double.parseDouble(f[1].trim()));
            if (f.length > 2 && !f[2].isBlank()) p.danger(Double.parseDouble(f[2].trim()));
            if (f.length > 3 && !f[3].isBlank()) {
                p.emotions.clear();
                for (String e : f[3].split(";")) { String[] kv = e.split(":"); if (kv.length == 2) EmotionKind.parse(kv[0]).ifPresent(k -> p.emotions.put(k, Double.parseDouble(kv[1].trim()))); }
            }
            if (f.length > 4 && !f[4].isBlank()) {
                String[] n = f[4].split(",");
                if (n.length == 7) p.social(Double.parseDouble(n[0].trim()), Double.parseDouble(n[1].trim()), Double.parseDouble(n[2].trim()), Double.parseDouble(n[3].trim()),
                        Double.parseDouble(n[4].trim()), Double.parseDouble(n[5].trim()), Double.parseDouble(n[6].trim()));
            }
        } catch (RuntimeException ignored) { /* a malformed override is skipped */ }
    }

    private static KnowledgeHint fact(Predicate p, Selector subject, Selector object, LearnMethod method, double importance) { return KnowledgeHint.of(KnowledgeType.FACT, p, subject, object, method, importance); }

    private void defaults() {
        add(ExperienceProfile.of(ExperienceKind.MET_PERSON).cat(Category.SOCIAL, EpisodeKind.CONVERSATION).mag(0.2).repeatable().feel(EmotionKind.CURIOSITY, 15).social(1, 0, 2, 0, 0, 0, 0)
                .know(KnowledgeHint.of(KnowledgeType.PERSON, Predicate.KNOWS, Selector.SELF, Selector.ACTOR, LearnMethod.OBSERVATION, 0.3)));
        add(ExperienceProfile.of(ExperienceKind.CONVERSATION).cat(Category.SOCIAL, EpisodeKind.CONVERSATION).outcome(Outcome.POSITIVE).mag(0.25).repeatable()
                .feel(EmotionKind.JOY, 10).feel(EmotionKind.CALM, 8).social(2, 1, 3, 0, 0, 0, 0).drift(Trait.SOCIABILITY, 0.2)
                .know(KnowledgeHint.of(KnowledgeType.PERSON, Predicate.KNOWS, Selector.SELF, Selector.ACTOR, LearnMethod.EXPERIENCE, 0.3)));
        add(ExperienceProfile.of(ExperienceKind.HELPED_ME).cat(Category.SOCIAL, EpisodeKind.FRIENDSHIP).outcome(Outcome.POSITIVE).mag(0.8).feel(EmotionKind.GRATITUDE, 70).feel(EmotionKind.JOY, 40).feel(EmotionKind.HOPE, 20)
                .social(28, 14, 14, 0, 8, 0, 12).impress(Aspect.TRUSTWORTHY, 0.6).impress(Aspect.HELPFUL, 0.7).impress(Aspect.FRIENDLY, 0.3).tag("help")
                .know(fact(Predicate.HELPED, Selector.ACTOR, Selector.SELF, LearnMethod.EXPERIENCE, 0.7)).drift(Trait.SOCIABILITY, 0.3).drift(Trait.LOYALTY, 0.2));
        add(ExperienceProfile.of(ExperienceKind.ATTACKED_ME).cat(Category.COMBAT, EpisodeKind.COMBAT).outcome(Outcome.NEGATIVE).mag(0.85).danger(0.8).feel(EmotionKind.FEAR, 60).feel(EmotionKind.ANGER, 55).feel(EmotionKind.SADNESS, 15)
                .social(-35, -4, -20, 30, 0, 18, -18).impress(Aspect.DANGEROUS, 0.7).impress(Aspect.HOSTILE, 0.6).impress(Aspect.TRUSTWORTHY, -0.6).tag("attack")
                .know(fact(Predicate.ATTACKED, Selector.ACTOR, Selector.SELF, LearnMethod.EXPERIENCE, 0.8)).drift(Trait.CAUTION, 0.5).drift(Trait.COURAGE, -0.3));
        add(ExperienceProfile.of(ExperienceKind.WITNESSED_ATTACK).cat(Category.COMBAT, EpisodeKind.OBSERVATION).outcome(Outcome.NEGATIVE).mag(0.6).danger(0.4).witnessed().publicEvent()
                .feel(EmotionKind.ANGER, 30).feel(EmotionKind.FEAR, 25).feel(EmotionKind.COMPASSION, 25).feel(EmotionKind.DISTRUST, 30).social(-25, -5, -8, 10, 0, 0, -25)
                .impress(Aspect.DANGEROUS, 0.4).tag("attack").know(fact(Predicate.ATTACKED, Selector.ACTOR, Selector.TARGET, LearnMethod.OBSERVATION, 0.6)).rumor(Predicate.ATTACKED, "BANDIT", 0.6));
        add(ExperienceProfile.of(ExperienceKind.WITNESSED_DEATH).cat(Category.DANGER, EpisodeKind.OBSERVATION).outcome(Outcome.NEGATIVE).mag(0.9).danger(0.5).witnessed().publicEvent()
                .feel(EmotionKind.SADNESS, 70).feel(EmotionKind.FEAR, 55).feel(EmotionKind.ANGER, 25).feel(EmotionKind.GUILT, 15).social(-40, -5, -10, 25, 0, 10, -20)
                .impress(Aspect.DANGEROUS, 0.5).tag("death").tag("pivotal").history(HistoryType.DEATH).know(fact(Predicate.ATTACKED, Selector.ACTOR, Selector.TARGET, LearnMethod.OBSERVATION, 0.8))
                .rumor(Predicate.ATTACKED, "BANDIT", 0.8).drift(Trait.CAUTION, 0.6).drift(Trait.COURAGE, -0.3));
        add(ExperienceProfile.of(ExperienceKind.BETRAYED).cat(Category.SOCIAL, EpisodeKind.FRIENDSHIP).outcome(Outcome.NEGATIVE).mag(0.95).feel(EmotionKind.ANGER, 65).feel(EmotionKind.SADNESS, 60).feel(EmotionKind.DISTRUST, 70).feel(EmotionKind.SHAME, 10)
                .social(-55, -25, -35, 5, -30, 25, -40).impress(Aspect.TRUSTWORTHY, -0.9).impress(Aspect.HONORABLE, -0.8).tag("betrayal").tag("pivotal").history(HistoryType.BETRAYAL)
                .know(fact(Predicate.ATTACKED, Selector.ACTOR, Selector.SELF, LearnMethod.EXPERIENCE, 0.9)).rumor(Predicate.ATTACKED, "TRAITOR", 0.8).drift(Trait.LOYALTY, -0.6).drift(Trait.CAUTION, 0.6));
        add(ExperienceProfile.of(ExperienceKind.GIFT_RECEIVED).cat(Category.SOCIAL, EpisodeKind.FRIENDSHIP).outcome(Outcome.POSITIVE).mag(0.4).feel(EmotionKind.JOY, 40).feel(EmotionKind.GRATITUDE, 40).social(8, 2, 12, 0, 3, 0, 2).impress(Aspect.FRIENDLY, 0.4));
        add(ExperienceProfile.of(ExperienceKind.INSULTED).cat(Category.SOCIAL, EpisodeKind.CONVERSATION).outcome(Outcome.NEGATIVE).mag(0.35).feel(EmotionKind.ANGER, 35).feel(EmotionKind.SHAME, 20).social(-8, -8, -10, 0, 0, 8, -5).impress(Aspect.FRIENDLY, -0.3));
        add(ExperienceProfile.of(ExperienceKind.PROMISE_MADE).cat(Category.SOCIAL, EpisodeKind.MISSION).mag(0.5).feel(EmotionKind.HOPE, 25).feel(EmotionKind.DETERMINATION, 30).tag("promise"));
        add(ExperienceProfile.of(ExperienceKind.PROMISE_KEPT).cat(Category.SOCIAL, EpisodeKind.FRIENDSHIP).outcome(Outcome.POSITIVE).mag(0.6).feel(EmotionKind.GRATITUDE, 40).feel(EmotionKind.RESPECT, 45).feel(EmotionKind.JOY, 25)
                .impress(Aspect.HONORABLE, 0.6).impress(Aspect.TRUSTWORTHY, 0.5).tag("promise"));
        add(ExperienceProfile.of(ExperienceKind.PROMISE_BROKEN).cat(Category.SOCIAL, EpisodeKind.FRIENDSHIP).outcome(Outcome.NEGATIVE).mag(0.8).feel(EmotionKind.ANGER, 45).feel(EmotionKind.SADNESS, 40).feel(EmotionKind.DISTRUST, 55)
                .impress(Aspect.HONORABLE, -0.6).impress(Aspect.TRUSTWORTHY, -0.5).tag("promise").know(fact(Predicate.IS_HONORABLE, Selector.ACTOR, Selector.NONE, LearnMethod.EXPERIENCE, 0.6)).rumor(Predicate.IS_HONORABLE, "TRAITOR", 0.6));
        add(ExperienceProfile.of(ExperienceKind.DISCOVERED_PLACE).cat(Category.DISCOVERY, EpisodeKind.DISCOVERY).outcome(Outcome.POSITIVE).mag(0.6).feel(EmotionKind.CURIOSITY, 45).feel(EmotionKind.JOY, 25).feel(EmotionKind.PRIDE, 15)
                .landmark("LANDMARK").tag("discovery").drift(Trait.CURIOSITY, 0.6).history(HistoryType.DISCOVERY)
                .know(KnowledgeHint.of(KnowledgeType.PLACE, Predicate.LOCATED_AT, Selector.PLACE, Selector.NONE, LearnMethod.OBSERVATION, 0.6).discovered())
                .know(KnowledgeHint.of(KnowledgeType.PLACE, Predicate.DISCOVERED, Selector.SELF, Selector.PLACE, LearnMethod.EXPERIENCE, 0.4)));
        add(ExperienceProfile.of(ExperienceKind.VISITED_PLACE).cat(Category.TRAVEL, EpisodeKind.TRAVEL).mag(0.2).repeatable().feel(EmotionKind.CALM, 5).landmark("LANDMARK")
                .know(KnowledgeHint.of(KnowledgeType.PLACE, Predicate.LOCATED_AT, Selector.PLACE, Selector.NONE, LearnMethod.OBSERVATION, 0.3))
                .know(KnowledgeHint.of(KnowledgeType.PLACE, Predicate.VISITED, Selector.SELF, Selector.PLACE, LearnMethod.EXPERIENCE, 0.2)));
        add(ExperienceProfile.of(ExperienceKind.PATROLLED).cat(Category.ROUTINE, EpisodeKind.PATROL).outcome(Outcome.POSITIVE).mag(0.12).repeatable().feel(EmotionKind.CALM, 6).feel(EmotionKind.DETERMINATION, 8)
                .skill("patrol", "PATROL").drift(Trait.DILIGENCE, 0.05));
        add(ExperienceProfile.of(ExperienceKind.TRAVELED).cat(Category.TRAVEL, EpisodeKind.TRAVEL).mag(0.15).repeatable().feel(EmotionKind.CURIOSITY, 8).skill("route", "ROUTE"));
        add(ExperienceProfile.of(ExperienceKind.MEDITATED).cat(Category.SPIRITUAL, EpisodeKind.MEDITATION).outcome(Outcome.POSITIVE).mag(0.15).repeatable().feel(EmotionKind.CALM, 25).feel(EmotionKind.INSPIRATION, 8)
                .skill("meditation", "MEDITATION").drift(Trait.SPIRITUALITY, 0.1).drift(Trait.PATIENCE, 0.05));
        add(ExperienceProfile.of(ExperienceKind.SLEPT).cat(Category.ROUTINE, EpisodeKind.REST).mag(0.08).repeatable().feel(EmotionKind.CALM, 12).skill("rest", "REST_SPOT"));
        add(ExperienceProfile.of(ExperienceKind.WON_BATTLE).cat(Category.COMBAT, EpisodeKind.COMBAT).outcome(Outcome.POSITIVE).mag(0.75).danger(0.5).feel(EmotionKind.PRIDE, 50).feel(EmotionKind.JOY, 35).feel(EmotionKind.DETERMINATION, 20)
                .history(HistoryType.BATTLE).drift(Trait.COURAGE, 0.5).drift(Trait.AGGRESSION, 0.15).tag("battle"));
        add(ExperienceProfile.of(ExperienceKind.LOST_BATTLE).cat(Category.COMBAT, EpisodeKind.COMBAT).outcome(Outcome.NEGATIVE).mag(0.8).danger(0.7).feel(EmotionKind.SHAME, 45).feel(EmotionKind.SADNESS, 45).feel(EmotionKind.FEAR, 30).feel(EmotionKind.ANGER, 25)
                .history(HistoryType.BATTLE).drift(Trait.CAUTION, 0.4).drift(Trait.COURAGE, -0.3).tag("battle"));
        add(ExperienceProfile.of(ExperienceKind.WITNESSED_FIRE).cat(Category.DANGER, EpisodeKind.FEAR).outcome(Outcome.NEGATIVE).mag(0.6).danger(0.7).witnessed().publicEvent().feel(EmotionKind.FEAR, 45).feel(EmotionKind.ANXIETY, 40).feel(EmotionKind.SURPRISE, 30)
                .history(HistoryType.FIRE).tag("fire").know(KnowledgeHint.of(KnowledgeType.DANGER, Predicate.IS_DANGEROUS, Selector.PLACE, Selector.NONE, LearnMethod.OBSERVATION, 0.6)));
        add(ExperienceProfile.of(ExperienceKind.WITNESSED_EXPLOSION).cat(Category.DANGER, EpisodeKind.FEAR).outcome(Outcome.NEGATIVE).mag(0.55).danger(0.6).witnessed().publicEvent().feel(EmotionKind.FEAR, 40).feel(EmotionKind.SURPRISE, 45).feel(EmotionKind.ANXIETY, 25)
                .history(HistoryType.DISASTER).tag("explosion").know(KnowledgeHint.of(KnowledgeType.DANGER, Predicate.IS_DANGEROUS, Selector.PLACE, Selector.NONE, LearnMethod.OBSERVATION, 0.6)));
        add(ExperienceProfile.of(ExperienceKind.TAUGHT).cat(Category.KNOWLEDGE, EpisodeKind.CONVERSATION).outcome(Outcome.POSITIVE).mag(0.3).feel(EmotionKind.PRIDE, 15).feel(EmotionKind.COMPASSION, 15).social(2, 3, 3, 0, 0, 0, 0).focus(ExperienceProfile.Focus.TARGET));
        add(ExperienceProfile.of(ExperienceKind.LEARNED_FROM).cat(Category.KNOWLEDGE, EpisodeKind.CONVERSATION).outcome(Outcome.POSITIVE).mag(0.35).feel(EmotionKind.CURIOSITY, 25).feel(EmotionKind.GRATITUDE, 15).feel(EmotionKind.RESPECT, 20).social(4, 6, 3, 0, 0, 0, 0)
                .drift(Trait.CURIOSITY, 0.2));
        add(ExperienceProfile.of(ExperienceKind.HEARD_RUMOR).cat(Category.SOCIAL, EpisodeKind.CONVERSATION).mag(0.2).feel(EmotionKind.CURIOSITY, 12).feel(EmotionKind.ANXIETY, 8));
        add(ExperienceProfile.of(ExperienceKind.TRAINED_TOGETHER).cat(Category.DUTY, EpisodeKind.MISSION).outcome(Outcome.POSITIVE).mag(0.35).repeatable().feel(EmotionKind.DETERMINATION, 25).feel(EmotionKind.JOY, 15).feel(EmotionKind.RESPECT, 20)
                .social(5, 8, 6, 0, 0, 3, 0).skill("training", "TRAINING").drift(Trait.DISCIPLINE, 0.1));
        add(ExperienceProfile.of(ExperienceKind.ATTENDED_RITUAL).cat(Category.CULTURAL, EpisodeKind.FESTIVAL).outcome(Outcome.POSITIVE).mag(0.3).repeatable().feel(EmotionKind.CALM, 20).feel(EmotionKind.INSPIRATION, 20).feel(EmotionKind.PRIDE, 10)
                .tag("tradition").drift(Trait.SPIRITUALITY, 0.1).drift(Trait.LOYALTY, 0.05));
        add(ExperienceProfile.of(ExperienceKind.CELEBRATED).cat(Category.CULTURAL, EpisodeKind.FESTIVAL).outcome(Outcome.POSITIVE).mag(0.45).publicEvent().witnessed().feel(EmotionKind.JOY, 50).feel(EmotionKind.PRIDE, 20).feel(EmotionKind.GRATITUDE, 15)
                .history(HistoryType.FESTIVAL).tag("festival").drift(Trait.SOCIABILITY, 0.15));
        add(ExperienceProfile.of(ExperienceKind.PROTECTED_OTHERS).cat(Category.DUTY, EpisodeKind.MISSION).outcome(Outcome.POSITIVE).mag(0.7).danger(0.4).feel(EmotionKind.PRIDE, 45).feel(EmotionKind.DETERMINATION, 35).feel(EmotionKind.JOY, 15)
                .social(12, 4, 10, 0, 6, 0, 4).focus(ExperienceProfile.Focus.TARGET).socialTag("shared_danger").history(HistoryType.HERO_ACT).rumor(Predicate.PROTECTS, "PROTECTOR", 0.7).publicEvent()
                .know(fact(Predicate.PROTECTS, Selector.SELF, Selector.TARGET, LearnMethod.EXPERIENCE, 0.6)).drift(Trait.COURAGE, 0.3).drift(Trait.DISCIPLINE, 0.1).tag("protect"));
        add(ExperienceProfile.of(ExperienceKind.NEAR_DEATH).cat(Category.DANGER, EpisodeKind.FEAR).outcome(Outcome.NEGATIVE).mag(0.95).danger(1.0).traumatic().feel(EmotionKind.FEAR, 85).feel(EmotionKind.ANGER, 30)
                .social(-25, 0, -10, 30, 0, 8, -8).impress(Aspect.DANGEROUS, 0.8).tag("death").drift(Trait.CAUTION, 0.7).drift(Trait.COURAGE, -0.4));
        add(ExperienceProfile.of(ExperienceKind.ABANDONED).cat(Category.SOCIAL, EpisodeKind.FRIENDSHIP).outcome(Outcome.NEGATIVE).mag(0.7).feel(EmotionKind.SADNESS, 55).feel(EmotionKind.LONELINESS, 60).feel(EmotionKind.ANGER, 30).feel(EmotionKind.DISTRUST, 40)
                .social(-30, -10, -25, 0, -25, 4, -22).impress(Aspect.TRUSTWORTHY, -0.5).drift(Trait.LOYALTY, -0.3).tag("abandon"));
        add(ExperienceProfile.of(ExperienceKind.LIED_TO).cat(Category.SOCIAL, EpisodeKind.CONVERSATION).outcome(Outcome.NEGATIVE).mag(0.45).feel(EmotionKind.ANGER, 30).feel(EmotionKind.DISTRUST, 50).social(-22, -6, -8, 0, 0, 3, -15)
                .impress(Aspect.TRUSTWORTHY, -0.5).impress(Aspect.HONORABLE, -0.3));
        add(ExperienceProfile.of(ExperienceKind.DUEL_HONORED).cat(Category.COMBAT, EpisodeKind.COMBAT).outcome(Outcome.MIXED).mag(0.65).danger(0.4).feel(EmotionKind.RESPECT, 45).feel(EmotionKind.PRIDE, 25).feel(EmotionKind.DETERMINATION, 30)
                .social(8, 22, 0, 0, 0, 12, 18).impress(Aspect.HONORABLE, 0.6).tag("duel").drift(Trait.DISCIPLINE, 0.2));
        add(ExperienceProfile.of(ExperienceKind.MISSION_DONE).cat(Category.DUTY, EpisodeKind.MISSION).outcome(Outcome.POSITIVE).mag(0.55).feel(EmotionKind.PRIDE, 40).feel(EmotionKind.JOY, 25).drift(Trait.DILIGENCE, 0.2));
        add(ExperienceProfile.of(ExperienceKind.THREAT_SEEN).cat(Category.DANGER, EpisodeKind.FEAR).outcome(Outcome.NEGATIVE).mag(0.3).danger(0.4).repeatable().feel(EmotionKind.FEAR, 25).feel(EmotionKind.ANXIETY, 25).social(-3, 0, 0, 8, 0, 0, 0)
                .know(KnowledgeHint.of(KnowledgeType.DANGER, Predicate.IS_DANGEROUS, Selector.PLACE, Selector.NONE, LearnMethod.OBSERVATION, 0.4)));
        add(ExperienceProfile.of(ExperienceKind.LOST_ALLY).cat(Category.SOCIAL, EpisodeKind.FRIENDSHIP).outcome(Outcome.NEGATIVE).mag(0.9).traumatic().feel(EmotionKind.SADNESS, 75).feel(EmotionKind.LONELINESS, 35).feel(EmotionKind.ANGER, 25)
                .tag("death").tag("pivotal").drift(Trait.CAUTION, 0.3).drift(Trait.SPIRITUALITY, 0.2));
        add(ExperienceProfile.of(ExperienceKind.HONOR_OBSERVED).cat(Category.SOCIAL, EpisodeKind.OBSERVATION).outcome(Outcome.POSITIVE).mag(0.4).witnessed().publicEvent().feel(EmotionKind.RESPECT, 30).feel(EmotionKind.INSPIRATION, 15)
                .social(6, 16, 0, 0, 0, 0, 18).impress(Aspect.HONORABLE, 0.5).rumor(Predicate.IS_HONORABLE, "SAMURAI", 0.5).know(fact(Predicate.IS_HONORABLE, Selector.ACTOR, Selector.NONE, LearnMethod.OBSERVATION, 0.5)));
        add(ExperienceProfile.of(ExperienceKind.DISHONOR_OBSERVED).cat(Category.SOCIAL, EpisodeKind.OBSERVATION).outcome(Outcome.NEGATIVE).mag(0.5).witnessed().publicEvent().feel(EmotionKind.DISTRUST, 35).feel(EmotionKind.ANGER, 20)
                .social(-10, -10, 0, 0, 0, 0, -22).impress(Aspect.HONORABLE, -0.5).rumor(Predicate.IS_HONORABLE, "TRAITOR", 0.5));
    }
}
