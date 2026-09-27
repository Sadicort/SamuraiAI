package yadi.samuraiai.ai.perception.hearing;

/**
 * A sound as one listener perceived it: how loud it registered, from which direction and, because localisation is
 * imprecise, where the listener <em>thinks</em> it came from and how unsure it is (grows with distance).
 */
public record HeardSound(SoundEvent sound, double distance, double intensity, SoundDirection direction,
                         double estimatedX, double estimatedY, double estimatedZ, double uncertainty) { }
