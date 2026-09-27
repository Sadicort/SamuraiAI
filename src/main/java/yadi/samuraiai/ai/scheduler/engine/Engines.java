package yadi.samuraiai.ai.scheduler.engine;

import yadi.samuraiai.ai.scheduler.emotion.EmotionScheduler;
import yadi.samuraiai.ai.scheduler.energy.EnergyModel;
import yadi.samuraiai.ai.scheduler.personality.PersonalityEngine;
import yadi.samuraiai.ai.scheduler.priority.PriorityEngine;
import yadi.samuraiai.ai.scheduler.response.EventResponsePlanner;
import yadi.samuraiai.ai.scheduler.routine.RoutinePlanner;
import yadi.samuraiai.ai.scheduler.routine.RoutineProfiles;
import yadi.samuraiai.ai.scheduler.social.SocialDistance;
import yadi.samuraiai.ai.scheduler.time.WorldCalendar;

/** The stateless engines built from one settings snapshot. They are rebuilt together when the configuration changes. */
public record Engines(SchedulerSettings settings, RoutineProfiles profiles, PersonalityEngine personality, EnergyModel energy, EmotionScheduler emotions,
                      RoutinePlanner planner, PriorityEngine priority, EventResponsePlanner responses, SocialDistance social) {
    public static Engines create(SchedulerSettings settings, WorldCalendar calendar) {
        RoutineProfiles profiles = new RoutineProfiles(settings.routineOverrides());
        PersonalityEngine personality = new PersonalityEngine(settings);
        EnergyModel energy = new EnergyModel(settings);
        return new Engines(settings, profiles, personality, energy, new EmotionScheduler(settings),
                new RoutinePlanner(settings, profiles, personality, energy, calendar), new PriorityEngine(settings),
                new EventResponsePlanner(settings, personality, energy), new SocialDistance(settings, personality));
    }
}
