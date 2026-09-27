package yadi.samuraiai.ai.perception.hearing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.PerceptionWorld;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.vision.RayBudget;
import yadi.samuraiai.ai.perception.vision.Raycaster;
import yadi.samuraiai.ai.perception.vision.VisionCone;

/**
 * Decides which sounds one NPC hears and how well: each category has its own radius scaled by loudness, walls dampen
 * the sound (they do not silence it), and localisation is imprecise: direction is coarse and the estimated position
 * gets less accurate with distance. Deterministic: the same sound and listener always give the same error.
 */
public final class HearingEngine {

    /** Footsteps are derived from movement rather than emitted by the world, so nobody has to fire an event per step. */
    public List<SoundEvent> footsteps(Perceiver ear, Collection<SensedEntity> entities, long tick) {
        List<SoundEvent> steps = new ArrayList<>();
        for (SensedEntity e : entities) {
            if (e.id().equals(ear.entityId()) || !e.kind().living() || !e.onGround() || e.sneaking()) continue;
            double speed = e.speed();
            if (speed < 0.06D) continue;
            SoundCategory category;
            double loudness;
            if (e.kind() == EntityClass.ANIMAL) { category = SoundCategory.ANIMAL; loudness = 0.3D; }
            else if (e.sprinting() || speed > 0.22D) { category = SoundCategory.FOOTSTEP_SPRINT; loudness = 0.8D; }
            else { category = SoundCategory.FOOTSTEP_WALK; loudness = 0.35D; }
            steps.add(new SoundEvent(UUID.nameUUIDFromBytes((e.id() + "@" + tick).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    category, e.id(), e.x(), e.y(), e.z(), loudness, tick, 10, e.name()));
        }
        return steps;
    }

    public List<HeardSound> hear(Perceiver ear, PerceptionWorld world, PerceptionSettings s, List<SoundEvent> sounds, RayBudget budget) {
        Map<String, HeardSound> best = new HashMap<>();
        double hearing = s.hearingSensitivity() * ear.senses().hearing();
        for (SoundEvent sound : sounds) {
            if (sound.source() != null && sound.source().equals(ear.entityId())) continue;
            double dx = sound.x() - ear.x(), dy = sound.y() - ear.eyeY(), dz = sound.z() - ear.z();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double radius = sound.category().radius(s) * (0.4D + 0.6D * sound.loudness()) * ear.senses().hearing();
            if (radius <= 0.0D || distance > radius) continue;
            double transmittance = 1.0D;
            if (distance > 2.0D) transmittance = budget.tryConsume(1)
                    ? Raycaster.transmittance(world, sound.x(), sound.y(), sound.z(), ear.x(), ear.eyeY(), ear.z()) : 0.85D;
            double dampening = 1.0D - s.wallDampening() * (1.0D - transmittance);
            double intensity = sound.loudness() * Math.pow(1.0D - distance / radius, 1.2D) * dampening * hearing;
            if (intensity < s.hearingThreshold()) continue;
            double horizontal = Math.hypot(dx, dz);
            double relative = horizontal < 1.0E-6D ? 0.0D : VisionCone.wrapDegrees(VisionCone.yawTo(ear.x(), ear.z(), sound.x(), sound.z()) - ear.yaw());
            SoundDirection direction = SoundDirection.of(relative, dy, horizontal);
            double error = distance * (0.04D + 0.20D * (distance / radius)) * (direction.behind() ? 1.5D : 1.0D) / Math.max(0.25D, ear.senses().hearing());
            double[] noise = noise(sound.id(), ear.id());
            HeardSound heard = new HeardSound(sound, distance, Math.min(1.0D, intensity), direction,
                    sound.x() + noise[0] * error, sound.y() + noise[1] * error * 0.3D, sound.z() + noise[2] * error, error);
            String key = sound.category() + ":" + sound.source();
            HeardSound known = best.get(key);
            if (known == null || known.intensity() < heard.intensity()) best.put(key, heard);
        }
        List<HeardSound> result = new ArrayList<>(best.values());
        result.sort(Comparator.comparingDouble(HeardSound::intensity).reversed());
        return result;
    }

    /** A repeatable pseudo-random offset in [-1, 1]^3 for this (sound, listener) pair. */
    static double[] noise(UUID sound, UUID listener) {
        long h = sound.getMostSignificantBits() * 31 + sound.getLeastSignificantBits() * 17 + listener.getMostSignificantBits() * 13 + listener.getLeastSignificantBits();
        h ^= (h >>> 33); h *= 0xff51afd7ed558ccdL; h ^= (h >>> 33);
        double a = ((h & 0xFFFF) / 65535.0D) * 2 - 1, b = (((h >>> 16) & 0xFFFF) / 65535.0D) * 2 - 1, c = (((h >>> 32) & 0xFFFF) / 65535.0D) * 2 - 1;
        return new double[]{a, b, c};
    }
}
