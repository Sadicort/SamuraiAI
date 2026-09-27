package yadi.samuraiai.ai.scheduler.formation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.group.GroupRole;

/**
 * Formation geometry. It builds the slots of a shape for a number of members and assigns each member a slot according to its
 * role (scouts to the front, guards to the flanks, reserves to the rear), then turns slots into world positions for the
 * leader's position and heading. It only computes positions; whoever owns the NPC decides whether and how to walk there.
 */
public final class FormationEngine {
    /** A member's target. */
    public record Assignment(UUID member, GroupRole role, int slot, double x, double z) { }

    /** A member to place. */
    public record Member(UUID id, GroupRole role) { }

    /** Slots for {@code count} members in the given shape; the first is the leader's. */
    public List<FormationSlot> slots(FormationType type, int count, double spacing) {
        List<FormationSlot> out = new ArrayList<>();
        int n = Math.max(1, count);
        out.add(new FormationSlot(0, 0, 0));
        switch (type) {
            case LINE -> { for (int i = 1; i < n; i++) out.add(new FormationSlot(i, ((i + 1) / 2) * spacing * (i % 2 == 1 ? -1 : 1), 0)); }
            case COLUMN -> { for (int i = 1; i < n; i++) out.add(new FormationSlot(i, 0, -i * spacing)); }
            case CIRCLE -> {
                int others = n - 1;
                double radius = Math.max(spacing, others * spacing / (2.0D * Math.PI));
                for (int i = 1; i < n; i++) {
                    double angle = 2.0D * Math.PI * (i - 1) / Math.max(1, others);
                    out.add(new FormationSlot(i, Math.cos(angle) * radius, Math.sin(angle) * radius));
                }
            }
            case DIAMOND -> {
                double[][] base = {{-1, -1}, {1, -1}, {0, -2}};
                for (int i = 1; i < n; i++) {
                    int ring = (i - 1) / 3, k = (i - 1) % 3;
                    out.add(new FormationSlot(i, base[k][0] * spacing * (ring + 1), base[k][1] * spacing * (ring + 1)));
                }
            }
            case ESCORT -> {
                double[][] base = {{0, 1}, {-1, 0}, {1, 0}, {0, -1}};
                for (int i = 1; i < n; i++) {
                    int ring = (i - 1) / 4, k = (i - 1) % 4;
                    out.add(new FormationSlot(i, base[k][0] * spacing * (ring + 1), base[k][1] * spacing * (ring + 1)));
                }
            }
            case TRIANGLE -> {
                int placed = 1;
                for (int row = 1; placed < n; row++) {
                    for (int j = 0; j <= row && placed < n; j++) out.add(new FormationSlot(placed++, (j - row / 2.0D) * spacing, -row * spacing));
                }
            }
        }
        return out;
    }

    /**
     * Places members around the leader. The leader takes slot 0; the rest are ordered by role (scouts first, then guards, then
     * reserves, ties by id) and given the remaining slots from the most forward to the most rearward, except that guards
     * prefer the flanks (largest |right|).
     *
     * @param hx,hz unit heading of the leader
     */
    public List<Assignment> assign(FormationType type, UUID leader, double lx, double lz, double hx, double hz, List<Member> members, double spacing) {
        double len = Math.hypot(hx, hz);
        double fx = len < 1e-6 ? 0.0D : hx / len, fz = len < 1e-6 ? 1.0D : hz / len;
        double rx = -fz, rz = fx;
        List<Member> others = new ArrayList<>();
        for (Member m : members) if (!m.id().equals(leader)) others.add(m);
        others.sort(Comparator.<Member>comparingInt(m -> m.role().ordinal()).thenComparing(Member::id));
        List<FormationSlot> slots = slots(type, others.size() + 1, spacing);
        List<FormationSlot> free = new ArrayList<>(slots.subList(1, slots.size()));
        free.sort(Comparator.comparingDouble(FormationSlot::forward).reversed().thenComparingInt(FormationSlot::index));
        List<Assignment> out = new ArrayList<>();
        out.add(new Assignment(leader, GroupRole.LEADER, 0, lx, lz));
        for (Member m : others) {
            FormationSlot pick;
            if (m.role() == GroupRole.GUARD) pick = free.stream().max(Comparator.comparingDouble((FormationSlot s) -> Math.abs(s.right())).thenComparingInt(s -> -s.index())).orElse(free.get(0));
            else if (m.role() == GroupRole.RESERVE) pick = free.get(free.size() - 1);
            else pick = free.get(0);
            free.remove(pick);
            out.add(new Assignment(m.id(), m.role(), pick.index(), lx + fx * pick.forward() + rx * pick.right(), lz + fz * pick.forward() + rz * pick.right()));
        }
        return out;
    }
}
