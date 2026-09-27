package yadi.samuraiai.ai.scheduler.energy;

import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.routine.RoutineProfile;

/**
 * Advances an NPC's condition by an elapsed number of ticks (so an NPC evaluated rarely loses nothing) and reports which
 * needs are pressing. What a routine costs or restores is in its {@link RoutineProfile}; the passive drift is configuration.
 */
public final class EnergyModel {
    private final SchedulerSettings settings;

    public EnergyModel(SchedulerSettings settings) { this.settings = settings; }

    /**
     * @param profile  the routine being performed, or null when the NPC is only travelling or idle
     * @param performing whether the routine's own effects apply (it has begun; a walker on its way is only passing time)
     * @param workRate  personality's tolerance of effort: below 1.0 tires slower
     */
    public void advance(EnergyState s, RoutineProfile profile, boolean performing, long ticks, double workRate) {
        if (ticks <= 0) return;
        boolean asleep = profile != null && performing && profile.restorative() && profile.fatigue() <= -0.02D;
        double energy = s.energy(), fatigue = s.fatigue(), focus = s.focus(), stress = s.stress(), motivation = s.motivation();
        double t = ticks;
        if (!asleep) { energy -= settings.energyDrainPerTick() * t; fatigue += settings.fatigueGainPerTick() * t; }
        stress -= settings.stressDecayPerTick() * t;
        focus += settings.focusRecoveryPerTick() * t;
        motivation += Math.signum(50.0D - motivation) * Math.min(Math.abs(50.0D - motivation), settings.motivationDriftPerTick() * t);
        if (profile != null && performing) {
            double drain = profile.energy() < 0 ? 1.0D / Math.max(0.3D, workRate) : 1.0D;
            energy += profile.energy() * t * drain;
            fatigue += profile.fatigue() * t * (profile.fatigue() > 0 ? drain : 1.0D);
            focus += profile.focus() * t;
            stress += profile.stress() * t;
            motivation += profile.motivation() * t;
        }
        s.set(energy, fatigue, focus, stress, motivation);
    }

    /** How pressing a need is, 0 (not at all) to 100. */
    public double urgency(EnergyState s, EnergyNeed need) {
        return switch (need) {
            case SLEEP -> scale(s.fatigue(), settings.sleepNeedFatigue() - 20, 100);
            case REST -> scale(s.fatigue(), settings.restNeedFatigue() - 15, settings.sleepNeedFatigue() + 10);
            case REFUEL -> scale(settings.lowEnergy() + 25 - s.energy(), 0, settings.lowEnergy() + 25);
            case CALM_DOWN -> scale(s.stress(), settings.highStress() - 30, 100);
            case MOTIVATE -> scale(40 - s.motivation(), 0, 40);
        };
    }

    public boolean exhausted(EnergyState s) { return s.fatigue() >= settings.sleepNeedFatigue() || s.energy() <= settings.lowEnergy() / 2.0D; }
    public boolean overstressed(EnergyState s) { return s.stress() >= settings.highStress(); }

    private static double scale(double v, double lo, double hi) {
        if (hi <= lo) return v >= hi ? 100.0D : 0.0D;
        return Math.max(0.0D, Math.min(100.0D, (v - lo) / (hi - lo) * 100.0D));
    }
}
