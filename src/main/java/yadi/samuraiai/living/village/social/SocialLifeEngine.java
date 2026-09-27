package yadi.samuraiai.living.village.social;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.security.SecurityState;

/**
 * Social life: how lively the village is (people whose day has them socialising, scaled by the season, festivals and safety)
 * and who gathers with whom — neighbours, colleagues of the same trade, guards with guards, monks with monks. When a
 * citizen's group is out socialising, the citizen is drawn to join (a small SOCIAL bias). What they say and feel stays with
 * the cognitive layer; this only decides that they meet.
 */
public final class SocialLifeEngine {
    /** Village-wide social activity: socialisers weighted by season, festival and security. */
    public double activity(Village village, int socialisers, double seasonSocial, double festivalSocial) {
        double safety = switch (village.security().state()) { case PEACE -> 1.0D; case RECOVERY -> 0.7D; case ALERT -> 0.6D; case DANGER -> 0.2D; case ATTACK -> 0.0D; };
        double value = socialisers * seasonSocial * Math.max(1.0D, festivalSocial) * safety;
        village.socialActivity(value);
        return value;
    }

    /** The social pull on one citizen: how many of its neighbours and colleagues are socialising now. */
    public double pull(UUID citizen, List<UUID> neighbours, Map<UUID, String> professions, Map<UUID, String> plannedNow, double perFriend, double cap) {
        String trade = professions.getOrDefault(citizen, "");
        int friends = 0;
        for (UUID n : neighbours) if ("SOCIAL".equals(plannedNow.get(n))) friends++;
        for (var e : plannedNow.entrySet())
            if (!e.getKey().equals(citizen) && "SOCIAL".equals(e.getValue()) && !trade.isEmpty() && trade.equals(professions.get(e.getKey()))) friends++;
        return Math.min(cap, friends * perFriend);
    }

    public static boolean safeToGather(Village village) { return village.security().state() != SecurityState.ATTACK && village.security().state() != SecurityState.DANGER; }
}
