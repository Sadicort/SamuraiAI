package yadi.samuraiai.living.quest.generator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.quest.api.QuestPorts;
import yadi.samuraiai.living.quest.conditions.WorldCondition;
import yadi.samuraiai.living.quest.consequences.ConsequenceSpec;
import yadi.samuraiai.living.quest.objectives.ObjectiveSpec;
import yadi.samuraiai.living.quest.objectives.QuestObjective;
import yadi.samuraiai.living.quest.rewards.RewardSpec;
import yadi.samuraiai.living.quest.runtime.Quest;
import yadi.samuraiai.living.quest.templates.Expr;
import yadi.samuraiai.living.quest.templates.QuestTemplate;

/**
 * The quest generator: WORLD CONDITION → ANALYZER → TEMPLATE → VARIABLES → FINAL QUEST. The analyzer (in the engine) decides
 * whether a condition deserves a quest; here a template that fits the condition, the season and the culture is chosen by
 * weight and severity, its variables are bound from the condition and the world (settlement, region, resource and its price,
 * the giver, weather, moon, neighbours, places), a giver with a fitting profession is found among the settlement's citizens,
 * and the objectives are placed in the world. A quest without a voice (nobody to give it) is not created.
 */
public final class QuestGenerator {
    public record Ports(QuestPorts.World world, QuestPorts.Villages villages, QuestPorts.Economy economy) { }

    private static final Map<String, String> PROFESSION_NAMES = Map.ofEntries(Map.entry("blacksmith", "El herrero"), Map.entry("carpenter", "El carpintero"),
            Map.entry("cook", "El cocinero"), Map.entry("farmer", "El campesino"), Map.entry("merchant", "El mercader"), Map.entry("monk", "El monje"), Map.entry("guard", "La guardia"),
            Map.entry("samurai", "El samurái"), Map.entry("fisherman", "El pescador"));

    public Optional<QuestTemplate> choose(List<QuestTemplate> candidates, WorldCondition c, String season, String culture, Dice dice, long step) {
        List<QuestTemplate> fitting = candidates.stream().filter(t -> t.weight() > 0 && t.fits(yadi.samuraiai.living.core.Season.parse(season).orElse(null), culture)).toList();
        if (fitting.isEmpty()) return Optional.empty();
        double[] w = new double[fitting.size()];
        for (int i = 0; i < w.length; i++) w[i] = fitting.get(i).weight() * (0.5D + c.severity());
        int pick = dice.weighted("quest-template:" + c.key(), step, w);
        return pick < 0 ? Optional.empty() : Optional.of(fitting.get(pick));
    }

    public Map<String, String> variables(QuestTemplate t, WorldCondition c, Ports p) {
        Map<String, String> v = new LinkedHashMap<>(c.variables());
        if (c.settlement() != null) { v.putIfAbsent("settlementId", c.settlement().toString()); v.put("settlement", p.world().settlementName(c.settlement())); }
        if (c.region() != null) { v.putIfAbsent("regionId", c.region().toString()); v.put("regionName", p.world().regionName(c.region())); }
        String resource = v.getOrDefault("resource", c.subject().isEmpty() ? "rice" : c.subject());
        v.put("resource", resource);
        v.putIfAbsent("resourceName", resource);
        double quantity = v.containsKey("quantity") ? Expr.number(v.get("quantity"), Map.of()) : Math.round(10 + c.severity() * 30);
        v.put("quantity", String.valueOf((long) Math.max(1, Math.round(quantity))));
        double price = c.settlement() == null ? 1 : p.economy().price(c.settlement(), p.economy().isFood(resource) || resource.equals("food") ? "rice" : resource);
        v.put("value", String.format(java.util.Locale.ROOT, "%.1f", Math.max(1, quantity * price)));
        v.put("severity", String.format(java.util.Locale.ROOT, "%.2f", c.severity()));
        v.put("threats", String.valueOf(2 + Math.round(c.severity() * 4)));
        v.put("stone", String.valueOf((long) Math.max(1, Math.ceil(quantity / 2))));
        v.put("season", yadi.samuraiai.living.core.Season.parse(p.world().seasonName()).map(yadi.samuraiai.living.core.CalendarDate::seasonName).orElse(p.world().seasonName()));
        v.put("weather", c.region() == null ? "" : p.world().weatherName(c.region()));
        v.put("moon", p.world().moonName());
        v.putIfAbsent("product", "tools");
        v.putIfAbsent("productQuantity", "1");
        if (c.settlement() != null) p.world().neighbour(c.settlement()).ifPresent(n -> { v.put("neighbourId", n.toString()); v.put("neighbour", p.world().settlementName(n)); });
        if (v.containsKey("destination")) {
            try { v.put("destinationName", p.world().settlementName(UUID.fromString(v.get("destination")))); } catch (IllegalArgumentException ignored) { v.putIfAbsent("destinationName", v.get("destination")); }
        }
        v.putIfAbsent("destinationName", v.getOrDefault("neighbour", "la aldea vecina"));
        v.putIfAbsent("placeName", v.getOrDefault("regionName", "los alrededores"));
        v.putIfAbsent("secret", "dónde crecen las mejores hierbas de " + v.getOrDefault("regionName", "la región"));
        v.putIfAbsent("festival", "la fiesta");
        return v;
    }

    /** Builds the quest, or empty when nobody in the settlement can give it. */
    public Optional<Quest> instantiate(QuestTemplate t, WorldCondition c, Ports p, long now, int minutesPerDay) {
        Map<String, String> v = variables(t, c, p);
        QuestPorts.Giver giver = null;
        if (v.containsKey("giver")) {
            try { giver = new QuestPorts.Giver(UUID.fromString(v.get("giver")), v.getOrDefault("giverName", "?"), v.getOrDefault("giverProfession", "")); } catch (IllegalArgumentException ignored) { }
        }
        if (giver == null && c.settlement() != null) giver = p.villages().giver(c.settlement(), t.giverProfessions()).orElse(null);
        if (giver == null) return Optional.empty();
        v.put("giver", giver.name());
        v.put("giverId", giver.npc().toString());
        v.put("giverProfession", giver.profession());
        v.put("giverProfessionName", PROFESSION_NAMES.getOrDefault(giver.profession(), giver.name()));
        Quest q = new Quest(UUID.randomUUID(), t.id(), t.category(), Expr.fill(t.title(), v), c.kind(), c.key(), c.severity(), c.cause(), giver.npc(), giver.name(), giver.profession(),
                c.settlement(), c.region(), now, now + (long) t.offerDays() * minutesPerDay, t.durationDays());
        q.variables().putAll(v);
        for (var e : t.story().entrySet()) q.story().put(e.getKey(), Expr.fill(e.getValue(), v));
        for (ObjectiveSpec o : t.objectives()) {
            Optional<QuestPorts.Place> place = place(o.place(), c, v, p);
            String target = o.target().equals("giver") ? giver.npc().toString() : Expr.fill(o.target(), v);
            double required = Math.max(1, Expr.number(o.quantity(), v));
            q.objectives().add(new QuestObjective(UUID.randomUUID(), o.type(), Expr.fill(o.description(), v), target, place.map(QuestPorts.Place::dimension).orElse(""),
                    place.map(QuestPorts.Place::x).orElse(Double.NaN), place.map(QuestPorts.Place::y).orElse(Double.NaN), place.map(QuestPorts.Place::z).orElse(Double.NaN),
                    o.radius(), required, o.optional(), o.branch()));
        }
        q.branches().addAll(t.branches());
        for (RewardSpec r : t.rewards())
            q.rewards().add(new RewardSpec(r.kind(), String.format(java.util.Locale.ROOT, "%.3f", Expr.number(r.amount(), v)), Expr.fill(r.target(), v), Expr.fill(r.text(), v)));
        for (ConsequenceSpec s : t.consequences())
            q.consequences().add(new ConsequenceSpec(s.kind(), s.target(), String.format(java.util.Locale.ROOT, "%.3f", Expr.number(s.amount(), v)), Expr.fill(s.text(), v), s.onSuccess(), s.onFailure(), s.branch()));
        return Optional.of(q);
    }

    private Optional<QuestPorts.Place> place(String key, WorldCondition c, Map<String, String> v, Ports p) {
        if (key == null || key.isEmpty()) return Optional.empty();
        Optional<QuestPorts.Place> home = c.settlement() == null ? Optional.empty() : p.world().settlementPlace(c.settlement());
        return switch (key) {
            case "settlement" -> home;
            case "settlement_north" -> home.map(h -> new QuestPorts.Place(h.dimension(), h.x(), h.y(), h.z() - 48));
            case "settlement_south" -> home.map(h -> new QuestPorts.Place(h.dimension(), h.x(), h.y(), h.z() + 48));
            case "temple" -> c.settlement() == null ? Optional.empty() : p.villages().building(c.settlement(), "TEMPLE").or(() -> home);
            case "region" -> c.region() == null ? home : p.world().regionCenter(c.region());
            case "destination", "neighbour" -> {
                String id = v.getOrDefault(key.equals("destination") ? "destination" : "neighbourId", "");
                try { yield id.isEmpty() ? Optional.empty() : p.world().settlementPlace(UUID.fromString(id)); } catch (IllegalArgumentException e) { yield Optional.empty(); }
            }
            case "route", "place", "event", "detour" -> {
                if (v.containsKey("x") && v.containsKey("z") && home.isPresent()) {
                    double off = key.equals("detour") ? 60 : 0;
                    yield Optional.of(new QuestPorts.Place(v.getOrDefault("dimension", home.get().dimension()), Expr.number(v.get("x"), Map.of()) + off, home.get().y(), Expr.number(v.get("z"), Map.of()) + off));
                }
                yield c.region() != null ? p.world().regionCenter(c.region()) : home;
            }
            default -> home;
        };
    }
}
