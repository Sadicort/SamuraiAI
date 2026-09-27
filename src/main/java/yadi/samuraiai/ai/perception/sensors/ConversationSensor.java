package yadi.samuraiai.ai.perception.sensors;

import yadi.samuraiai.ai.perception.engine.Reports;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/** Someone spoke to the NPC in text: a conversation stimulus, remembered as a social event. */
public final class ConversationSensor implements Sensor {
    @Override public SensorType type() { return SensorType.CONVERSATION; }

    @Override public void scan(SensorContext ctx) {
        var state = ctx.state();
        Reports.ConversationReport report;
        while ((report = state.pendingConversation.poll()) != null) {
            double intensity = Math.min(1.0D, 0.6D + report.length() / 200.0D);
            ctx.out().accept(new Stimulus(StimulusType.CONVERSATION, StimulusCategory.SPEECH, report.speaker(), report.speakerName(), report.x(), report.y(), report.z(),
                    0.0D, intensity, StimulusCategory.SPEECH.basePriority(), ctx.tick(), 100, "conversation"));
            state.memory.remember(MemoryKind.SOCIAL, "spoke:" + report.speaker(), report.speaker(), report.speakerName(), report.x(), report.y(), report.z(),
                    0, 0, 0.7D, ctx.tick(), "PLAYER");
        }
    }
}
