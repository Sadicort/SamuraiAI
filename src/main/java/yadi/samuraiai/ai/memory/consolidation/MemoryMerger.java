package yadi.samuraiai.ai.memory.consolidation;

import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Chapter;
import yadi.samuraiai.ai.memory.model.Consequence;
import yadi.samuraiai.ai.memory.model.EmotionalSignature;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;

/** Fuses one memory into a similar one: counts add up, the emotional signature is averaged by weight, and what followed is kept. The absorbed memory is then dropped by the caller. */
public final class MemoryMerger {
    public void merge(MemoryRecord survivor, MemoryRecord other, MemorySettings s) {
        int n1 = survivor.repeatCount(), n2 = other.repeatCount();
        EmotionalSignature a = survivor.emotion(), b = other.emotion();
        double intensity = (a.intensity() * n1 + b.intensity() * n2) / (n1 + n2);
        double valence = (a.valence() * n1 + b.valence() * n2) / (n1 + n2);
        EmotionalSignature merged = new EmotionalSignature(a.intensity() >= b.intensity() ? a.primary() : b.primary(), a.secondary(), intensity, valence, a.traumatic() || b.traumatic());
        survivor.emotion(merged);
        survivor.repeatCount(n1 + n2);
        survivor.duration(survivor.duration() + other.duration());
        survivor.endTime(other.endTime());
        if (other.importance().ordinal() > survivor.importance().ordinal()) survivor.importance(other.importance());
        double keep = Math.max(survivor.strength(), other.strength());
        survivor.strength(Math.min(1.0D, keep + 0.1D * Math.min(survivor.strength(), other.strength())));
        survivor.emotionalWeight(Math.max(survivor.emotionalWeight(), other.emotionalWeight()));
        survivor.lastReinforced(Math.max(survivor.lastReinforced(), other.lastReinforced()));
        survivor.accessCount(survivor.accessCount() + other.accessCount());
        survivor.confidence(Math.max(survivor.confidence(), other.confidence()));
        survivor.tags().addAll(other.tags());
        for (String event : other.events()) if (survivor.events().size() < s.maxEventsPerRecord() && !survivor.events().contains(event)) survivor.events().add(event);
        for (Consequence c : other.consequences()) {
            boolean found = false;
            for (int i = 0; i < survivor.consequences().size(); i++) {
                Consequence mine = survivor.consequences().get(i);
                if (mine.kind().equals(c.kind()) && mine.target().equals(c.target())) { survivor.consequences().set(i, new Consequence(mine.kind(), mine.target(), mine.magnitude() + c.magnitude())); found = true; break; }
            }
            if (!found && survivor.consequences().size() < s.maxConsequences()) survivor.consequences().add(c);
        }
        if (survivor.chapters().size() < s.maxChaptersPerRecord()) survivor.chapters().add(new Chapter(other.stamp().gameTime(), other.endTime(), other.kind().name().toLowerCase() + " x" + n2));
        survivor.socialLinks().addAll(other.socialLinks());
        survivor.knowledgeLinks().addAll(other.knowledgeLinks());
        survivor.bumpVersion();
    }

    /** Repetition also makes a memory matter a little more: many repeats promote it one level, up to HIGH. */
    public Importance promoted(MemoryRecord r) {
        if (r.repeatCount() >= 20 && r.importance().ordinal() < Importance.HIGH.ordinal()) return Importance.HIGH;
        if (r.repeatCount() >= 6 && r.importance().ordinal() < Importance.NORMAL.ordinal()) return Importance.NORMAL;
        return r.importance();
    }
}
