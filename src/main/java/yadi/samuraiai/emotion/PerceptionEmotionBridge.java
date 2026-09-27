package yadi.samuraiai.emotion;

import yadi.samuraiai.ai.perception.awareness.ThreatLevel;
import yadi.samuraiai.ai.perception.events.InterestDetectedEvent;
import yadi.samuraiai.ai.perception.events.SuspicionRaisedEvent;
import yadi.samuraiai.ai.perception.events.ThreatDetectedEvent;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.npc.NPCManager;

/**
 * The brain's side of the perception contract for feelings: perception publishes what was perceived, and this listener (which
 * belongs to the emotion system, not to perception) lets it move the NPC's emotions. Perception itself never touches emotions.
 */
public final class PerceptionEmotionBridge {
    private PerceptionEmotionBridge() { }

    public static void install() {
        NPCEventBus bus = NPCEventBus.getInstance();
        bus.subscribe(ThreatDetectedEvent.class, event -> NPCManager.getInstance().find(event.npcId()).filter(npc -> npc.isActive()).ifPresent(npc -> {
            if (event.level().atLeast(ThreatLevel.DANGER)) EmotionService.getInstance().stimulate(npc, EmotionService.Stimulus.FEAR, (int) Math.round(event.score() / 4.0D));
            else if (event.level() == ThreatLevel.WARNING) EmotionService.getInstance().adjust(npc, Emotion.ANXIETY, 8);
        }));
        bus.subscribe(SuspicionRaisedEvent.class, event -> NPCManager.getInstance().find(event.npcId()).filter(npc -> npc.isActive())
                .ifPresent(npc -> EmotionService.getInstance().adjust(npc, Emotion.ANXIETY, 10)));
        bus.subscribe(InterestDetectedEvent.class, event -> NPCManager.getInstance().find(event.npcId()).filter(npc -> npc.isActive())
                .ifPresent(npc -> EmotionService.getInstance().adjust(npc, Emotion.SURPRISE, 5)));
    }
}
