package yadi.samuraiai;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.spawn.*;
import yadi.samuraiai.world.SpawnLocation;
import static org.junit.jupiter.api.Assertions.*;

class FoundationRuntimeStressTest extends ServerTestBase {
    @Test void fiveHundredLogicalRuntimesRegisterIndexAndRemoveCleanly() {
        SamuraiSettings.Values original=SamuraiSettings.snapshot();
        SamuraiSettings.apply(withMaxActive(original,1000));
        try {
            for(int index=0;index<500;index++) {
                var result=NPCSpawnService.getInstance().spawn(new NPCSpawnRequest(NPCTypeId.SAMURAI,"stress_"+index,
                        new SpawnLocation("minecraft:overworld",index%32,64,index/32,0)));
                assertTrue(result.success(),result.message());
            }
            assertEquals(500,NPCManager.getInstance().count());
            assertEquals(500,NPCManager.getInstance().byType(NPCTypeId.SAMURAI).size());
            assertEquals(500,NPCManager.getInstance().byWorld("minecraft:overworld").size());
            assertEquals(500,NPCSpawnService.getInstance().removeAll("stress complete",true));
            assertTrue(NPCManager.getInstance().isEmpty());
        } finally {
            if(!NPCManager.getInstance().isEmpty()) NPCSpawnService.getInstance().removeAll("stress cleanup",true);
            SamuraiSettings.apply(original);
        }
    }
    private static SamuraiSettings.Values withMaxActive(SamuraiSettings.Values v,int maximum) {
        return new SamuraiSettings.Values(v.ollamaHost(),v.ollamaModel(),v.connectTimeoutSeconds(),v.requestTimeoutSeconds(),
                v.maxConcurrentRequests(),v.aiTimeoutSeconds(),v.queueSize(),v.queueWaitMillis(),v.dialogueQueueSize(),
                v.maxMessageLength(),v.memoryMessages(),v.responseLength(),v.promptLimit(),v.playerCooldownMillis(),
                v.npcCooldownMillis(),v.conversationCooldownMillis(),maximum,v.brainTickInterval(),v.perceptionRadius(),
                v.chatRadius(),v.answerPublicChat(),v.emotionDecay());
    }
}
