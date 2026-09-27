package yadi.samuraiai.living.village.security;

/** A village's state of security. It shapes the whole community's day: guards on duty, civilians home, the market closed. */
public enum SecurityState {
    PEACE, ALERT, DANGER, ATTACK, RECOVERY;

    public boolean threatened() { return this == DANGER || this == ATTACK; }
}
