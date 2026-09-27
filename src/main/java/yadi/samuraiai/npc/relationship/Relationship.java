package yadi.samuraiai.npc.relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * One NPC's individual bond towards another character (player or NPC).
 * Deliberately separate from Personality (stable tendencies) and Emotion
 * (short-lived state) — this is what accumulates over a shared history.
 *
 * <p>Every axis runs from -100 to 100 and is clamped on write, so no caller
 * can push a relationship out of range. Synchronised because bonds are
 * updated on the server thread while {@link #describe()} is read by the
 * dialogue pipeline off-thread.
 */
public class Relationship {

    /** Below this magnitude an axis is not worth mentioning in dialogue. */
    private static final int SIGNIFICANT = 20;

    private int trust;
    private int respect;
    private int hostility;
    private int gratitude;
    private int loyalty;

    public Relationship() {
        this(0, 0, 0, 0, 0);
    }

    public Relationship(int trust, int respect, int hostility, int gratitude, int loyalty) {
        this.trust = clamp(trust);
        this.respect = clamp(respect);
        this.hostility = clamp(hostility);
        this.gratitude = clamp(gratitude);
        this.loyalty = clamp(loyalty);
    }

    private static int clamp(int value) {
        return Math.max(-100, Math.min(100, value));
    }

    synchronized void adjustTrust(int amount) {
        trust = clamp(trust + amount);
    }

    synchronized void adjustRespect(int amount) {
        respect = clamp(respect + amount);
    }

    synchronized void adjustHostility(int amount) {
        hostility = clamp(hostility + amount);
    }

    synchronized void adjustGratitude(int amount) {
        gratitude = clamp(gratitude + amount);
    }

    synchronized void adjustLoyalty(int amount) {
        loyalty = clamp(loyalty + amount);
    }

    public synchronized int getTrust() {
        return trust;
    }

    public synchronized int getRespect() {
        return respect;
    }

    public synchronized int getHostility() {
        return hostility;
    }

    public synchronized int getGratitude() {
        return gratitude;
    }

    public synchronized int getLoyalty() {
        return loyalty;
    }

    /** True while nothing has happened between these two worth remembering. */
    public synchronized boolean isNeutral() {
        return Math.abs(trust) < SIGNIFICANT
                && Math.abs(respect) < SIGNIFICANT
                && Math.abs(hostility) < SIGNIFICANT
                && Math.abs(gratitude) < SIGNIFICANT
                && Math.abs(loyalty) < SIGNIFICANT;
    }

    /**
     * Overall disposition, useful for a quick hostile/friendly check by the
     * decision layer without it needing to know the individual axes.
     */
    public synchronized int overall() {
        return clamp((trust + respect + gratitude + loyalty) / 4 - hostility);
    }

    /**
     * Plain-language summary for the prompt. Numbers would invite the model
     * to talk about statistics; adjectives make it act on them instead.
     */
    public synchronized String describe() {

        List<String> traits = new ArrayList<>();

        addTrait(traits, trust, "confias en el", "desconfias de el");
        addTrait(traits, respect, "le respetas", "le desprecias");
        addTrait(traits, hostility, "le guardas rencor", "estas en paz con el");
        addTrait(traits, gratitude, "le debes un favor", "sientes que te debe algo");
        addTrait(traits, loyalty, "le eres leal", "no le debes lealtad");

        if (traits.isEmpty()) {
            return "no tienes una opinion formada";
        }

        return String.join(", ", traits);
    }

    private static void addTrait(List<String> traits, int value, String positive, String negative) {

        if (value >= SIGNIFICANT) {
            traits.add(positive);
        } else if (value <= -SIGNIFICANT) {
            traits.add(negative);
        }
    }

    @Override
    public synchronized String toString() {
        return "Relationship[trust=" + trust + ", respect=" + respect
                + ", hostility=" + hostility + ", gratitude=" + gratitude
                + ", loyalty=" + loyalty + "]";
    }
}
