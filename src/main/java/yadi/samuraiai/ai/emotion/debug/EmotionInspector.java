package yadi.samuraiai.ai.emotion.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.HistoryEntry;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;

/** Text views over an NPC's emotions: mood and blend, active emotions with an intensity chart, traumas with their recovery, and the emotional timeline. */
public final class EmotionInspector {
    private EmotionInspector() { }

    public static List<String> summary(EmotionRuntime rt, long now) {
        List<String> lines = new ArrayList<>();
        lines.add("ánimo=" + rt.mood() + " desde hace " + Math.max(0, now - rt.moodSince()) + " ticks; mezcla=" + rt.blend().label() + String.format(Locale.ROOT, " (valencia %.2f, activación %.2f)", rt.blend().valence(), rt.blend().arousal()));
        lines.add("técnica=" + rt.technique() + " consejo=" + rt.advice().technique() + String.format(Locale.ROOT, " (urgencia %.2f)", rt.advice().urgency()) + " último disparador=" + rt.lastTrigger());
        return lines;
    }

    /** One bar per active emotion: a quick "emotional graph". */
    public static List<String> chart(EmotionRuntime rt) {
        List<EmotionRecord> records = rt.activeRecords();
        records.sort((a, b) -> Double.compare(b.intensity(), a.intensity()));
        List<String> lines = new ArrayList<>();
        for (EmotionRecord r : records) {
            int bars = (int) Math.round(r.intensity() / 5.0D);
            lines.add(String.format(Locale.ROOT, "%-18s %5.1f %s [%s %s%s]", r.kind(), r.intensity(), "#".repeat(Math.max(0, bars)), r.curve(), r.origin().source(),
                    r.origin().memoryId() == null ? "" : " memoria " + r.origin().memoryId().toString().substring(0, 8)));
        }
        return lines;
    }

    public static List<String> traumas(EmotionRuntime rt) {
        List<String> lines = new ArrayList<>();
        for (TraumaRecord t : rt.traumas())
            lines.add(t.phase() + " " + t.originKind() + " " + t.emotion() + String.format(Locale.ROOT, " profundidad=%.2f recuperación=%.0f%% flashbacks=%d desencadenantes=%d", t.intensity(), t.progress() * 100, t.flashbacks(), t.triggers().size()));
        return lines;
    }

    public static List<String> timeline(EmotionRuntime rt, int limit) {
        List<String> lines = new ArrayList<>();
        var it = rt.history().descendingIterator();
        while (it.hasNext() && lines.size() < limit) {
            HistoryEntry h = it.next();
            lines.add(h.kind() + " pico " + Math.round(h.peak()) + " durante " + (h.end() - h.start()) + " ticks (" + h.source() + ") -> " + h.outcome());
        }
        return lines;
    }
}
