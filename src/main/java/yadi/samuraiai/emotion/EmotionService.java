package yadi.samuraiai.emotion;

import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.event.npc.EmotionChangedEvent;

/** Mutation boundary. Stimuli are explicit infrastructure for future behaviors. */
public final class EmotionService {
    public enum Stimulus { DAMAGE, CONVERSATION, COMBAT, FEAR, JOY, REST }
    private static final EmotionService INSTANCE = new EmotionService();
    public static EmotionService getInstance() { return INSTANCE; }
    public void adjust(NPCRuntime npc, Emotion emotion, int delta) {
        ServerScheduler.getInstance().requireServerThread();
        if (!npc.isActive()) return;
        int previous = npc.getEmotionState().get(emotion);
        npc.getEmotionState().adjust(emotion, delta);
        int next = npc.getEmotionState().get(emotion);
        if (previous != next) NPCEventBus.getInstance().post(new EmotionChangedEvent(npc.getId(), emotion, next));
    }
    public void decay(NPCRuntime npc) {
        ServerScheduler.getInstance().requireServerThread();
        if (!npc.isActive()) return;
        var before = npc.getEmotionState().snapshot();
        npc.getEmotionState().decay();
        npc.getEmotionState().snapshot().forEach((emotion, value) -> {
            if (!value.equals(before.get(emotion)))
                NPCEventBus.getInstance().post(new EmotionChangedEvent(npc.getId(), emotion, value));
        });
    }
    public void conversation(NPCRuntime npc) { stimulate(npc, Stimulus.CONVERSATION, 1); }
    public void stimulate(NPCRuntime npc, Stimulus stimulus, int magnitude) {
        Emotion emotion = switch (stimulus) {
            case DAMAGE, COMBAT -> Emotion.ANGER;
            case CONVERSATION, REST -> Emotion.CALM;
            case FEAR -> Emotion.FEAR;
            case JOY -> Emotion.HAPPINESS;
        };
        adjust(npc, emotion, Math.max(0, Math.min(100, magnitude)));
    }
}
