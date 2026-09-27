package yadi.samuraiai.ai.knowledge.engine;

import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.PersonalityView;

/**
 * How an NPC sees the people it hears things from. The knowledge engine asks; the cognition layer answers from the relationship
 * engine. Trust and respect run 0-100, reputation 0-1, sociability -1..+1. This is the only channel through which trust
 * modifies the transmission of information, and keeps the knowledge engine independent of relationships.
 */
public interface SourceView {
    double trust(UUID listener, UUID source);
    double respect(UUID listener, UUID source);
    double reputation(UUID listener, UUID source);
    PersonalityView personality(UUID npc);

    SourceView NEUTRAL = new SourceView() {
        @Override public double trust(UUID listener, UUID source) { return 40.0D; }
        @Override public double respect(UUID listener, UUID source) { return 30.0D; }
        @Override public double reputation(UUID listener, UUID source) { return 0.3D; }
        @Override public PersonalityView personality(UUID npc) { return PersonalityView.NEUTRAL; }
    };
}
