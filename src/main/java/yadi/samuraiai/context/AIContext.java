package yadi.samuraiai.context;

import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.memory.ConversationMemory;
import yadi.samuraiai.npc.relationship.Relationship;
import yadi.samuraiai.personality.NpcPersonality;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything the dialogue pipeline is allowed to tell the language model
 * about one exchange: who is speaking, to whom, how the NPC currently feels,
 * what it remembers, and what is around it.
 *
 * <p>Immutable and built through {@link Builder}. The previous mutable
 * JavaBean was populated field by field and then handed to another thread,
 * which meant a half-filled context could be observed mid-build; it also
 * carried {@code npcName} and {@code playerName} that nothing ever read, so
 * NPCs did not know their own name in conversation.
 */
public final class AIContext {

    private final String npcName;
    private final String npcType;
    private final String playerName;
    private final String playerMessage;
    private final NpcPersonality personality;
    private final ConversationMemory memory;
    private final Map<Emotion, Integer> emotions;
    private final Relationship relationship;
    private final List<String> nearby;
    private final String timeOfDay;
    private final List<String> world;

    private AIContext(Builder builder) {
        this.npcName = builder.npcName;
        this.npcType = builder.npcType;
        this.playerName = builder.playerName;
        this.playerMessage = builder.playerMessage;
        this.personality = builder.personality;
        this.memory = builder.memory;
        this.emotions = builder.emotions == null ? Map.of() : Map.copyOf(builder.emotions);
        this.relationship = builder.relationship;
        this.nearby = builder.nearby == null ? List.of() : List.copyOf(builder.nearby);
        this.timeOfDay = builder.timeOfDay;
        this.world = builder.world == null ? List.of() : List.copyOf(builder.world);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getNpcName() {
        return npcName == null ? "NPC" : npcName;
    }

    public String getNpcType() {
        return npcType;
    }

    public String getPlayerName() {
        return playerName == null ? "viajero" : playerName;
    }

    public String getPlayerMessage() {
        return playerMessage == null ? "" : playerMessage;
    }

    public NpcPersonality getPersonality() {
        return personality;
    }

    public ConversationMemory getMemory() {
        return memory;
    }

    public Map<Emotion, Integer> getEmotions() {
        return emotions;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public List<String> getNearby() {
        return nearby;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    /** What the NPC knows of its world right now (date, season, weather, its village, open problems), one line each. */
    public List<String> getWorld() {
        return world;
    }

    public static final class Builder {

        private String npcName;
        private String npcType;
        private String playerName;
        private String playerMessage;
        private NpcPersonality personality;
        private ConversationMemory memory;
        private Map<Emotion, Integer> emotions;
        private Relationship relationship;
        private List<String> nearby;
        private String timeOfDay;
        private List<String> world;

        private Builder() {
        }

        public Builder npcName(String value) {
            this.npcName = value;
            return this;
        }

        public Builder npcType(String value) {
            this.npcType = value;
            return this;
        }

        public Builder playerName(String value) {
            this.playerName = value;
            return this;
        }

        public Builder playerMessage(String value) {
            this.playerMessage = value;
            return this;
        }

        public Builder personality(NpcPersonality value) {
            this.personality = value;
            return this;
        }

        public Builder memory(ConversationMemory value) {
            this.memory = value;
            return this;
        }

        public Builder emotions(Map<Emotion, Integer> value) {
            this.emotions = value;
            return this;
        }

        public Builder relationship(Relationship value) {
            this.relationship = value;
            return this;
        }

        public Builder nearby(List<String> value) {
            this.nearby = value;
            return this;
        }

        public Builder timeOfDay(String value) {
            this.timeOfDay = value;
            return this;
        }

        public Builder world(List<String> value) {
            this.world = value;
            return this;
        }

        public AIContext build() {
            Objects.requireNonNull(playerMessage, "playerMessage");
            return new AIContext(this);
        }
    }
}
