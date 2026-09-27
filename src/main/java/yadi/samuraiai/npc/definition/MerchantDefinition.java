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
 * A travelling trader. Notably lacks CAN_FIGHT, so the Brain will never offer
 * it a COMBAT goal no matter how angry it gets — the capability filter, not a
 * special case, is what keeps a merchant from charging into battle.
 */
public final class MerchantDefinition {

    public static final NPCTypeId TYPE = NPCTypeId.of("merchant");

    private MerchantDefinition() {
    }

    public static NPCDefinition create() {
        return new BaseNPCDefinition(
                TYPE,
                "Mercader",
                new NpcPersonality(
                        "Mercader",
                        "Hablador y astuto. Siempre intenta llevar la conversacion hacia un trato, "
                                + "y prefiere huir antes que pelear."),
                Set.of(
                        NPCCapability.CAN_TRADE,
                        NPCCapability.CAN_TALK),
                List.of(
                        GoalType.TALK,
                        GoalType.REST,
                        GoalType.FLEE));
    }
}
