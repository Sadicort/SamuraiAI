package yadi.samuraiai.ai.cognition.catalog;

import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;

/** What an experience teaches: a fact about a subject and (maybe) an object, how it was learned and how much it matters. {@code discovery} routes it through the discovery engine. */
public record KnowledgeHint(KnowledgeType type, Predicate predicate, Selector subject, Selector object, KnowledgeCategory category, LearnMethod method, double importance, AccessLevel access, boolean discovery) {
    public static KnowledgeHint of(KnowledgeType type, Predicate predicate, Selector subject, Selector object, LearnMethod method, double importance) {
        return new KnowledgeHint(type, predicate, subject, object, KnowledgeCategory.GENERAL, method, importance, AccessLevel.PUBLIC, false);
    }
    public KnowledgeHint discovered() { return new KnowledgeHint(type, predicate, subject, object, category, method, importance, access, true); }
    public KnowledgeHint category(KnowledgeCategory c) { return new KnowledgeHint(type, predicate, subject, object, c, method, importance, access, discovery); }
}
