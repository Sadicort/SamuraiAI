package yadi.samuraiai.living.economy.wealth;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.living.core.LivingIds;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.economy.ledger.EconomyLedger;
import yadi.samuraiai.living.economy.ledger.MovementRecord;

/**
 * Money. Every coin in the world entered through {@link #mint} with a provenance (a settlement's founding endowment, a sale
 * of goods a player brought from outside the simulation) and moves only by {@link #transfer}, which fails rather than going
 * negative and is recorded in the ledger. Wealth levels (poor, modest, prosperous, rich, noble) come from net worth.
 */
public final class WealthEngine {
    public enum Level { POOR, MODEST, PROSPEROUS, RICH, NOBLE }

    private final Map<UUID, WealthAccount> accounts = new LinkedHashMap<>();
    private final Map<UUID, UUID> byOwner = new LinkedHashMap<>();
    private final EconomyLedger ledger;
    private int windowMinutes = 30 * 1440;
    private double minted;
    private boolean dirty;

    public WealthEngine(EconomyLedger ledger) { this.ledger = ledger; }

    public void configure(int window) { windowMinutes = Math.max(1440, window); }

    /** The account of an owner, created on first use. */
    public WealthAccount account(WealthAccount.OwnerKind kind, UUID owner, String label, long now) {
        UUID existing = byOwner.get(owner);
        if (existing != null) return accounts.get(existing);
        WealthAccount a = new WealthAccount(LivingIds.named("account", kind + ":" + owner), kind, owner, label, now);
        accounts.put(a.id(), a);
        byOwner.put(owner, a.id());
        dirty = true;
        return a;
    }

    public Optional<WealthAccount> of(UUID owner) { UUID id = byOwner.get(owner); return id == null ? Optional.empty() : Optional.ofNullable(accounts.get(id)); }
    public Optional<WealthAccount> get(UUID id) { return Optional.ofNullable(accounts.get(id)); }
    public Collection<WealthAccount> all() { return List.copyOf(accounts.values()); }
    public double coins(UUID owner) { return of(owner).map(WealthAccount::coins).orElse(0.0D); }
    public double minted() { return minted; }

    /** Creates coins with a provenance (the only way money enters the world). */
    public void mint(WealthAccount to, double amount, Provenance origin, long now, UUID settlement) {
        if (amount <= 0) return;
        to.credit(amount, now, windowMinutes);
        minted += amount;
        dirty = true;
        ledger.record(now, MovementRecord.Kind.MINTED, "coins", amount, null, to.id(), amount, origin.label(), origin, settlement);
    }

    /** Moves coins. False (and nothing moves) when the payer has not enough. */
    public boolean transfer(WealthAccount from, WealthAccount to, double amount, MovementRecord.Kind kind, String reference, long now, UUID settlement) {
        if (amount <= 0) return true;
        if (from == null || to == null || !from.debit(amount, now, windowMinutes)) return false;
        to.credit(amount, now, windowMinutes);
        dirty = true;
        ledger.record(now, kind, "coins", amount, from.id(), to.id(), amount, reference, null, settlement);
        return true;
    }

    /** Pays as much as possible up to {@code amount}; returns what was paid. */
    public double transferUpTo(WealthAccount from, WealthAccount to, double amount, MovementRecord.Kind kind, String reference, long now, UUID settlement) {
        double pay = Math.min(amount, from == null ? 0 : from.coins());
        return pay > 0 && transfer(from, to, pay, kind, reference, now, settlement) ? pay : 0;
    }

    public static Level level(double netWorth, double perCapitaScale) {
        double w = netWorth / Math.max(1e-9, perCapitaScale);
        if (w >= 50) return Level.NOBLE;
        if (w >= 15) return Level.RICH;
        if (w >= 4) return Level.PROSPEROUS;
        if (w >= 1) return Level.MODEST;
        return Level.POOR;
    }

    public void restore(WealthAccount a) { accounts.put(a.id(), a); byOwner.put(a.owner(), a.id()); }
    public void restoreMinted(double m) { minted = m; }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { accounts.clear(); byOwner.clear(); minted = 0; dirty = false; }
}
