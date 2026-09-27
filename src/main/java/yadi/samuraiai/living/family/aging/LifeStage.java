package yadi.samuraiai.living.family.aging;

/** Stages of life. The _FUTURE ones are prepared (the living world does not have children with bodies yet) but ages are real. */
public enum LifeStage {
    INFANT_FUTURE, CHILD_FUTURE, ADOLESCENT_FUTURE, YOUNG_ADULT, ADULT, MATURE, ELDER;

    public boolean adult() { return ordinal() >= YOUNG_ADULT.ordinal(); }
}
