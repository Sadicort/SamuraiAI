package yadi.samuraiai.ai.emotion.regulation;

import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;

/** Mental fortitude, 0.5-1.5: discipline and courage help, so do the traumas already overcome and present hope; open wounds weigh it down. It scales how hard unpleasant emotions hit and how fast wounds heal. */
public final class ResilienceModel {
    public double factor(PersonalityView p, EmotionRuntime rt, EmotionSettings s) {
        int recovered = 0, open = 0;
        for (TraumaRecord t : rt.traumas()) { if (t.phase() == TraumaRecord.Phase.RECOVERED) recovered++; else open++; }
        double hope = 0;
        for (EmotionRecord r : rt.activeRecords()) if (r.kind() == EmotionKind.HOPE) hope = Math.max(hope, r.intensity() / 100.0D);
        double f = 1.0D + s.resilienceDiscipline() * p.lean(Trait.DISCIPLINE) + s.resilienceCourage() * p.lean(Trait.COURAGE)
                + s.resilienceExperience() * Math.min(1.0D, recovered / 5.0D) + s.resilienceHope() * hope - s.resiliencePenalty() * Math.min(3, open);
        return Math.max(0.5D, Math.min(1.5D, f));
    }
}
