package yadi.samuraiai.living.economy.events;

/** A contract was fulfilled, failed, expired or cancelled. */
public record ContractResolvedEvent(long minute, java.util.UUID contractId, String state, double delivered) implements EconomyEngineEvent { }
