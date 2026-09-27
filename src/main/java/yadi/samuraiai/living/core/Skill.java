package yadi.samuraiai.living.core;

/** Experience in a trade, shared vocabulary of the World (professions), Villages (citizens), Economy (output) and Family (apprentices). */
public final class Skill {
    private Skill() { }

    /** Skill level from accumulated work hours: novice, apprentice, journeyman, master. */
    public static String rank(double hours) {
        if (hours >= 2000) return "maestro";
        if (hours >= 600) return "oficial";
        if (hours >= 120) return "aprendiz";
        return "novato";
    }

    /** Output multiplier from experience: 0.6 for a novice rising towards 1.3 for a master. */
    public static double multiplier(double hours) { return 0.6D + 0.7D * (1.0D - Math.exp(-Math.max(0.0D, hours) / 700.0D)); }
}
