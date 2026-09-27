package yadi.samuraiai.living.village.population;

import java.util.Map;
import java.util.TreeMap;

/**
 * A village's population at a glance: residents, citizens without an entity, professions, guards, monks, children and elders
 * (from the Family Engine's life stages), visitors present and citizens without a home.
 */
public record VillageCensus(int residents, int abstractCitizens, Map<String, Integer> professions, int guards, int monks, int children, int elders, int visitors, int homeless) {
    public VillageCensus { professions = new TreeMap<>(professions); }
    public int total() { return residents; }
}
