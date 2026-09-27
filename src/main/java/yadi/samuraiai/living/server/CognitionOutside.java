package yadi.samuraiai.living.server;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.cognition.engine.CognitionEngine;
import yadi.samuraiai.ai.cognition.engine.ExperienceInput;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.world.CognitionService;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.history.HistoryType;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.society.Community;
import yadi.samuraiai.ai.knowledge.society.CommunityKind;
import yadi.samuraiai.living.sim.Outside;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.world.ServerWorlds;

/**
 * The living world's view of the rest of SamuraiAI, over the cognitive layer and the online players. Village communities are
 * Knowledge communities (collective knowledge, culture, history and standing stay there); relationships, moods and
 * personality are read from the cognitive engines; what the living world makes an NPC live through becomes an experience in
 * the cognitive pipeline, so memory, emotion and relationships follow from the same catalogue as everything else. When the
 * cognitive layer is off every call answers neutrally and changes nothing. Server-thread only.
 */
final class CognitionOutside implements Outside {
    private final ResourceItems items;

    CognitionOutside(ResourceItems items) { this.items = items; }

    private static CognitionService cognition() { return CognitionService.getInstance(); }
    private static boolean on() { return cognition().enabled(); }
    private static CognitionEngine engine() { return cognition().engine(); }

    // ------------------------------------------------------------------ communities

    @Override public String ensureCommunity(String key, String name, String culture, String dimension, double x, double y, double z, double radius) {
        if (!on()) return key;
        try {
            Community c = engine().society().community(key).orElse(null);
            if (c == null) c = engine().society().create(key, name, CommunityKind.VILLAGE, culture, new PlaceRef(dimension, x, y, z, ""), radius);
            return c.id();
        } catch (IllegalStateException full) {
            SamuraiLogger.BRAIN.warn("Living world could not create community {}: {}", key, full.getMessage());
            return key;
        }
    }

    @Override public void joinCommunity(UUID npc, String key, boolean leader) {
        if (!on()) return;
        engine().society().community(key).ifPresent(c -> {
            if (!c.member(npc)) engine().society().join(npc, c.id(), leader ? AccessLevel.LEADERS : AccessLevel.MEMBERS);
        });
    }

    @Override public void leaveCommunity(UUID npc, String key) {
        if (on()) engine().society().leave(npc, key == null ? "" : key.toLowerCase(Locale.ROOT));
    }

    @Override public void rememberInCommunity(String key, String historyType, String title, double significance, long minute, UUID actor) {
        if (!on()) return;
        Community c = engine().society().community(key).orElse(null);
        if (c == null) return;
        HistoryType type;
        try { type = HistoryType.valueOf(historyType.toUpperCase(Locale.ROOT)); } catch (RuntimeException e) { type = HistoryType.OTHER; }
        List<EntityRef> participants = actor == null ? List.of() : List.of(cognition().ref(actor));
        // the community history keeps game ticks like every other cognitive record; the Deiliora minute travels as a tag
        engine().society().record(c.id(), new HistoricalEvent(UUID.randomUUID(), type, cognition().now(), c.center(), participants, Set.of("living", "minute:" + minute),
                significance, title, null, c.id(), actor));
    }

    @Override public double standing(UUID subject, String communityKey, String context) {
        if (!on() || subject == null) return 0;
        Community c = engine().society().community(communityKey).orElse(null);
        if (c == null) return 0;
        Map<String, Double> labels = c.standing().get(subject);
        if (labels == null) return 0;
        if (context == null || context.isEmpty()) return labels.values().stream().mapToDouble(Double::doubleValue).sum();
        return labels.getOrDefault(context, 0.0D);
    }

    @Override public void adjustStanding(UUID subject, String subjectName, String communityKey, String context, double amount) {
        if (!on() || subject == null) return;
        EntityRef ref = cognition().ref(subject);
        if (ref.kind() == EntityKind.UNKNOWN && subjectName != null) ref = EntityRef.player(subject, subjectName);
        engine().society().adjustStanding(communityKey, ref, context == null || context.isEmpty() ? "general" : context, amount);
    }

    // ------------------------------------------------------------------ relationships, emotions, experiences

    @Override public double trust(UUID npc, UUID other) {
        if (!on()) return 50;
        return engine().relationships().find(npc, other).map(r -> r.trust()).orElse(engine().relationships().settings().initialTrust());
    }

    @Override public double respect(UUID npc, UUID other) {
        if (!on()) return 50;
        return engine().relationships().find(npc, other).map(r -> r.respect()).orElse(engine().relationships().settings().initialRespect());
    }

    @Override public String mood(UUID npc) {
        if (!on()) return "CALM";
        return engine().emotions().blend(npc).dominant().name();
    }

    @Override public void experience(UUID npc, UUID other, String otherName, String kind, String note) {
        if (!on() || npc == null || kind == null) return;
        ExperienceKind k;
        try { k = ExperienceKind.valueOf(kind.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException unknown) { SamuraiLogger.BRAIN.warn("Living world asked for unknown experience {}", kind); return; }
        ExperienceInput in = ExperienceInput.of(npc, k, cognition().now()).source("living");
        if (other != null) {
            EntityRef ref = cognition().ref(other);
            if (ref.kind() == EntityKind.UNKNOWN && otherName != null && !otherName.isEmpty()) ref = new EntityRef(other, EntityKind.UNKNOWN, otherName);
            in.actor(ref);
        }
        if (note != null && !note.isEmpty()) in.note(note).context("placeName", note);
        cognition().experience(npc, in);
    }

    /** Learning is lived, not written: the disciple learns from the master and the master teaches, both through the catalogue. */
    @Override public void learn(UUID npc, String key, String text, UUID teacher) {
        if (!on() || npc == null) return;
        String note = text == null || text.isEmpty() ? key : text;
        experience(npc, teacher, null, ExperienceKind.LEARNED_FROM.name(), note);
        if (teacher != null) experience(teacher, npc, null, ExperienceKind.TAUGHT.name(), note);
    }

    @Override public double teachingQuality(UUID master, UUID disciple) {
        if (!on()) return 0.6;
        double respect = respect(disciple, master) / 100.0D, trust = trust(disciple, master) / 100.0D;
        double patience = engine().personality(master).unit(Trait.PATIENCE), diligence = engine().personality(disciple).unit(Trait.DILIGENCE);
        return Math.max(0.1D, Math.min(1.0D, 0.2D + 0.3D * respect + 0.2D * trust + 0.15D * patience + 0.15D * diligence));
    }

    /** How much a personality suits a profession, 0..1 (0.5 neutral), from the traits the profession leans on. */
    @Override public double affinity(UUID npc, String profession) {
        if (!on() || npc == null) return 0.5;
        var p = engine().personality(npc);
        Trait[] traits = switch (profession == null ? "" : profession) {
            case "guard" -> new Trait[]{Trait.COURAGE, Trait.DISCIPLINE, Trait.LOYALTY};
            case "samurai" -> new Trait[]{Trait.COURAGE, Trait.DISCIPLINE, Trait.PRIDE};
            case "monk", "healer" -> new Trait[]{Trait.SPIRITUALITY, Trait.PATIENCE, Trait.EMPATHY};
            case "merchant" -> new Trait[]{Trait.SOCIABILITY, Trait.CURIOSITY};
            case "hunter" -> new Trait[]{Trait.COURAGE, Trait.PATIENCE};
            case "herbalist" -> new Trait[]{Trait.CURIOSITY, Trait.PATIENCE};
            case "cook" -> new Trait[]{Trait.SOCIABILITY, Trait.DILIGENCE};
            default -> new Trait[]{Trait.DILIGENCE, Trait.PATIENCE};
        };
        double sum = 0;
        for (Trait t : traits) sum += p.unit(t);
        return sum / traits.length;
    }

    /** Members who feel grateful and whose warmest relationship with a player is strong: npc → player. */
    @Override public Map<UUID, UUID> grateful(String communityKey) {
        if (!on()) return Map.of();
        Community c = engine().society().community(communityKey).orElse(null);
        if (c == null) return Map.of();
        Map<UUID, UUID> out = new HashMap<>();
        for (UUID member : c.memberIds()) {
            if (!engine().isLoaded(member) || engine().emotions().intensity(member, EmotionKind.GRATITUDE) < 40.0D) continue;
            UUID best = null;
            double bestAffinity = 65.0D;
            for (var r : engine().relationships().relationships(member))
                if (r.target().kind() == EntityKind.PLAYER && r.affinity() > bestAffinity) { best = r.target().id(); bestAffinity = r.affinity(); }
            if (best != null) out.put(member, best);
        }
        return out;
    }

    /** Members whose relationship with another member shows open rivalry (rivalry ≥ 60, or trust ≤ 15 with respect ≤ 25), each pair once. */
    @Override public List<UUID[]> rivals(String communityKey) {
        if (!on()) return List.of();
        Community c = engine().society().community(communityKey).orElse(null);
        if (c == null) return List.of();
        List<UUID[]> out = new java.util.ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (UUID member : c.memberIds()) {
            if (!engine().isLoaded(member)) continue;
            for (var r : engine().relationships().relationships(member)) {
                UUID other = r.target().id();
                if (r.target().kind() != EntityKind.NPC || !c.member(other)) continue;
                if (r.rivalry() < 60 && !(r.trust() <= 15 && r.respect() <= 25)) continue;
                String key = member.compareTo(other) < 0 ? member + ":" + other : other + ":" + member;
                if (seen.add(key)) out.add(new UUID[]{member, other});
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ players

    @Override public void tell(UUID player, String text) {
        ServerWorlds.playerById(player).ifPresent(p -> p.sendSystemMessage(Component.literal(text)));
    }

    @Override public double giveItems(UUID player, String resource, double quantity) {
        ServerPlayer p = ServerWorlds.playerById(player).orElse(null);
        return p == null ? 0 : items.give(p, resource, quantity);
    }

    @Override public Set<UUID> playersNear(String dimension, double x, double z, double radius) {
        Set<UUID> out = new LinkedHashSet<>();
        for (ServerPlayer p : ServerWorlds.onlinePlayers()) {
            if (!p.level.dimension().location().toString().equals(dimension)) continue;
            if (Math.hypot(p.getX() - x, p.getZ() - z) <= radius) out.add(p.getUUID());
        }
        return out;
    }
}
