package yadi.samuraiai.living.village.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.living.core.DayPhase;
import yadi.samuraiai.living.core.Dice;

/**
 * Moves visitors through ARRIVE → STAY → INTERACT → LEAVE → GONE and draws new ones each day from what the village offers:
 * travellers in proportion to traffic and safety, pilgrims to temples on holy days, samurai and monks now and then.
 * Merchants arrive with caravans and messengers with quests (the hub adds those).
 */
public final class VisitorEngine {
    public record Changes(List<VisitorRecord> arrived, List<VisitorRecord> left) { }

    private static final List<String> NAMES = List.of("Hiro", "Kenji", "Aiko", "Sora", "Daichi", "Yuki", "Ren", "Mei", "Taro", "Hana", "Jun", "Rin", "Kaito", "Nami", "Shin", "Emi");

    /** Advances the visits of one village to {@code now}. {@code interactPhase}: whether the market or temple is receiving people. */
    public Changes advance(List<VisitorRecord> visitors, long now, DayPhase phase, boolean interactPhase) {
        List<VisitorRecord> left = new ArrayList<>();
        for (VisitorRecord v : visitors) {
            if (v.phase() == VisitorRecord.Phase.GONE || now < v.arrived()) continue;   // expected later today
            if (now >= v.leaves()) { v.phase(VisitorRecord.Phase.GONE); left.add(v); continue; }
            if (now >= v.leaves() - 60) v.phase(VisitorRecord.Phase.LEAVE);
            else if (now - v.arrived() < 60) v.phase(VisitorRecord.Phase.ARRIVE);
            else v.phase(interactPhase && !phase.dark() ? VisitorRecord.Phase.INTERACT : VisitorRecord.Phase.STAY);
        }
        visitors.removeIf(v -> v.phase() == VisitorRecord.Phase.GONE);
        return new Changes(List.of(), left);
    }

    /**
     * Draws the day's new visitors. {@code traffic} ~ number of roads into the village, {@code safety} 0..1, {@code holy} whether
     * it is a festival or holiday, {@code temple} whether the village has a temple.
     */
    public List<VisitorRecord> draw(UUID village, long dayIndex, long now, Dice dice, double baseRate, int traffic, double safety, boolean holy, boolean temple,
                                    int minStayHours, int maxStayHours, int room) {
        List<VisitorRecord> out = new ArrayList<>();
        if (room <= 0) return out;
        String key = "visitors:" + village;
        double expected = baseRate * (0.5D + traffic) * Math.max(0.1D, safety) * (holy ? 1.8D : 1.0D);
        int count = (int) Math.floor(expected) + (dice.chance(key + ":extra", dayIndex, expected - Math.floor(expected)) ? 1 : 0);
        for (int i = 0; i < Math.min(count, room); i++) {
            VisitorRecord.Kind kind;
            double r = dice.unit(key + ":kind", dayIndex * 31 + i);
            if (holy && temple && r < 0.5D) kind = VisitorRecord.Kind.PILGRIM;
            else if (r < 0.55D) kind = VisitorRecord.Kind.TRAVELER;
            else if (r < 0.7D) kind = VisitorRecord.Kind.MONK;
            else if (r < 0.85D) kind = VisitorRecord.Kind.SAMURAI;
            else kind = VisitorRecord.Kind.PILGRIM;
            long arrive = now + dice.below(key + ":at", dayIndex * 31 + i, 12 * 60);
            long stay = (long) (dice.between(key + ":stay", dayIndex * 31 + i, minStayHours, maxStayHours + 1) * 60);
            String name = NAMES.get(dice.below(key + ":name", dayIndex * 31 + i, NAMES.size()));
            UUID id = UUID.nameUUIDFromBytes((key + ":" + dayIndex + ":" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.add(new VisitorRecord(id, kind, name, null, null, arrive, arrive + stay, switch (kind) {
                case PILGRIM -> "peregrinación"; case MONK -> "enseñanza"; case SAMURAI -> "de paso"; case MERCHANT -> "comercio"; case MESSENGER -> "mensaje"; default -> "viaje";
            }));
        }
        return out;
    }

    public static String randomName(Dice dice, String key, long step) { return NAMES.get(dice.below(key, step, NAMES.size())); }
}
