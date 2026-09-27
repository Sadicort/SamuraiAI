package yadi.samuraiai.npc.definition;

import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.npc.BaseNPCDefinition;
import yadi.samuraiai.npc.NPCCapability;
import yadi.samuraiai.npc.NPCDefinition;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.personality.NpcPersonality;

import java.util.List;
import java.util.Set;

/**
 * First concrete NPC type. Everything here is data — adding GUARD,
 * MERCHANT, MONK, BANDIT, NINJA later means writing a sibling class like
 * this one and registering it in {@link yadi.samuraiai.registry.NPCTypeRegistry},
 * not touching the Brain.
 */
public final class SamuraiDefinition {

    public static final NPCTypeId TYPE = NPCTypeId.SAMURAI;

    private SamuraiDefinition() {
    }

    public static NPCDefinition create() {
        return new BaseNPCDefinition(
                TYPE,
                "Samurai",
                new NpcPersonality(
                        "Samurai",
                        "Disciplinado, valiente y honorable. Habla con calma y respeto."
                ),
                Set.of(
                        NPCCapability.CAN_FIGHT,
                        NPCCapability.CAN_PATROL,
                        NPCCapability.CAN_TALK,
                        NPCCapability.CAN_USE_WEAPON
                ),
                List.of(
                        GoalType.PROTECT,
                        GoalType.PATROL,
                        GoalType.INVESTIGATE,
                        GoalType.TALK,
                        GoalType.REST
                )
        );
    }
}
