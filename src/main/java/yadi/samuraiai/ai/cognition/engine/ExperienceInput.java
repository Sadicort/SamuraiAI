package yadi.samuraiai.ai.cognition.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** What the world tells the cognitive layer happened to an NPC; the layer gives it meaning through the experience catalogue. Optional overrides replace the profile's defaults for this one experience. */
public final class ExperienceInput {
    final UUID npc;
    final ExperienceKind kind;
    EntityRef actor, target;
    final List<EntityRef> participants = new ArrayList<>();
    PlaceRef place;
    long at;
    long duration;
    final Map<String, String> context = new LinkedHashMap<>();
    final Set<String> tags = new LinkedHashSet<>();
    Boolean witnessed, publicEvent, traumatic;
    double magnitudeScale = 1.0D;
    String note = "", source = "world";
    UUID trace;

    private ExperienceInput(UUID npc, ExperienceKind kind, long at) { this.npc = npc; this.kind = kind; this.at = at; }

    public static ExperienceInput of(UUID npc, ExperienceKind kind, long at) { return new ExperienceInput(npc, kind, at); }

    public ExperienceInput actor(EntityRef v) { actor = v; return this; }
    public ExperienceInput target(EntityRef v) { target = v; return this; }
    public ExperienceInput participant(EntityRef v) { if (v != null) participants.add(v); return this; }
    public ExperienceInput place(PlaceRef v) { place = v; return this; }
    public ExperienceInput duration(long v) { duration = v; return this; }
    public ExperienceInput context(String key, String value) { context.put(key, value); return this; }
    public ExperienceInput tag(String v) { tags.add(v); return this; }
    public ExperienceInput witnessed(boolean v) { witnessed = v; return this; }
    public ExperienceInput publicEvent(boolean v) { publicEvent = v; return this; }
    public ExperienceInput traumatic(boolean v) { traumatic = v; return this; }
    public ExperienceInput scale(double v) { magnitudeScale = Math.max(0.0D, Math.min(2.0D, v)); return this; }
    public ExperienceInput note(String v) { note = v == null ? "" : v; return this; }
    public ExperienceInput source(String v) { source = v == null ? "world" : v; return this; }
    public ExperienceInput trace(UUID v) { trace = v; return this; }

    public UUID npc() { return npc; }
    public ExperienceKind kind() { return kind; }
}
