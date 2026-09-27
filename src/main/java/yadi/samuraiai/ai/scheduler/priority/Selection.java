package yadi.samuraiai.ai.scheduler.priority;

import yadi.samuraiai.ai.scheduler.conflict.Resolution;
import yadi.samuraiai.ai.scheduler.engine.Candidate;

/** What the priority engine decided: the winning candidate, whether it is simply the current one kept, and the conflict that was settled. */
public record Selection(Candidate winner, boolean keepCurrent, boolean emergency, Resolution conflict) { }
