package yadi.samuraiai.ai.relationship.engine;

import yadi.samuraiai.ai.relationship.model.PromiseRecord;

/** How a promise ended and, when the other party's standing with this NPC changed, the update it caused. */
public record PromiseOutcome(PromiseRecord promise, Update update, boolean selfPromise) { }
