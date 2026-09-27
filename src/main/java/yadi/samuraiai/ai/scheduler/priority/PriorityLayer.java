package yadi.samuraiai.ai.scheduler.priority;

/** The four layers of priority. A higher layer that has something worth doing always outranks a lower one. */
public enum PriorityLayer {
    /** The timeline's routine: what this kind of NPC does at this hour. */
    BASELINE,
    /** Personal needs and moods: tiredness, hunger, stress, grief. */
    PERSONAL,
    /** Something happening around the NPC: a sound, an ally's call, a suspicious presence. */
    SITUATIONAL,
    /** Survival: it overrides everything, including the interrupt policy of what was being done. */
    EMERGENCY;

    public boolean above(PriorityLayer other) { return compareTo(other) > 0; }
}
