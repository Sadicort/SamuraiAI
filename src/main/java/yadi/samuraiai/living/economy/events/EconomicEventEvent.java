package yadi.samuraiai.living.economy.events;

/** An economic event affected a settlement: GREAT_HARVEST, BAD_HARVEST, FIRE, SPECIAL_MARKET, LOST_CARAVAN, BLOCKED_ROUTE, FESTIVAL. */
public record EconomicEventEvent(long minute, java.util.UUID settlementId, String kind, String detail) implements EconomyEngineEvent { }
