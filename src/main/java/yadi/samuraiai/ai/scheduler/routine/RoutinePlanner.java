package yadi.samuraiai.ai.scheduler.routine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import yadi.samuraiai.ai.scheduler.cooldown.CooldownEngine;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInfluence;
import yadi.samuraiai.ai.scheduler.energy.EnergyModel;
import yadi.samuraiai.ai.scheduler.energy.EnergyNeed;
import yadi.samuraiai.ai.scheduler.energy.EnergyState;
import yadi.samuraiai.ai.scheduler.engine.Candidate;
import yadi.samuraiai.ai.scheduler.engine.Intent;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.engine.Source;
import yadi.samuraiai.ai.scheduler.lifestyle.Lifestyle;
import yadi.samuraiai.ai.scheduler.personality.PersonalityEngine;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;
import yadi.samuraiai.ai.scheduler.time.CalendarEvent;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.time.WorldCalendar;
import yadi.samuraiai.ai.scheduler.zone.Place;

/**
 * The routine planner: from what part of the day it is, the NPC's lifestyle and personality, its condition, its mood and the
 * world calendar, it lists the routines the NPC could be doing, each with a score and the reason for it. It plans; it does
 * not choose (that is the priority engine's job) and it moves nothing.
 */
public final class RoutinePlanner {
    /** Everything the planner reads about one NPC at one moment. */
    public record Input(DayPeriod period, Lifestyle lifestyle, PersonalityTraits traits, EnergyState energy, EmotionInfluence emotion,
                        List<CalendarEvent> events, CooldownEngine cooldowns, long now, String currentKey,
                        Function<RoutineType, Place> places, java.util.Map<RoutineType, Double> external) {
        public Input {
            external = external == null ? java.util.Map.of() : external;
        }

        public Input(DayPeriod period, Lifestyle lifestyle, PersonalityTraits traits, EnergyState energy, EmotionInfluence emotion,
                     List<CalendarEvent> events, CooldownEngine cooldowns, long now, String currentKey, Function<RoutineType, Place> places) {
            this(period, lifestyle, traits, energy, emotion, events, cooldowns, now, currentKey, places, java.util.Map.of());
        }
    }

    private final SchedulerSettings settings;
    private final RoutineProfiles profiles;
    private final PersonalityEngine personality;
    private final EnergyModel energy;
    private final WorldCalendar calendar;

    public RoutinePlanner(SchedulerSettings settings, RoutineProfiles profiles, PersonalityEngine personality, EnergyModel energy, WorldCalendar calendar) {
        this.settings = settings; this.profiles = profiles; this.personality = personality; this.energy = energy; this.calendar = calendar;
    }

    public RoutineProfiles profiles() { return profiles; }

    public List<Candidate> plan(Input in) {
        List<Candidate> out = new ArrayList<>();
        double fatigueNeed = Math.max(energy.urgency(in.energy(), EnergyNeed.SLEEP), energy.urgency(in.energy(), EnergyNeed.REST));
        for (RoutineType routine : RoutineType.values()) {
            double calendarBias = settings.calendarInfluence() * calendar.bias(in.events(), routine);
            double emotionBias = in.emotion().bias(routine);
            double externalBias = in.external().getOrDefault(routine, 0.0D);
            double weight = in.lifestyle().weight(in.period(), routine) * settings.lifestyleWeightScale();
            if (weight <= 0 && calendarBias <= 0 && emotionBias <= 0 && externalBias <= 0) continue;
            if (!in.cooldowns().ready(cooldownKey(routine), in.now())) continue;
            Place place = in.places().apply(routine);
            if (place == null) continue;
            RoutineProfile profile = profiles.of(routine);
            double affinity = personality.affinity(in.traits(), routine);
            double score = weight * (1.0D + settings.personalityInfluence() * (affinity - 1.0D)) + calendarBias + emotionBias + externalBias;
            StringBuilder why = new StringBuilder(String.format("%s weight %.0f", in.period(), weight));
            if (Math.abs(affinity - 1.0D) > 0.05D) why.append(String.format(", personality x%.2f", affinity));
            if (calendarBias != 0) why.append(String.format(", calendar %+.0f", calendarBias));
            if (emotionBias != 0) why.append(String.format(", mood %+.0f", emotionBias));
            if (externalBias != 0) why.append(String.format(", life %+.0f", externalBias));
            if (profile.energy() < 0 && fatigueNeed > 0) {
                double tired = 1.0D - Math.min(0.85D, settings.energyInfluence() * 0.6D * fatigueNeed / 100.0D);
                score *= tired;
                why.append(String.format(", tired x%.2f", tired));
            }
            if (profile.restorative()) {
                double need = Math.max(fatigueNeed, energy.urgency(in.energy(), EnergyNeed.REFUEL));
                score += settings.energyInfluence() * 0.3D * need;
            }
            if (cooldownKey(routine).equals(in.currentKey())) {
                score += settings.stickiness();
                why.append(", already doing it");
            }
            out.add(new Candidate(Intent.of(routine, place), PriorityLayer.BASELINE, Source.ROUTINE, score, why.toString()));
        }
        addNeeds(in, out);
        addEmotions(in, out);
        return out;
    }

    private void addNeeds(Input in, List<Candidate> out) {
        for (EnergyNeed need : EnergyNeed.values()) {
            double urgency = energy.urgency(in.energy(), need);
            if (urgency < 40.0D) continue;
            RoutineType remedy = need.remedy();
            if (!in.cooldowns().ready(cooldownKey(remedy), in.now())) continue;
            Place place = in.places().apply(remedy);
            if (place == null) continue;
            double affinity = personality.affinity(in.traits(), remedy);
            double score = settings.personalThreshold() + urgency * 0.6D * settings.energyInfluence() * (1.0D + settings.personalityInfluence() * (affinity - 1.0D));
            if (cooldownKey(remedy).equals(in.currentKey())) score += settings.stickiness();
            out.add(new Candidate(Intent.of(remedy, place), PriorityLayer.PERSONAL, Source.NEED, score,
                    String.format("%s need %.0f%% (%s)", need, urgency, in.energy())));
        }
    }

    private void addEmotions(Input in, List<Candidate> out) {
        for (RoutineType routine : RoutineType.values()) {
            double bias = in.emotion().bias(routine);
            if (bias < 20.0D || !in.cooldowns().ready(cooldownKey(routine), in.now())) continue;
            Place place = in.places().apply(routine);
            if (place == null) continue;
            out.add(new Candidate(Intent.of(routine, place), PriorityLayer.PERSONAL, Source.EMOTION, settings.personalThreshold() + bias,
                    String.format("feeling %s pulls towards %s (%+.0f)", in.emotion().mood(), routine, bias)));
        }
    }

    public static String cooldownKey(RoutineType routine) { return "R:" + routine; }

    /** How long a new routine will last: between its minimum and maximum, longer for patient personalities, stable per seed. */
    public int duration(RoutineType routine, PersonalityTraits traits, long seed) {
        RoutineProfile p = profiles.of(routine);
        double spread = p.maxTicks() - p.minTicks();
        double unit = ((seed * 0x9E3779B97F4A7C15L) >>> 40) / (double) (1L << 24);
        double patient = 0.5D + 0.25D * traits.lean(yadi.samuraiai.ai.scheduler.personality.Trait.PATIENCE);
        return (int) Math.round(p.minTicks() + spread * Math.max(0.0D, Math.min(1.0D, 0.5D * unit + 0.5D * patient)));
    }
}
