package yadi.samuraiai.ai.scheduler.emotion;

/** The feelings the scheduler reads (each 0-100), supplied by the world adapter from the NPC's emotion state. */
public record EmotionInput(double fear, double anger, double sadness, double joy, double calm, double anxiety, double trust) {
    public static final EmotionInput CALM = new EmotionInput(0, 0, 0, 0, 70, 0, 0);
}
