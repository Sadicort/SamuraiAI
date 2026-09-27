package yadi.samuraiai.ai.scheduler.engine;

/** Something worth going to look at, as reported by perception: where, how sure the location is, how pressing. */
public record Investigation(double x, double y, double z, double uncertainty, double urgency) { }
