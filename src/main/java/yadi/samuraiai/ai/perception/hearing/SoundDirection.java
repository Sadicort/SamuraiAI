package yadi.samuraiai.ai.perception.hearing;

/** Where a sound seems to come from, relative to the way the listener faces. */
public enum SoundDirection {
    FRONT, FRONT_RIGHT, RIGHT, BACK_RIGHT, BACK, BACK_LEFT, LEFT, FRONT_LEFT, ABOVE, BELOW;

    /**
     * @param relativeYaw degrees to turn (in Minecraft yaw, positive = clockwise from above = toward the listener's right)
     *                    to face the source
     * @param dy          source height minus ear height
     * @param horizontal  horizontal distance to the source
     */
    public static SoundDirection of(double relativeYaw, double dy, double horizontal) {
        if (Math.abs(dy) > Math.max(3.0D, horizontal * 1.2D)) return dy > 0 ? ABOVE : BELOW;
        double a = Math.abs(relativeYaw);
        boolean right = relativeYaw > 0;
        if (a <= 22.5D) return FRONT;
        if (a <= 67.5D) return right ? FRONT_RIGHT : FRONT_LEFT;
        if (a <= 112.5D) return right ? RIGHT : LEFT;
        if (a <= 157.5D) return right ? BACK_RIGHT : BACK_LEFT;
        return BACK;
    }

    public boolean behind() { return this == BACK || this == BACK_LEFT || this == BACK_RIGHT; }
}
