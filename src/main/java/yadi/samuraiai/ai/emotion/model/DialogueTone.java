package yadi.samuraiai.ai.emotion.model;

/** How feelings colour speech (0-1 unless noted): formality, verbosity, pace, chance of falling silent, how often it asks questions, warmth, and a voice tone label for a future voice engine. */
public record DialogueTone(double formality, double verbosity, double pace, double silence, double questionRate, double warmth, String voiceTone) {
    public static final DialogueTone NEUTRAL = new DialogueTone(0.5D, 0.5D, 0.5D, 0.1D, 0.3D, 0.5D, "neutral");
}
