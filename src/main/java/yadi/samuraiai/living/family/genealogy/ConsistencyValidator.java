package yadi.samuraiai.living.family.genealogy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.family.model.Person;

/**
 * Keeps the genealogy structurally valid. A parent link is refused when it would make someone their own parent, reverse an
 * existing link (A parent of B and B parent of A), close a cycle of ancestry, duplicate an edge, give a child more than two
 * parents, or be impossible in time (a parent younger than {@code minParentAge} years at the child's birth). A partnership is
 * refused with oneself or between a parent and a child. A full audit checks the whole store.
 */
public final class ConsistencyValidator {
    public record Verdict(boolean ok, String reason) {
        public static final Verdict OK = new Verdict(true, "");
        static Verdict no(String reason) { return new Verdict(false, reason); }
    }

    private final double minParentAge;
    private final long minutesPerYear;

    public ConsistencyValidator(double minParentAge, long minutesPerYear) { this.minParentAge = minParentAge; this.minutesPerYear = minutesPerYear; }

    public Verdict canLinkParent(GenealogyGraph g, Person parent, Person child) {
        if (parent == null || child == null) return Verdict.no("persona desconocida");
        UUID p = parent.id(), c = child.id();
        if (p.equals(c)) return Verdict.no("nadie puede ser su propio progenitor");
        if (g.parentsOf(c).contains(p)) return Verdict.no("vínculo duplicado");
        if (g.parentsOf(p).contains(c)) return Verdict.no("A no puede ser progenitor de B si B es progenitor de A");
        if (g.isAncestor(c, p)) return Verdict.no("crearía un ciclo de ascendencia");
        if (g.parentsOf(c).size() >= 2) return Verdict.no("ya tiene dos progenitores");
        if (child.birth() - parent.birth() < minParentAge * minutesPerYear) return Verdict.no(String.format("el progenitor tendría menos de %.0f años al nacer el hijo", minParentAge));
        return Verdict.OK;
    }

    public Verdict canPartner(GenealogyGraph g, Person a, Person b) {
        if (a == null || b == null) return Verdict.no("persona desconocida");
        if (a.id().equals(b.id())) return Verdict.no("nadie es su propia pareja");
        if (g.parentsOf(a.id()).contains(b.id()) || g.parentsOf(b.id()).contains(a.id())) return Verdict.no("progenitor e hijo no pueden ser pareja");
        if (g.currentPartners(a.id()).contains(b.id())) return Verdict.no("ya son pareja");
        return Verdict.OK;
    }

    /** Every inconsistency in the store (after a load, or on demand). */
    public List<String> audit(GenealogyGraph g, Map<UUID, Person> people) {
        List<String> problems = new ArrayList<>();
        for (UUID[] edge : g.parentEdges()) {
            Person parent = people.get(edge[0]), child = people.get(edge[1]);
            if (parent == null || child == null) { problems.add("arista hacia persona desconocida " + edge[0] + " → " + edge[1]); continue; }
            if (edge[0].equals(edge[1])) problems.add(child.name().full() + " es su propio progenitor");
            if (g.parentsOf(edge[0]).contains(edge[1])) problems.add("bucle " + parent.name().full() + " ↔ " + child.name().full());
            if (g.ancestorsOf(edge[0]).contains(edge[1])) problems.add("ciclo de ascendencia en " + child.name().full());
            if (child.generation() <= parent.generation() && child.family() != null && child.family().equals(parent.family()))
                problems.add("generación imposible: " + child.name().full() + " (" + child.generation() + ") no es posterior a " + parent.name().full() + " (" + parent.generation() + ")");
            if (child.birth() < parent.birth()) problems.add(child.name().full() + " nació antes que su progenitor " + parent.name().full());
        }
        for (UUID p : g.people()) if (g.parentsOf(p).size() > 2) problems.add(p + " tiene más de dos progenitores");
        return problems;
    }
}
