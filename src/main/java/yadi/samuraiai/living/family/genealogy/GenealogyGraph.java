package yadi.samuraiai.living.family.genealogy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The genealogy as a graph. Only two kinds of edge are stored — PARENT_OF (directed) and PARTNER_OF (undirected, with the
 * minute it began) — and everything else (children, siblings, grandparents, cousins, ancestors, descendants) is derived from
 * them, so kinship can never be recorded twice and contradict itself. Ancestor sets are cached per person and invalidated
 * by a version counter whenever an edge is added or removed.
 */
public final class GenealogyGraph {
    public record Partnership(UUID a, UUID b, long since, long until) {
        public boolean current() { return until == Long.MAX_VALUE; }
        public UUID other(UUID p) { return p.equals(a) ? b : a; }
    }

    private final Map<UUID, Set<UUID>> parents = new HashMap<>();
    private final Map<UUID, Set<UUID>> children = new HashMap<>();
    private final Map<UUID, List<Partnership>> partners = new HashMap<>();
    private final Map<UUID, Set<UUID>> ancestorCache = new HashMap<>();
    private long version, cacheHits, cacheMisses;

    public Set<UUID> parentsOf(UUID p) { return Set.copyOf(parents.getOrDefault(p, Set.of())); }
    public Set<UUID> childrenOf(UUID p) { return Set.copyOf(children.getOrDefault(p, Set.of())); }
    public List<Partnership> partnershipsOf(UUID p) { return List.copyOf(partners.getOrDefault(p, List.of())); }
    public long version() { return version; }
    public long cacheHits() { return cacheHits; }
    public long cacheMisses() { return cacheMisses; }

    /** Adds PARENT_OF without checks (the validator runs first; restore from disk uses this directly). */
    public boolean linkParent(UUID parent, UUID child) {
        boolean added = parents.computeIfAbsent(child, k -> new LinkedHashSet<>()).add(parent);
        children.computeIfAbsent(parent, k -> new LinkedHashSet<>()).add(child);
        if (added) { version++; ancestorCache.clear(); }
        return added;
    }

    public boolean unlinkParent(UUID parent, UUID child) {
        boolean removed = parents.getOrDefault(child, new LinkedHashSet<>()).remove(parent);
        children.getOrDefault(parent, new LinkedHashSet<>()).remove(child);
        if (removed) { version++; ancestorCache.clear(); }
        return removed;
    }

    public void linkPartners(UUID a, UUID b, long since, long until) {
        Partnership p = new Partnership(a, b, since, until);
        partners.computeIfAbsent(a, k -> new ArrayList<>()).add(p);
        partners.computeIfAbsent(b, k -> new ArrayList<>()).add(p);
        version++;
    }

    public boolean endPartnership(UUID a, UUID b, long at) {
        for (Partnership p : List.copyOf(partners.getOrDefault(a, List.of())))
            if (p.current() && p.other(a).equals(b)) {
                Partnership ended = new Partnership(p.a(), p.b(), p.since(), at);
                partners.get(a).remove(p); partners.get(a).add(ended);
                partners.get(b).remove(p); partners.get(b).add(ended);
                version++;
                return true;
            }
        return false;
    }

    public Set<UUID> currentPartners(UUID p) {
        Set<UUID> out = new LinkedHashSet<>();
        for (Partnership x : partners.getOrDefault(p, List.of())) if (x.current()) out.add(x.other(p));
        return out;
    }

    // ------------------------------------------------------------------ derived kinship

    /** Brothers and sisters: people sharing at least one parent. */
    public Set<UUID> siblingsOf(UUID p) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID parent : parents.getOrDefault(p, Set.of())) for (UUID c : children.getOrDefault(parent, Set.of())) if (!c.equals(p)) out.add(c);
        return out;
    }

    /** Full siblings share both (two) parents. */
    public boolean fullSiblings(UUID a, UUID b) {
        Set<UUID> pa = parents.getOrDefault(a, Set.of()), pb = parents.getOrDefault(b, Set.of());
        return pa.size() == 2 && pa.equals(pb);
    }

    public Set<UUID> grandparentsOf(UUID p) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID parent : parents.getOrDefault(p, Set.of())) out.addAll(parents.getOrDefault(parent, Set.of()));
        return out;
    }

    public Set<UUID> grandchildrenOf(UUID p) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID c : children.getOrDefault(p, Set.of())) out.addAll(children.getOrDefault(c, Set.of()));
        return out;
    }

    /** Uncles and aunts: siblings of one's parents. */
    public Set<UUID> parentsSiblingsOf(UUID p) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID parent : parents.getOrDefault(p, Set.of())) out.addAll(siblingsOf(parent));
        return out;
    }

    /** Nephews and nieces: children of one's siblings. */
    public Set<UUID> siblingsChildrenOf(UUID p) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID s : siblingsOf(p)) out.addAll(children.getOrDefault(s, Set.of()));
        return out;
    }

    /** First cousins: children of one's parents' siblings. */
    public Set<UUID> cousinsOf(UUID p) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID u : parentsSiblingsOf(p)) out.addAll(children.getOrDefault(u, Set.of()));
        out.remove(p);
        out.removeAll(siblingsOf(p));
        return out;
    }

    /** Every ancestor (unbounded), cached. */
    public Set<UUID> ancestorsOf(UUID p) {
        Set<UUID> cached = ancestorCache.get(p);
        if (cached != null) { cacheHits++; return cached; }
        cacheMisses++;
        Set<UUID> out = new LinkedHashSet<>();
        Deque<UUID> open = new ArrayDeque<>(parents.getOrDefault(p, Set.of()));
        while (!open.isEmpty()) { UUID x = open.poll(); if (out.add(x)) open.addAll(parents.getOrDefault(x, Set.of())); }
        Set<UUID> frozen = Set.copyOf(out);
        ancestorCache.put(p, frozen);
        return frozen;
    }

    /** Ancestors with their distance in generations (1 = parent), up to {@code depth} (0 = unlimited). */
    public Map<UUID, Integer> ancestors(UUID p, int depth) { return walk(p, depth, parents); }
    public Map<UUID, Integer> descendants(UUID p, int depth) { return walk(p, depth, children); }

    private static Map<UUID, Integer> walk(UUID start, int depth, Map<UUID, Set<UUID>> edges) {
        Map<UUID, Integer> out = new LinkedHashMap<>();
        Deque<UUID> open = new ArrayDeque<>();
        Map<UUID, Integer> dist = new HashMap<>();
        dist.put(start, 0);
        open.add(start);
        while (!open.isEmpty()) {
            UUID x = open.poll();
            int d = dist.get(x);
            if (depth > 0 && d >= depth) continue;
            for (UUID y : edges.getOrDefault(x, Set.of())) {
                if (dist.containsKey(y)) continue;
                dist.put(y, d + 1);
                out.put(y, d + 1);
                open.add(y);
            }
        }
        return out;
    }

    public boolean isAncestor(UUID ancestor, UUID of) { return ancestorsOf(of).contains(ancestor); }

    /** The nearest common ancestor of two people and the generations from each to it, or null. */
    public record Common(UUID ancestor, int fromA, int fromB) { }

    public Common commonAncestor(UUID a, UUID b) {
        Map<UUID, Integer> ua = ancestors(a, 0), ub = ancestors(b, 0);
        ua.put(a, 0); ub.put(b, 0);
        Common best = null;
        for (var e : ua.entrySet()) {
            Integer db = ub.get(e.getKey());
            if (db == null) continue;
            if (best == null || e.getValue() + db < best.fromA() + best.fromB()) best = new Common(e.getKey(), e.getValue(), db);
        }
        return best;
    }

    /** Degree of kinship (civil law counting: generations up to the common ancestor plus down), or -1 when not related. */
    public int degree(UUID a, UUID b) {
        if (a.equals(b)) return 0;
        Common c = commonAncestor(a, b);
        return c == null ? -1 : c.fromA() + c.fromB();
    }

    public boolean related(UUID a, UUID b) { return degree(a, b) >= 0; }

    public Collection<UUID> people() { Set<UUID> all = new LinkedHashSet<>(parents.keySet()); all.addAll(children.keySet()); all.addAll(partners.keySet()); return all; }

    /** Every PARENT_OF edge (for persistence). */
    public List<UUID[]> parentEdges() {
        List<UUID[]> out = new ArrayList<>();
        for (var e : parents.entrySet()) for (UUID parent : e.getValue()) out.add(new UUID[]{parent, e.getKey()});
        return out;
    }

    public List<Partnership> allPartnerships() {
        Set<Partnership> out = new LinkedHashSet<>();
        for (List<Partnership> l : partners.values()) out.addAll(l);
        return new ArrayList<>(out);
    }

    public void clear() { parents.clear(); children.clear(); partners.clear(); ancestorCache.clear(); version++; }
}
