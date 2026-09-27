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
 * A watchful town guard. Exists mainly to prove the extension point: it is a
 * different NPC with different priorities and not one line of Brain code was
 * touched to add it.
 */
public final class GuardDefinition {

    public static final NPCTypeId TYPE = NPCTypeId.of("guard");

    private GuardDefinition() {
    }

    public static NPCDefinition create() {
        return new BaseNPCDefinition(
                TYPE,
                "Guardia",
                new NpcPersonality(
                        "Guardia",
                        "Vigilante y directo. Desconfia de los forasteros y habla poco, "
                                + "pero cumple su deber sin dudar."),
                Set.of(
                        NPCCapability.CAN_FIGHT,
                        NPCCapability.CAN_PATROL,
                        NPCCapability.CAN_TALK,
                        NPCCapability.CAN_USE_WEAPON),
                List.of(
                        GoalType.PATROL,
                        GoalType.PROTECT,
                        GoalType.INVESTIGATE,
                        GoalType.TALK));
    }
}
