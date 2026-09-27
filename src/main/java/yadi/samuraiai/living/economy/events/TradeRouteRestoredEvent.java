package yadi.samuraiai.living.economy.events;

/** A trade route is usable again. */
public record TradeRouteRestoredEvent(long minute, java.util.UUID routeId, java.util.UUID origin, java.util.UUID destination) implements EconomyEngineEvent { }
