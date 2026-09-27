package yadi.samuraiai.ai.cognition.catalog;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.knowledge.history.HistoryType;
import yadi.samuraiai.ai.memory.model.Category;
import yadi.samuraiai.ai.memory.model.EpisodeKind;
import yadi.samuraiai.ai.memory.model.Impression;
import yadi.samuraiai.ai.memory.model.Outcome;
import yadi.samuraiai.ai.relationship.model.LoyaltyKind;
import yadi.samuraiai.ai.relationship.model.RelationType;
import yadi.samuraiai.ai.relationship.model.SocialEffect;

/**
 * What a kind of experience means, all of it data: how much it matters, what it feels like, what it does to the relationship
 * with the person involved, what can be learned from it, what rumour and history it can produce and how it shapes personality.
 * The cognition layer turns one experience into a memory, emotions, relationship changes and knowledge using its profile;
 * no engine hard-codes any of this. Profiles are built with {@link #of} and can be overridden by configuration lines.
 */
public final class ExperienceProfile {
    public enum Focus { ACTOR, TARGET, NONE }

    public final ExperienceKind kind;
    public Category category = Category.PERSONAL;
    public EpisodeKind episode = EpisodeKind.OBSERVATION;
    public Outcome outcome = Outcome.NEUTRAL;
    public double magnitude = 0.3D, danger;
    public boolean repeatable, traumatic, publicEvent, witnessedByDefault;
    public final Map<EmotionKind, Double> emotions = new EnumMap<>(EmotionKind.class);
    public SocialEffect social = SocialEffect.NONE;
    public Focus focus = Focus.ACTOR;
    public RelationType role;
    public LoyaltyKind loyalty;
    public final Set<String> tags = new java.util.LinkedHashSet<>();
    public final Set<String> socialTags = new java.util.LinkedHashSet<>();
    public final List<Impression> impressions = new ArrayList<>();
    public final List<KnowledgeHint> knowledge = new ArrayList<>();
    public RumorHint rumor;
    public HistoryType history;
    public final Map<Trait, Double> drift = new EnumMap<>(Trait.class);
    public String skill, skillKind, landmark;

    private ExperienceProfile(ExperienceKind kind) { this.kind = kind; }

    public static ExperienceProfile of(ExperienceKind kind) { return new ExperienceProfile(kind); }

    public ExperienceProfile cat(Category c, EpisodeKind e) { category = c; episode = e; return this; }
    public ExperienceProfile outcome(Outcome o) { outcome = o; return this; }
    public ExperienceProfile mag(double m) { magnitude = m; return this; }
    public ExperienceProfile danger(double d) { danger = d; return this; }
    public ExperienceProfile repeatable() { repeatable = true; return this; }
    public ExperienceProfile traumatic() { traumatic = true; return this; }
    public ExperienceProfile publicEvent() { publicEvent = true; return this; }
    public ExperienceProfile witnessed() { witnessedByDefault = true; return this; }
    public ExperienceProfile feel(EmotionKind kind, double intensity) { emotions.put(kind, intensity); return this; }
    public ExperienceProfile social(double trust, double respect, double affinity, double fear, double loyalty, double rivalry, double honor) { social = new SocialEffect(trust, respect, affinity, fear, loyalty, rivalry, honor); return this; }
    public ExperienceProfile focus(Focus f) { focus = f; return this; }
    public ExperienceProfile role(RelationType r) { role = r; return this; }
    public ExperienceProfile tag(String t) { tags.add(t); return this; }
    public ExperienceProfile socialTag(String t) { socialTags.add(t); return this; }
    public ExperienceProfile impress(yadi.samuraiai.ai.memory.semantic.Aspect aspect, double delta) { impressions.add(new Impression(aspect, delta)); return this; }
    public ExperienceProfile know(KnowledgeHint hint) { knowledge.add(hint); return this; }
    public ExperienceProfile rumor(yadi.samuraiai.ai.knowledge.model.Predicate p, String label, double magnitude) { rumor = new RumorHint(p, label, magnitude); return this; }
    public ExperienceProfile history(HistoryType h) { history = h; return this; }
    public ExperienceProfile drift(Trait trait, double coefficient) { drift.put(trait, coefficient); return this; }
    public ExperienceProfile skill(String key, String kind) { skill = key; skillKind = kind; return this; }
    public ExperienceProfile landmark(String kind) { landmark = kind; return this; }

    /** The strongest emotion this kind of experience causes (or CALM). */
    public EmotionKind primary() {
        EmotionKind best = EmotionKind.CALM;
        double top = -1;
        for (var e : emotions.entrySet()) if (e.getValue() > top) { top = e.getValue(); best = e.getKey(); }
        return best;
    }

    public ExperienceProfile copy() {
        ExperienceProfile p = new ExperienceProfile(kind);
        p.category = category; p.episode = episode; p.outcome = outcome; p.magnitude = magnitude; p.danger = danger; p.repeatable = repeatable; p.traumatic = traumatic;
        p.publicEvent = publicEvent; p.witnessedByDefault = witnessedByDefault; p.emotions.putAll(emotions); p.social = social; p.focus = focus; p.role = role; p.loyalty = loyalty;
        p.tags.addAll(tags); p.socialTags.addAll(socialTags); p.impressions.addAll(impressions); p.knowledge.addAll(knowledge); p.rumor = rumor; p.history = history;
        p.drift.putAll(drift); p.skill = skill; p.skillKind = skillKind; p.landmark = landmark;
        return p;
    }
}
