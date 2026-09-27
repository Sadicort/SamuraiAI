package yadi.samuraiai.living.family.kinship;

import java.util.UUID;
import java.util.function.Function;
import yadi.samuraiai.living.family.genealogy.GenealogyGraph;
import yadi.samuraiai.living.family.model.KinGender;
import yadi.samuraiai.living.family.model.Person;

/**
 * Names the kinship between two people from the genealogy: father, mother, parent, son, daughter, child, brother, sister,
 * sibling, grandparent, grandchild, uncle, aunt, nephew, niece, cousin, partner, ancestor, descendant, or distant relative.
 * The gendered words are used only when the person's record says which to use; otherwise the neutral word.
 */
public final class KinshipEngine {
    private final GenealogyGraph graph;
    private final Function<UUID, Person> people;

    public KinshipEngine(GenealogyGraph graph, Function<UUID, Person> people) { this.graph = graph; this.people = people; }

    private String word(UUID who, String masculine, String feminine, String neutral) {
        Person p = people.apply(who);
        KinGender g = p == null ? KinGender.UNSPECIFIED : p.gender();
        return g == KinGender.MASCULINE ? masculine : g == KinGender.FEMININE ? feminine : neutral;
    }

    /** What {@code b} is to {@code a} ("Hiro es el padre de Aiko" → term(aiko, hiro) = "padre"). */
    public String term(UUID a, UUID b) {
        if (a.equals(b)) return "la misma persona";
        if (graph.currentPartners(a).contains(b)) return word(b, "esposo", "esposa", "pareja");
        if (graph.parentsOf(a).contains(b)) return word(b, "padre", "madre", "progenitor");
        if (graph.childrenOf(a).contains(b)) return word(b, "hijo", "hija", "descendiente directo");
        if (graph.siblingsOf(a).contains(b)) return graph.fullSiblings(a, b) ? word(b, "hermano", "hermana", "hermano/a") : word(b, "medio hermano", "media hermana", "medio hermano/a");
        if (graph.grandparentsOf(a).contains(b)) return word(b, "abuelo", "abuela", "abuelo/a");
        if (graph.grandchildrenOf(a).contains(b)) return word(b, "nieto", "nieta", "nieto/a");
        if (graph.parentsSiblingsOf(a).contains(b)) return word(b, "tío", "tía", "tío/a");
        if (graph.siblingsChildrenOf(a).contains(b)) return word(b, "sobrino", "sobrina", "sobrino/a");
        if (graph.cousinsOf(a).contains(b)) return word(b, "primo", "prima", "primo/a");
        var up = graph.ancestors(a, 0).get(b);
        if (up != null) return "antepasado (" + up + " generaciones)";
        var down = graph.descendants(a, 0).get(b);
        if (down != null) return "descendiente (" + down + " generaciones)";
        int d = graph.degree(a, b);
        return d < 0 ? "sin parentesco" : "pariente de " + d + "º grado";
    }
}
