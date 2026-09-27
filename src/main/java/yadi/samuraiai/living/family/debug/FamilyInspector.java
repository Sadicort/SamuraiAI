package yadi.samuraiai.living.family.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.family.clan.ClanRecord;
import yadi.samuraiai.living.family.engine.FamilyEngine;
import yadi.samuraiai.living.family.heritage.Heirloom;
import yadi.samuraiai.living.family.knowledge.Technique;
import yadi.samuraiai.living.family.legacy.LegacyRecord;
import yadi.samuraiai.living.family.lineage.Lineage;
import yadi.samuraiai.living.family.mentorship.Mentorship;
import yadi.samuraiai.living.family.metrics.FamilyMetrics;
import yadi.samuraiai.living.family.model.KinGender;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.family.naming.NameRecord;
import yadi.samuraiai.living.family.naming.NamingEngine;
import yadi.samuraiai.living.family.naming.cultures.NameCulture;
import yadi.samuraiai.living.family.naming.cultures.NameCultureCatalog;
import yadi.samuraiai.living.family.naming.generator.SurnameGenerator;
import yadi.samuraiai.living.family.registry.FamilyRecord;
import yadi.samuraiai.living.family.succession.SuccessionEngine;

/**
 * The Family Inspector and the Genealogy Viewer in text: a person's family, generation, parents, children, siblings, household,
 * head, profession, mentor, disciples, lineage, reputation, honour, heirlooms and legacy; a family's tree from its founders
 * ({@code ├──}/{@code └──}, as the specification draws it) and a school's tree of masters and disciples.
 */
public final class FamilyInspector {
    private FamilyInspector() { }

    private static String name(FamilyEngine e, UUID id) {
        return e.person(id).map(p -> p.name().full() + (p.state().living() ? "" : " †" + p.state().name().toLowerCase(java.util.Locale.ROOT).replace("_future", ""))).orElse("?");
    }

    private static String names(FamilyEngine e, Set<UUID> ids) {
        List<String> out = new ArrayList<>();
        for (UUID id : ids) out.add(name(e, id));
        return out.isEmpty() ? "—" : String.join(", ", out);
    }

    public static List<String> person(FamilyEngine e, UUID id) {
        List<String> out = new ArrayList<>();
        Person p = e.person(id).orElse(null);
        if (p == null) { out.add("Sin registro familiar."); return out; }
        FamilyRecord f = e.familyOf(id).orElse(null);
        out.add(String.format("%s — %s, generación %d, %.0f años (%s%s), %s", p.name().formal(), f == null ? "sin familia" : "familia " + f.name(), p.generation(), e.age(id), e.stage(id),
                p.birthEstimated() ? ", edad estimada" : "", p.state()));
        out.add("Progenitores: " + names(e, e.parents(id)) + "; hijos: " + names(e, e.children(id)) + "; hermanos: " + names(e, e.siblings(id)) + "; pareja: " + names(e, e.partners(id)));
        out.add("Hogar: " + (p.household() == null ? "—" : e.household(p.household()).map(h -> h.status() + ", " + h.residents().size() + "/" + h.beds() + " camas").orElse("?"))
                + "; jefe de familia: " + (f == null || f.head() == null ? "—" : name(e, f.head())) + "; oficio: " + (p.profession().isEmpty() ? "—" : p.profession()));
        e.masterOf(id).ifPresent(m -> out.add(String.format("Maestro: %s (%s, %.0f%%, %s)", name(e, m.master()), m.type(), m.progress() * 100, m.state())));
        List<Mentorship> disciples = e.disciplesOf(id);
        if (!disciples.isEmpty()) out.add("Discípulos: " + String.join(", ", disciples.stream().map(m -> String.format("%s %.0f%% %s", name(e, m.disciple()), m.progress() * 100, m.state())).toList()));
        List<String> techs = e.techniques().stream().filter(t -> t.holders().contains(id)).map(Technique::name).toList();
        if (!techs.isEmpty()) out.add("Sabe: " + String.join(", ", techs));
        if (f != null) out.add(String.format("Reputación familiar %+.2f, honor familiar %+.1f (expectativa social que hereda)", f.reputation().value(), f.honor().value()));
        if (f != null && f.isHouse()) out.add("Casa: " + f.houseTitle());
        if (f != null && f.clan() != null) e.clan(f.clan()).ifPresent(c -> out.add("Clan: " + c.name() + " (" + c.status() + ")"));
        List<String> owned = e.heirlooms().stream().filter(h -> id.equals(h.currentOwner())).map(Heirloom::name).toList();
        if (!owned.isEmpty()) out.add("Reliquias: " + String.join(", ", owned));
        LegacyRecord legacy = e.legacy(id);
        if (!legacy.causes().isEmpty()) out.add(String.format("Legado %.1f: %s", legacy.total(), String.join("; ", legacy.causes().stream().map(LegacyRecord.Cause::text).toList())));
        if (!p.importantMemories().isEmpty()) out.add("Recuerdos importantes: " + String.join("; ", p.importantMemories()));
        return out;
    }

    /**
     * The composed identity block the specification's name debugger asks for: every part of a person's name, the house and
     * clan they belong to, their lineage, and the culture and order that shaped the name — one line per field, all of it
     * read from state that already exists (nothing here is recomputed or re-rolled).
     */
    public static List<String> identity(FamilyEngine e, UUID id) {
        List<String> out = new ArrayList<>();
        Person p = e.person(id).orElse(null);
        if (p == null) { out.add("Sin registro familiar."); return out; }
        NameRecord n = p.name();
        FamilyRecord f = e.familyOf(id).orElse(null);
        NameCulture culture = NameCulture.parse(n.cultureId()).orElse(NameCulture.YAMATO);
        out.add("Nombre de pila: " + (n.given().isEmpty() ? "—" : n.given()));
        out.add("Apellido / familia: " + (n.family().isEmpty() ? "—" : n.family()));
        out.add("Nombre completo: " + n.formal());
        out.add("Casa: " + (f != null && f.isHouse() ? f.houseTitle() : "—"));
        out.add("Clan: " + (f != null && f.clan() != null ? e.clan(f.clan()).map(ClanRecord::name).orElse("?") : "—"));
        out.add("Linaje: " + (n.lineageName().isEmpty() ? "—" : n.lineageName()));
        String titleHonorific = (n.title() + (n.honorific().isEmpty() ? "" : " " + n.honorific())).trim();
        out.add("Título / honorífico: " + (titleHonorific.isEmpty() ? "—" : titleHonorific));
        out.add("Epíteto: " + (n.epithet().isEmpty() ? "—" : n.epithet()));
        out.add("Cultura de nombre: " + culture.label() + " (" + (n.order() == NameRecord.Order.FAMILY_FIRST ? "apellido primero" : "nombre primero") + ")");
        out.add("Semilla de generación: " + id);
        return out;
    }

    public static List<String> family(FamilyEngine e, FamilyRecord f) {
        List<String> out = new ArrayList<>();
        out.add(String.format("Familia %s [%s] — fundada %s, %d generaciones, %d miembros vivos de %d, jefe %s%s", f.name(), f.status(), e.clock().date(f.created()).shortDate(),
                f.generationCount(), e.living(f.id()).size(), e.members(f.id()).size(), f.head() == null ? "—" : name(e, f.head()), f.parentFamily() == null ? "" : " (rama: " + f.branchReason() + ")"));
        out.add(String.format("Reputación %+.2f, honor %+.1f, importancia histórica %.2f; hogares %d; ramas %d", f.reputation().value(), f.honor().value(), f.historicalImportance(),
                f.households().size(), f.branches().size()));
        if (!f.traditions().isEmpty()) out.add("Tradiciones: " + String.join(", ", f.traditions()));
        if (!f.knowledge().isEmpty()) out.add("Conocimiento que guarda: " + String.join(", ", f.knowledge()));
        if (!f.relations().isEmpty()) out.add("Relaciones: " + f.relations());
        List<String> causes = f.honor().causes().stream().map(c -> String.format("%+.1f %s", c.delta(), c.cause())).toList();
        if (!causes.isEmpty()) out.add("Causas del honor: " + String.join(" | ", causes));
        List<SuccessionEngine.Ranked> ranking = e.successionRanking(f.id());
        if (!ranking.isEmpty()) out.add("Sucesión: " + String.join(", ", ranking.stream().limit(3).map(r -> String.format("%s (%.2f)", r.name(), r.score())).toList()));
        return out;
    }

    /** The family tree drawn from its founders, as the specification shows it. */
    public static List<String> tree(FamilyEngine e, FamilyRecord f) {
        List<String> out = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        List<UUID> roots = new ArrayList<>(f.founders());
        if (roots.isEmpty()) for (UUID m : e.members(f.id())) if (e.parents(m).isEmpty()) roots.add(m);
        for (UUID root : roots) {
            if (seen.contains(root)) continue;
            Set<UUID> partners = e.partners(root);
            String couple = name(e, root) + (partners.isEmpty() ? "" : " + " + names(e, partners));
            seen.addAll(partners);
            out.add(couple);
            draw(e, root, "", seen, out, 0);
        }
        return out;
    }

    private static void draw(FamilyEngine e, UUID person, String prefix, Set<UUID> seen, List<String> out, int depth) {
        if (!seen.add(person) || depth > 12) return;
        List<UUID> kids = new ArrayList<>(e.children(person));
        kids.sort(Comparator.comparingLong(k -> e.person(k).map(Person::birth).orElse(0L)));
        for (int i = 0; i < kids.size(); i++) {
            UUID k = kids.get(i);
            boolean last = i == kids.size() - 1;
            Set<UUID> partners = e.partners(k);
            out.add(prefix + (last ? "└── " : "├── ") + name(e, k) + (partners.isEmpty() ? "" : " + " + names(e, partners)) + String.format(" [g%d]", e.person(k).map(Person::generation).orElse(0)));
            draw(e, k, prefix + (last ? "    " : "│   "), seen, out, depth + 1);
        }
    }

    /** A school's tree: each master with the disciples they formed, and theirs. */
    public static List<String> lineageTree(FamilyEngine e, Lineage l) {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s (%s, %s) — %d generaciones de maestros, %d miembros, sabe: %s", l.name(), l.type(), l.status(), l.generations(), l.members().size(),
                l.knowledge().isEmpty() ? "—" : String.join(", ", l.knowledge())));
        out.add(name(e, l.founder()) + " (fundador)");
        drawSchool(e, l, l.founder(), "", new HashSet<>(), out);
        for (Lineage.Leadership x : l.leaders()) out.add(String.format("  maestro %s desde %s (%s)", name(e, x.leader()), e.clock().date(x.from()).shortDate(), x.how()));
        return out;
    }

    private static void drawSchool(FamilyEngine e, Lineage l, UUID master, String prefix, Set<UUID> seen, List<String> out) {
        if (!seen.add(master)) return;
        List<Mentorship> ds = e.disciplesOf(master).stream().filter(m -> l.id().equals(m.lineage()) || m.lineage() == null && l.members().contains(m.disciple())).toList();
        for (int i = 0; i < ds.size(); i++) {
            Mentorship m = ds.get(i);
            boolean last = i == ds.size() - 1;
            out.add(prefix + (last ? "└── " : "├── ") + name(e, m.disciple()) + String.format(" (%s, %.0f%%)", m.state(), m.progress() * 100));
            drawSchool(e, l, m.disciple(), prefix + (last ? "    " : "│   "), seen, out);
        }
    }

    public static List<String> heirloom(FamilyEngine e, Heirloom h) {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s (%s) de la familia %s, creada %s, en manos de %s%s, valor simbólico %.1f", h.name(), h.kind(), e.family(h.family()).map(FamilyRecord::name).orElse("?"),
                e.clock().date(h.created()).shortDate(), name(e, h.currentOwner()), h.lost() ? " [PERDIDA]" : "", h.symbolicValue()));
        for (Heirloom.TransferRecord t : h.transfers()) out.add(String.format("  %s: %s → %s (%s)", e.clock().date(t.minute()).shortDate(), name(e, t.from()), name(e, t.to()), t.reason()));
        for (Heirloom.HistoricEvent x : h.events()) out.add(String.format("  %s: %s", e.clock().date(x.minute()).shortDate(), x.text()));
        return out;
    }

    /** A clan's members, leadership, region, history, reputation, honour and standing with other clans. */
    public static List<String> clan(FamilyEngine e, ClanRecord c) {
        List<String> out = new ArrayList<>();
        out.add(String.format("Clan %s [%s] — fundado %s, %d familia(s), líder %s", c.name(), c.status(), e.clock().date(c.founded()).shortDate(),
                c.memberFamilies().size(), c.leaderFamily() == null ? "—" : e.family(c.leaderFamily()).map(FamilyRecord::name).orElse("?")));
        out.add("Familias miembro: " + (c.memberFamilies().isEmpty() ? "—" : String.join(", ",
                c.memberFamilies().stream().map(fid -> e.family(fid).map(FamilyRecord::name).orElse("?")).toList())));
        out.add(String.format("Reputación %+.2f, honor %+.1f", c.reputation().value(), c.honor().value()));
        if (!c.traditions().isEmpty()) out.add("Tradiciones: " + String.join(", ", c.traditions()));
        if (!c.history().isEmpty()) out.add("Historia: " + String.join("; ", c.history()));
        List<String> allies = new ArrayList<>(), rivals = new ArrayList<>();
        for (var entry : c.relations().entrySet()) {
            String other = e.clan(entry.getKey()).map(ClanRecord::name).orElse("?");
            if (entry.getValue() == ClanRecord.Relation.ALLY) allies.add(other);
            else if (entry.getValue() == ClanRecord.Relation.RIVAL || entry.getValue() == ClanRecord.Relation.HOSTILE) rivals.add(other);
        }
        if (!allies.isEmpty()) out.add("Aliados: " + String.join(", ", allies));
        if (!rivals.isEmpty()) out.add("Rivales: " + String.join(", ", rivals));
        return out;
    }

    /**
     * A batch of sample names for a culture, generated for inspection only — nothing here touches a family, a person or the
     * store. Development/debug tool only, as the specification asks for; the world's own naming always goes through
     * {@link FamilyEngine#adopt}, never through this method.
     */
    public static List<String> sampleNames(NameCulture culture, long seed, int count) {
        List<String> out = new ArrayList<>();
        Dice dice = new Dice(seed);
        for (int i = 0; i < count; i++) {
            KinGender gender = i % 2 == 0 ? KinGender.MASCULINE : KinGender.FEMININE;
            String key = "debug-name-" + i;
            String given = NamingEngine.givenName(dice, key, gender, culture);
            SurnameGenerator.Result surname = SurnameGenerator.roll(NameCultureCatalog.of(culture), dice, key);
            NameRecord n = new NameRecord(given, surname.surname(), "", "", "").withCulture(culture.name().toLowerCase(java.util.Locale.ROOT), NamingEngine.orderOf(culture));
            out.add(n.full() + " (" + surname.origin() + ")");
        }
        return out;
    }

    public static List<String> metrics(FamilyEngine e) {
        FamilyMetrics.Snapshot m = e.metrics().snapshot();
        long active = e.families().stream().filter(FamilyRecord::active).count();
        return List.of(
                String.format("Familias activas %d (creadas %d, extintas %d, ramas %d), personas %d, nacimientos %d, adoptados %d, generaciones nuevas %d, sucesiones %d, tradiciones %d",
                        active, m.familiesCreated(), m.extinct(), m.branches(), e.people().size(), m.births(), m.adopted(), m.generations(), m.successions(), m.traditions()),
                String.format("Mentorías %d (completadas %d), técnicas enseñadas %d (perdidas %d), herencias %d, vínculos rechazados %d",
                        m.mentorships(), m.mentorshipsCompleted(), m.techniquesTaught(), m.techniquesLost(), m.inheritances(), m.rejectedLinks()),
                String.format("Consultas de parentesco %d (%.2f µs), caché genealógica %d aciertos / %d fallos, simulaciones %d (%.1f µs)", m.queries(), m.queryMicros(),
                        e.graph().cacheHits(), e.graph().cacheMisses(), m.simulations(), m.simulationMicros()));
    }
}
