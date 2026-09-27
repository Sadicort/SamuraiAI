package yadi.samuraiai.ai.emotion.states;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.Blend;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;

/** Emotions coexist: this reads the mixture (joy with pride, fear with hope, sadness with calm...) by combining each kind's records and naming the dominant one and, when strong enough, the second. */
public final class EmotionalBlender {
    public Blend blend(EmotionRuntime rt, EmotionSettings s) {
        Map<EmotionKind, Double> combined = new EnumMap<>(EmotionKind.class);
        for (EmotionRecord r : rt.activeRecords()) combined.merge(r.kind(), r.intensity(), (a, b) -> 100.0D * (1.0D - (1.0D - a / 100.0D) * (1.0D - b / 100.0D)));
        if (combined.isEmpty()) return Blend.CALM;
        List<Map.Entry<EmotionKind, Double>> ranked = new ArrayList<>(combined.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        var first = ranked.get(0);
        if (first.getValue() < s.blendMinIntensity() * 0.5D) return Blend.CALM;
        EmotionKind second = null;
        if (ranked.size() > 1 && ranked.get(1).getValue() >= s.blendMinIntensity() && ranked.get(1).getValue() >= first.getValue() * s.blendRatio()) second = ranked.get(1).getKey();
        double total = 0, valence = 0, arousal = 0;
        for (var e : combined.entrySet()) { total += e.getValue(); valence += e.getKey().valence() * e.getValue(); arousal += e.getKey().arousal() * e.getValue(); }
        String label = second == null ? first.getKey().name() : first.getKey().name() + "+" + second.name();
        return new Blend(first.getKey(), second, label, first.getValue(), total == 0 ? 0 : valence / total, total == 0 ? 0 : arousal / total);
    }
}
