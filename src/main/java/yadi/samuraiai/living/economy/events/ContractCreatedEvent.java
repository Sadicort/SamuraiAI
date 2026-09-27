package yadi.samuraiai.living.economy.events;

/** A contract was offered. */
public record ContractCreatedEvent(long minute, java.util.UUID contractId, String kind, java.util.UUID settlementId, String resource, double quantity, double unitPrice, long deadline, String reason) implements EconomyEngineEvent { }
