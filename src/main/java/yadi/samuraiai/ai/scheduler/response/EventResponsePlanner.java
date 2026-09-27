package yadi.samuraiai.ai.scheduler.response;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInfluence;
import yadi.samuraiai.ai.scheduler.energy.EnergyModel;
import yadi.samuraiai.ai.scheduler.energy.EnergyState;
import yadi.samuraiai.ai.scheduler.engine.Candidate;
import yadi.samuraiai.ai.scheduler.engine.Intent;
import yadi.samuraiai.ai.scheduler.engine.Perceived;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.engine.Source;
import yadi.samuraiai.ai.scheduler.group.Alarm;
import yadi.samuraiai.ai.scheduler.group.GroupRole;
import yadi.samuraiai.ai.scheduler.personality.PersonalityEngine;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;
import yadi.samuraiai.ai.scheduler.zone.Place;

/**
 * The event response planner: given what the NPC perceived (a sound to check, a threat, a wound) or was told (a group alarm),
 * and who it is (personality, role, condition, whether it can fight), it proposes how to respond (investigate, flee,
 * assist, watch) as candidates in the situational or emergency layer, and says whether to raise the alarm for its group. It
 * proposes; it does not act and it does not choose.
 */
public final class EventResponsePlanner {
    /** What the planner reads about one NPC. */
    public record Input(String dimension, double x, double y, double z, Perceived perceived, boolean canFight, GroupRole role, Alarm groupAlarm,
                        Place home, EnergyState energy, EmotionInfluence emotion, PersonalityTraits traits, boolean inGroup) { }

    /** A request to warn the group: how serious and where. */
    public record AlarmRequest(int level, double x, double y, double z) { }

    /** The planner's proposals. */
    public record Plan(List<Candidate> candidates, AlarmRequest alarm) { }

    private final SchedulerSettings settings;
    private final PersonalityEngine personality;
    private final EnergyModel energy;

    public EventResponsePlanner(SchedulerSettings settings, PersonalityEngine personality, EnergyModel energy) {
        this.settings = settings; this.personality = personality; this.energy = energy;
    }

    public Plan plan(Input in) {
        List<Candidate> out = new ArrayList<>();
        Perceived p = in.perceived();
        GroupRole role = in.role() == null ? GroupRole.LEADER : in.role();
        double fatigue = energy.exhausted(in.energy()) ? 0.6D : 1.0D;
        double calm = energy.overstressed(in.energy()) ? 0.8D : 1.0D;
        boolean grave = p.threatLevel() >= 2 || p.damaged() || in.emotion().panic();

        if (grave) {
            double severity = p.threatLevel() >= 3 ? 1.0D : p.threatLevel() == 2 ? 0.75D : 0.6D;
            double flee = settings.fleeBase() * personality.responseBias(in.traits(), ResponseKind.FLEE) * role.flee() * (0.5D + 0.5D * severity) * (in.emotion().panic() ? 1.15D : 1.0D);
            out.add(new Candidate(Intent.respond(ResponseKind.FLEE, fleePlace(in, p.threatX(), p.threatZ())), PriorityLayer.EMERGENCY, Source.SURVIVAL, flee,
                    String.format("threat %d (%.0f)%s%s", p.threatLevel(), p.threatScore(), p.damaged() ? ", wounded" : "", in.emotion().panic() ? ", panic" : "")));
            if (in.canFight()) {
                double assist = (settings.assistBase() + 25.0D) * personality.responseBias(in.traits(), ResponseKind.ASSIST) * role.assist() * (0.5D + 0.5D * severity) * fatigue;
                Place at = p.hasThreatPosition() ? new Place(in.dimension(), p.threatX(), p.threatY(), p.threatZ(), 4.0D, null) : new Place(in.dimension(), in.x(), in.y(), in.z(), 4.0D, null);
                out.add(new Candidate(Intent.respond(ResponseKind.ASSIST, at), PriorityLayer.EMERGENCY, Source.SURVIVAL, assist, "can fight; threat " + p.threatLevel()));
            }
        } else if (p.investigation() != null) {
            var target = p.investigation();
            double score = settings.investigateBase() * personality.responseBias(in.traits(), ResponseKind.INVESTIGATE) * role.investigate()
                    * (0.6D + 0.8D * Math.min(1.0D, target.urgency() / 100.0D)) * fatigue * calm;
            score /= Math.sqrt(personality.responseBias(in.traits(), ResponseKind.IGNORE));
            double radius = Math.max(1.5D, Math.min(6.0D, target.uncertainty() + 1.5D));
            out.add(new Candidate(Intent.respond(ResponseKind.INVESTIGATE, new Place(in.dimension(), target.x(), target.y(), target.z(), radius, null)),
                    PriorityLayer.SITUATIONAL, Source.EVENT, score, String.format("something at %.0f,%.0f (urgency %.0f, +/-%.1f)", target.x(), target.z(), target.urgency(), target.uncertainty())));
        } else if (p.suspicious() || p.threatLevel() == 1) {
            double score = settings.watchBase() * personality.responseBias(in.traits(), ResponseKind.WATCH) * (1.0D + p.suspicion() / 200.0D);
            out.add(new Candidate(Intent.respond(ResponseKind.WATCH, new Place(in.dimension(), in.x(), in.y(), in.z(), 2.0D, null)), PriorityLayer.SITUATIONAL,
                    Source.EVENT, score, String.format("uneasy (suspicion %.0f)", p.suspicion())));
        }

        Alarm alarm = in.groupAlarm();
        if (alarm != null && !grave) {
            if (in.canFight()) {
                double score = settings.assistBase() * personality.responseBias(in.traits(), ResponseKind.ASSIST) * role.assist() * (0.6D + 0.2D * alarm.level()) * fatigue;
                PriorityLayer layer = alarm.level() >= 3 ? PriorityLayer.EMERGENCY : PriorityLayer.SITUATIONAL;
                out.add(new Candidate(Intent.respond(ResponseKind.ASSIST, new Place(in.dimension(), alarm.x(), alarm.y(), alarm.z(), 4.0D, null)), layer, Source.GROUP,
                        alarm.level() >= 3 ? score + 20.0D : score, "group alarm level " + alarm.level()));
            } else if (alarm.level() >= 2) {
                double score = settings.fleeBase() * 0.6D * personality.responseBias(in.traits(), ResponseKind.FLEE) * role.flee();
                out.add(new Candidate(Intent.respond(ResponseKind.FLEE, fleePlace(in, alarm.x(), alarm.z())), PriorityLayer.SITUATIONAL, Source.GROUP, score, "group alarm level " + alarm.level()));
            } else {
                out.add(new Candidate(Intent.respond(ResponseKind.WATCH, new Place(in.dimension(), in.x(), in.y(), in.z(), 2.0D, null)), PriorityLayer.SITUATIONAL, Source.GROUP,
                        settings.watchBase() * 1.2D, "group is uneasy"));
            }
        }
        return new Plan(out, alarmRequest(in, p));
    }

    private AlarmRequest alarmRequest(Input in, Perceived p) {
        if (!in.inGroup() || (p.threatLevel() < 1 && !p.damaged())) return null;
        if (personality.responseBias(in.traits(), ResponseKind.RAISE_ALARM) < 0.8D) return null;
        int level = Math.min(3, Math.max(1, p.threatLevel() + (p.damaged() ? 1 : 0)));
        return p.hasThreatPosition() ? new AlarmRequest(level, p.threatX(), p.threatY(), p.threatZ()) : new AlarmRequest(level, in.x(), in.y(), in.z());
    }

    /** Away from the danger, or home if home is the safer side of it; with no idea where the danger is, home or a step aside. */
    private Place fleePlace(Input in, double tx, double tz) {
        double distance = settings.fleeDistance();
        boolean known = !Double.isNaN(tx) && !Double.isNaN(tz);
        if (known) {
            if (in.home() != null && Math.hypot(in.home().x() - tx, in.home().z() - tz) > Math.hypot(in.x() - tx, in.z() - tz) + 4.0D)
                return in.home().withRadius(Math.max(3.0D, in.home().radius()));
            double ax = in.x() - tx, az = in.z() - tz, n = Math.hypot(ax, az);
            if (n < 1e-6) { ax = 1; az = 0; n = 1; }
            return new Place(in.dimension(), in.x() + ax / n * distance, in.y(), in.z() + az / n * distance, 3.0D, null);
        }
        if (in.home() != null) return in.home().withRadius(Math.max(3.0D, in.home().radius()));
        return new Place(in.dimension(), in.x() + distance, in.y(), in.z(), 3.0D, null);
    }
}
