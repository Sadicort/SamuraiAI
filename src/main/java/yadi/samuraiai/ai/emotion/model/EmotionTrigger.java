package yadi.samuraiai.ai.emotion.model;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/**
 * Something that should make the NPC feel: the effects, their source, the memory and entity behind it and how much it matters.
 * Echoes and contagion are flagged so they never turn into new memories or spread again (which keeps memory, emotion and
 * other NPCs from feeding each other in loops).
 */
public record EmotionTrigger(UUID npc, TriggerSource source, List<EmotionEffect> effects, UUID memoryId, String ref, UUID traceId, long at, PlaceRef place,
                             EntityRef entity, boolean traumatic, double weight, boolean fromEcho, String experienceKind, String note) {
    public EmotionTrigger {
        effects = effects == null ? List.of() : List.copyOf(effects);
        ref = ref == null ? "" : ref;
        place = place == null ? PlaceRef.unknown() : place;
        weight = Double.isFinite(weight) ? Math.max(0.0D, Math.min(1.0D, weight)) : 0.5D;
        experienceKind = experienceKind == null ? "" : experienceKind;
        note = note == null ? "" : note;
    }

    public static EmotionTrigger simple(UUID npc, TriggerSource source, yadi.samuraiai.ai.cognition.model.EmotionKind kind, double intensity, long at) {
        return new EmotionTrigger(npc, source, List.of(new EmotionEffect(kind, intensity)), null, "", null, at, null, null, false, 0.5D, false, "", "");
    }
}
