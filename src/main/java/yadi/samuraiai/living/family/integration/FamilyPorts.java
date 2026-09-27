package yadi.samuraiai.living.family.integration;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;

/**
 * What the Family Engine needs from the other engines. The family never duplicates them: where someone lives and works is the
 * Village's, coins are the Economy's, trust and respect are the Relationship Engine's (family is not love), learned facts are
 * the Knowledge Engine's (family only records who taught what), history is the Calendar's timeline. The hub implements these.
 */
public final class FamilyPorts {
    private FamilyPorts() { }

    public interface Calendar { long minutesPerYear(); }

    public interface Villages {
        Optional<UUID> villageOf(UUID npc);
        int beds(UUID building);
        boolean damaged(UUID building);
        boolean destroyed(UUID building);
        Optional<String> profession(UUID npc);
        void assignProfession(UUID npc, String profession, String reason);
        List<String> neededProfessions(UUID village);
        void transferBuilding(UUID building, UUID newOwner, String ownerLabel);
        List<UUID> buildingsOwnedBy(UUID npc);
    }

    public interface Economy {
        double coins(UUID owner);
        /** Moves coins between two owners' accounts (an inheritance); returns what moved. */
        double transfer(UUID from, UUID to, String toName, double amount, String reason);
    }

    public interface Social {
        double standing(UUID npc, UUID village);
        double trust(UUID a, UUID b);
        double respect(UUID a, UUID b);
        /** 0..1: how well a master can teach this disciple (the Knowledge Engine's teaching quality over their relationship). */
        double teachingQuality(UUID master, UUID disciple);
        /** The Knowledge Engine learns a fact for an NPC (a technique, a family story), with who taught it. */
        void learn(UUID npc, String key, String text, UUID teacher);
        /** 0..1: how well a profession suits a person's personality. */
        double affinity(UUID npc, String profession);
        /**
         * A ceremony lived through (a succession, an heirloom handed on, an apprenticeship completed, a clan founded):
         * memory, emotion and relationship follow from the cognitive layer's own catalogue, exactly as any other experience
         * does. {@code other} is who else was part of it, or {@code null} for one lived alone.
         */
        void experience(UUID npc, UUID other, String otherName, String kind, String note);
    }

    public interface Chronicle {
        void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source);
        void anniversary(String kind, String subject, String title, long minute);
        int mentions(String scope);
    }

    public static final Social NEUTRAL_SOCIAL = new Social() {
        @Override public double standing(UUID npc, UUID village) { return 0; }
        @Override public double trust(UUID a, UUID b) { return 50; }
        @Override public double respect(UUID a, UUID b) { return 50; }
        @Override public double teachingQuality(UUID master, UUID disciple) { return 0.6; }
        @Override public void learn(UUID npc, String key, String text, UUID teacher) { }
        @Override public double affinity(UUID npc, String profession) { return 0.5; }
        @Override public void experience(UUID npc, UUID other, String otherName, String kind, String note) { }
    };
}
