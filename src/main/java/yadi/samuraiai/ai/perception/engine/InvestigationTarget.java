package yadi.samuraiai.ai.perception.engine;

/**
 * Where the NPC has a reason to go and look: the last place a lost target was seen, where a threat was, where a sound came
 * from (with how unsure that is), or something interesting. Evidence for the brain and scheduler, not an order: perception
 * does not start behaviors, it says what is worth investigating and why.
 */
public record InvestigationTarget(double x, double y, double z, double uncertainty, String reason, double urgency, long tick) { }
