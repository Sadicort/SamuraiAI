package yadi.samuraiai.living.economy.events;

/** A trade route became blocked or unsafe. */
public record TradeRouteDisruptedEvent(long minute, java.util.UUID routeId, java.util.UUID origin, java.util.UUID destination, String reason) implements EconomyEngineEvent { }
