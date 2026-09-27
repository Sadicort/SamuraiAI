package yadi.samuraiai.living.economy.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.economy.caravans.Caravan;
import yadi.samuraiai.living.economy.contracts.Contract;
import yadi.samuraiai.living.economy.engine.EconomyEngine;
import yadi.samuraiai.living.economy.inventory.ResourceLot;
import yadi.samuraiai.living.economy.ledger.MovementRecord;
import yadi.samuraiai.living.economy.metrics.EconomyMetrics;
import yadi.samuraiai.living.economy.prices.PricePoint;
import yadi.samuraiai.living.economy.scarcity.MarketBalance;
import yadi.samuraiai.living.economy.simulation.SettlementEconomy;
import yadi.samuraiai.living.economy.storage.Warehouse;
import yadi.samuraiai.living.economy.trade_routes.TradeRoute;
import yadi.samuraiai.living.economy.traders.Merchant;
import yadi.samuraiai.living.economy.wealth.WealthAccount;
import yadi.samuraiai.living.economy.wealth.WealthEngine;

/** The Economy Overlay in text form: production, consumption, stock and provenance, prices, balances, merchants, caravans, routes, contracts, ledger. */
public final class EconomyInspector {
    private EconomyInspector() { }

    public static List<String> settlement(EconomyEngine e, UUID id) {
        List<String> out = new ArrayList<>();
        SettlementEconomy se = e.settlement(id).orElse(null);
        if (se == null) { out.add("Sin economía."); return out; }
        Warehouse w = e.warehouse(se.warehouse()).orElseThrow();
        WealthAccount t = e.treasury(id);
        out.add(String.format("%s — tesoro %.0f monedas, prosperidad %.2f (%s), comida para %.1f días, mercado %s", se.name(), t == null ? 0 : t.coins(), se.prosperity(),
                WealthEngine.level(t == null ? 0 : t.coins(), 100), se.foodCoverDays(), se.market() == null ? "no" : se.market().open() ? "abierto" : "cerrado"));
        StringBuilder stock = new StringBuilder("Almacén (" + String.format("%.0f/%.0f", w.inventory().weight(r -> e.resources().get(r).map(d -> d.weight()).orElse(1.0)), w.capacity()) + "): ");
        w.inventory().totals().forEach((r, q) -> stock.append(String.format("%s %.1f  ", r, q)));
        out.add(stock.toString().trim());
        out.add("Hoy producido " + round(se.produced()) + "; consumido " + round(se.consumed()) + "; importado " + round(se.imported()) + "; exportado " + round(se.exported()) + "; faltó " + round(se.shortfall()));
        StringBuilder bal = new StringBuilder("Balance: ");
        for (MarketBalance b : se.balances().values()) if (b.state() != MarketBalance.State.NORMAL) bal.append(String.format("%s %s (%.1f d)  ", b.resource(), b.state(), b.coverDays()));
        out.add(bal.length() > 9 ? bal.toString().trim() : "Balance: todo normal");
        out.add(String.format("Impuestos cobrados: mercado %.1f, diezmo %.1f, tributo %.1f", se.taxes().totalMarket(), se.taxes().totalTithe(), se.taxes().totalLevy()));
        return out;
    }

    public static List<String> prices(EconomyEngine e, UUID id) {
        List<String> out = new ArrayList<>();
        for (PricePoint p : e.prices(id).values()) {
            double base = e.resources().get(p.resource()).map(d -> d.baseValue()).orElse(1.0);
            out.add(String.format("%s: %.2f (base %.2f) oferta %.0f demanda %.0f %s", p.resource(), p.price(), base, p.supply(), p.demand(), p.factors().isEmpty() ? "" : fmt(p.factors())));
        }
        if (out.isEmpty()) out.add("Sin precios aún.");
        return out;
    }

    public static List<String> provenance(EconomyEngine e, UUID settlement, String resource) {
        List<String> out = new ArrayList<>();
        Warehouse w = e.storeOf(settlement);
        if (w == null) return List.of("Sin almacén.");
        for (ResourceLot l : w.inventory().lotsOf(resource))
            out.add(String.format("%.1f %s calidad %.2f — origen %s, producido %s%s", l.quantity(), resource, l.quality(), l.origin().label(), e.clock().date(l.producedAt()).shortDate(),
                    l.originSettlement() == null || l.originSettlement().equals(settlement) ? "" : " (importado)"));
        if (out.isEmpty()) out.add("No hay " + resource + ".");
        return out;
    }

    public static List<String> merchants(EconomyEngine e) {
        List<String> out = new ArrayList<>();
        for (Merchant m : e.merchants()) {
            double coins = e.wealth().get(m.account()).map(WealthAccount::coins).orElse(0.0);
            out.add(String.format("%s (%s%s) monedas %.0f, reputación %.0f, tratos %d, beneficio %.0f, %s", m.name(), m.personality(), m.embodied() ? ", NPC" : ", casa", coins, m.reputation(),
                    m.trades(), m.profit(), m.caravan() == null ? "en casa" : "de viaje"));
        }
        if (out.isEmpty()) out.add("Sin mercaderes.");
        return out;
    }

    public static List<String> caravans(EconomyEngine e) {
        List<String> out = new ArrayList<>();
        for (Caravan c : e.caravans()) {
            Warehouse cargo = e.warehouse(c.cargo()).orElse(null);
            out.add(String.format("%s %s [%s] %.0f bloques recorridos, tramo %d, guardias %d, carros %d, carga %s — %s", c.id().toString().substring(0, 8), c.type(), c.state(), c.progress(), c.legIndex(),
                    c.guards(), c.carts(), cargo == null ? "-" : round(cargo.inventory().totals()), c.note()));
        }
        if (out.isEmpty()) out.add("Sin caravanas.");
        return out;
    }

    public static List<String> routes(EconomyEngine e) {
        List<String> out = new ArrayList<>();
        for (TradeRoute r : e.routes())
            out.add(String.format("%s → %s: %s, %.0f bloques, peligro %.2f, puentes %d, caravanas %d/%d (perdidas %d), valor %.0f", e.settlement(r.origin()).map(SettlementEconomy::name).orElse("?"),
                    e.settlement(r.destination()).map(SettlementEconomy::name).orElse("?"), r.status(), r.distance(), r.danger(), r.bridges(), r.arrived(), r.sent(), r.lost(), r.valueCarried()));
        if (out.isEmpty()) out.add("Sin rutas comerciales.");
        return out;
    }

    public static List<String> contracts(EconomyEngine e) {
        List<String> out = new ArrayList<>();
        for (Contract c : e.contracts())
            out.add(String.format("%s %s %.0f %s a %.2f en %s [%s] entregado %.0f, vence %s — %s", c.id().toString().substring(0, 8), c.kind(), c.quantity(), c.resource(), c.unitPrice(),
                    e.settlement(c.settlement()).map(SettlementEconomy::name).orElse("?"), c.state(), c.delivered(), e.clock().date(c.deadline()).shortDate(), c.reason()));
        if (out.isEmpty()) out.add("Sin contratos.");
        return out;
    }

    public static List<String> ledger(EconomyEngine e, UUID settlement, int limit) {
        List<String> out = new ArrayList<>();
        for (MovementRecord r : settlement == null ? e.ledger().recent(limit) : e.ledger().of(settlement, limit))
            out.add(String.format("%s %s %.2f %s (%.1f) %s%s", e.clock().date(r.minute()).shortDate(), r.kind(), r.quantity(), r.resource(), r.value(), r.reference(), r.origin() == null ? "" : " ← " + r.origin().label()));
        return out;
    }

    public static List<String> metrics(EconomyEngine e) {
        EconomyMetrics.Snapshot m = e.metrics().snapshot();
        return List.of(
                String.format("Pasos %d (%.1f µs), producción %d lotes, producido %.0f, consumido %.0f, precios %d (cambios %d)", m.steps(), m.stepMicros(), m.productionRuns(), m.produced(), m.consumed(),
                        m.priceUpdates(), m.priceChanges()),
                String.format("Caravanas %d (llegadas %d, perdidas %d, emboscadas %d), escaseces %d, excedentes %d, ventas %d, contratos %d (cumplidos %d), planes %d (%.2f ms)",
                        m.caravansCreated(), m.caravansArrived(), m.caravansLost(), m.ambushes(), m.shortages(), m.surpluses(), m.trades(), m.contracts(), m.contractsFulfilled(), m.tradePlans(), m.planMillis()),
                String.format("Movimientos registrados %d; monedas acuñadas %.0f", e.ledger().recorded(), e.wealth().minted()));
    }

    private static String round(Map<String, Double> m) {
        StringBuilder sb = new StringBuilder("{");
        m.forEach((k, v) -> sb.append(k).append(' ').append(String.format("%.1f", v)).append(", "));
        if (sb.length() > 1) sb.setLength(sb.length() - 2);
        return sb.append('}').toString();
    }

    private static String fmt(Map<String, Double> f) {
        StringBuilder sb = new StringBuilder("[");
        f.forEach((k, v) -> { if (Math.abs(v - 1.0) > 0.01) sb.append(k).append(String.format(" x%.2f ", v)); });
        return sb.toString().trim() + "]";
    }
}
