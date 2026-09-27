package yadi.samuraiai.living.quest.templates;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.quest.branching.BranchSpec;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.consequences.ConsequenceSpec;
import yadi.samuraiai.living.quest.objectives.ObjectiveSpec;
import yadi.samuraiai.living.quest.rewards.RewardSpec;

/**
 * A quest template: the shape of a story that fits certain world conditions. It has a category, the conditions that can
 * trigger it, a title and a text for each stage of the story (prologue, development, twist, ending, consequences), its
 * objectives, its paths, rewards and consequences, how long it is offered and how long it may take, the seasons and cultures
 * it belongs to, which professions its giver should have, a weight, and optionally a follow-up (a chain) or a place in a
 * campaign. Texts and quantities use variables filled from the condition and the world.
 */
public record QuestTemplate(String id, Category category, Set<ConditionKind> triggers, String title, Map<StoryStage, String> story, List<ObjectiveSpec> objectives,
                            List<BranchSpec> branches, List<RewardSpec> rewards, List<ConsequenceSpec> consequences, int offerDays, int durationDays, Set<Season> seasons,
                            Set<String> cultures, List<String> giverProfessions, double weight, String followUp, boolean mergeable, double minTrust) {
    public enum Category { STORY, ECONOMY, EXPLORATION, INVESTIGATION, ESCORT, COMBAT, RELIGION, TRAINING, MYSTERY, DIPLOMACY }
    public enum StoryStage { PROLOGUE, DEVELOPMENT, TWIST, ENDING, CONSEQUENCES }

    public QuestTemplate {
        triggers = Set.copyOf(triggers);
        Map<StoryStage, String> s = new EnumMap<>(StoryStage.class);
        s.putAll(story);
        story = Map.copyOf(s);
        objectives = List.copyOf(objectives); branches = List.copyOf(branches); rewards = List.copyOf(rewards); consequences = List.copyOf(consequences);
        seasons = Set.copyOf(seasons); cultures = Set.copyOf(cultures); giverProfessions = List.copyOf(giverProfessions);
        offerDays = Math.max(1, offerDays); durationDays = Math.max(1, durationDays);
        weight = Math.max(0, weight);
        followUp = followUp == null ? "" : followUp;
    }

    public boolean fits(Season season, String culture) {
        return (seasons.isEmpty() || seasons.contains(season)) && (cultures.isEmpty() || culture == null || cultures.contains(culture));
    }

    public String text(StoryStage stage) { return story.getOrDefault(stage, ""); }
}
