package yadi.samuraiai.ai.cognition.catalog;

import yadi.samuraiai.ai.knowledge.model.Predicate;

/** The rumour a public or witnessed experience can start: what is claimed of the actor, which reputation label it bears on and how big it is. */
public record RumorHint(Predicate predicate, String label, double magnitude) { }
