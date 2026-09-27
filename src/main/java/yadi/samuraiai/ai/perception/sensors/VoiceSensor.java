package yadi.samuraiai.ai.perception.sensors;

import yadi.samuraiai.ai.perception.engine.Reports;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Voice input from players. In this project speech recognition runs on the client and arrives as chat text, so today the
 * conversation sensor covers it; this sensor is the ready channel for server-side voice (a spoken utterance with a
 * loudness and a position) and is fully wired: reports queued for the NPC become stimuli exactly like conversation.
 */
public final class VoiceSensor implements Sensor {
    @Override public SensorType type() { return SensorType.VOICE; }

    @Override public void scan(SensorContext ctx) {
        var state = ctx.state();
        Reports.VoiceReport report;
        while ((report = state.pendingVoice.poll()) != null) {
            ctx.out().accept(new Stimulus(StimulusType.VOICE, StimulusCategory.VOICE, report.speaker(), report.speakerName(), report.x(), report.y(), report.z(),
                    0.0D, report.loudness(), StimulusCategory.VOICE.basePriority() * Math.max(0.3D, report.loudness()), ctx.tick(), 100, "voice"));
            state.memory.remember(MemoryKind.SOCIAL, "voice:" + report.speaker(), report.speaker(), report.speakerName(), report.x(), report.y(), report.z(),
                    0, 0, 0.7D, ctx.tick(), "PLAYER");
        }
    }
}
