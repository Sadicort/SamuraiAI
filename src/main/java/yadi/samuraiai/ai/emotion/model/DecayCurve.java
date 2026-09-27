package yadi.samuraiai.ai.emotion.model;

/** How an emotion fades: steadily, by half-lives, slowly with a residue (trauma), slowly and renewably (hope), or by its own half-life. */
public enum DecayCurve { LINEAR, EXPONENTIAL, TRAUMA, HOPE, CUSTOM }
