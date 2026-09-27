package yadi.samuraiai.living.village.market;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.living.core.DayPhase;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.life.VillageEventRecord;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.security.SecurityState;

/**
 * Market life: the market as a social place. It opens in its hours unless an attack closes it (a special market or a
 * festival keeps it open into the evening), gives each merchant citizen a stall around the market, and counts footfall —
 * citizens whose day has them at the market plus visitors doing business. Prices and goods are the Economy's; it reads
 * {@link Village#marketOpen()} and the footfall as demand.
 */
public final class MarketLifeEngine {
    public enum Change { NONE, OPENED, CLOSED }

    public Change update(Village village, DayPhase phase, List<VillageEventRecord> events, List<UUID> merchants, int customers) {
        Building market = village.market() == null ? null : village.buildings().get(village.market());
        boolean closes = false, extends_ = false;
        for (VillageEventRecord e : events) { closes |= e.kind().closesMarket(); extends_ |= e.kind().extendsMarket(); }
        boolean open = market != null && market.usable() && !closes && village.security().state() != SecurityState.ATTACK
                && (market.openAt(phase) || (extends_ && (phase == DayPhase.SUNSET || phase == DayPhase.NIGHT)));
        if (market != null) assignStalls(village, market, merchants);
        village.marketFootfall(open ? customers + merchants.size() : 0);
        if (open == village.marketOpen()) return Change.NONE;
        village.marketOpen(open);
        return open ? Change.OPENED : Change.CLOSED;
    }

    private static void assignStalls(Village village, Building market, List<UUID> merchants) {
        village.stalls().keySet().removeIf(m -> !merchants.contains(m));
        int i = 0;
        for (UUID m : merchants) {
            double angle = 2 * Math.PI * i / Math.max(1, merchants.size());
            village.stalls().putIfAbsent(m, new double[]{market.x() + Math.cos(angle) * market.radius() * 0.7D, market.y(), market.z() + Math.sin(angle) * market.radius() * 0.7D});
            i++;
        }
    }
}
