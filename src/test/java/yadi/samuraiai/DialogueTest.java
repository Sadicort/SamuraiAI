package yadi.samuraiai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import yadi.samuraiai.ai.*;
import yadi.samuraiai.action.TalkAction;
import yadi.samuraiai.runtime.DialogueService;
import yadi.samuraiai.perception.NoopPerceptionSystem;

class DialogueTest extends ServerTestBase {
    @Test void nextTurnSeesCommittedHistoryAndResponsesStayOrdered() {
        var npc=spawn("speaker");List<CompletableFuture<AIResponse>> responses=new ArrayList<>();
        List<Integer> history=new ArrayList<>();
        try(var queue=new AIRequestQueue(context->{ history.add(context.getMemory().size());var f=new CompletableFuture<AIResponse>();responses.add(f);return f;})) {
            var service=new DialogueService(queue,new NoopPerceptionSystem());
            service.submit(npc,new TalkAction("p1",UUID.randomUUID(),"first"));
            service.submit(npc,new TalkAction("p2",UUID.randomUUID(),"second"));
            assertEquals(1,responses.size());
            responses.get(0).complete(AIResponse.ok("one"));
            assertEquals(List.of(0,2),history);
            responses.get(1).complete(AIResponse.ok("two"));
            assertEquals(4,npc.getConversationMemory().size());
            assertEquals("p1: first",npc.getConversationMemory().snapshot().get(0).getContent());
            assertEquals("p2: second",npc.getConversationMemory().snapshot().get(2).getContent());
            assertEquals(0,service.pending(npc.getId()));service.clear();
        }
    }
    @Test void lateReplyAfterRemovalCannotMutateMemory() throws Exception {
        var npc=spawn("removed");var response=new CompletableFuture<AIResponse>();
        try(var queue=new AIRequestQueue(c->response)) {
            var service=new DialogueService(queue,new NoopPerceptionSystem());
            service.submit(npc,new TalkAction("p",UUID.randomUUID(),"hi"));
            CompletableFuture.runAsync(()->response.complete(AIResponse.ok("late"))).get();
            yadi.samuraiai.spawn.NPCSpawnService.getInstance().remove(npc.getId(),"test",true);
            drain();
            assertEquals(0,npc.getConversationMemory().size());service.clear();
        }
    }
    @Test void cancellationStopsUpstreamAndQueuedTurns() {
        var npc=spawn("cancel");var response=new CompletableFuture<AIResponse>();
        try(var queue=new AIRequestQueue(c->response)) {
            var service=new DialogueService(queue,new NoopPerceptionSystem());
            service.submit(npc,new TalkAction("p",UUID.randomUUID(),"a"));
            service.submit(npc,new TalkAction("p",UUID.randomUUID(),"b"));
            service.cancelNpc(npc.getId());
            assertTrue(response.isCancelled());assertEquals(0,service.pending(npc.getId()));
            assertEquals(0,npc.getConversationMemory().size());
        }
    }
    @Test void failedTurnHasFallbackButDoesNotRewardTrustOrPolluteHistory() {
        var npc=spawn("offline");UUID player=UUID.randomUUID();
        try(var queue=new AIRequestQueue(c->CompletableFuture.completedFuture(AIResponse.failure(AIError.UNAVAILABLE)))) {
            var service=new DialogueService(queue,new NoopPerceptionSystem());
            service.submit(npc,new TalkAction("p",player,"hi"));
            assertEquals(0,npc.getConversationMemory().size());assertFalse(npc.getInstance().hasRelationship(player));
            assertTrue(AIResponse.failure(AIError.UNAVAILABLE).hasSpeakableText());service.clear();
        }
    }
}
