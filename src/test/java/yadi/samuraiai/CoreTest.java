package yadi.samuraiai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.npc.lifecycle.*;
import yadi.samuraiai.npc.relationship.*;
import yadi.samuraiai.emotion.*;
import yadi.samuraiai.event.*;
import yadi.samuraiai.memory.*;
import yadi.samuraiai.runtime.*;
import yadi.samuraiai.spawn.NPCSpawnService;

class CoreTest extends ServerTestBase {
    @Test void uniqueNamesIndicesAndRemovalAreConsistent() {
        var a=spawn("Kenji");var b=spawn("kenji");
        assertEquals("kenji_2",b.getName());
        assertEquals(2,NPCManager.getInstance().byType(NPCTypeId.SAMURAI).size());
        a.setState(NPCState.RESTING);
        assertEquals(List.of(a),NPCManager.getInstance().byState(NPCState.RESTING));
        assertThrows(IllegalStateException.class,()->NPCManager.getInstance().register(a));
        a.getConversationMemory().addExchange("hello","hi");
        assertTrue(NPCSpawnService.getInstance().remove(a.getId(),"test",true));
        assertFalse(NPCSpawnService.getInstance().remove(a.getId(),"test",true));
        assertFalse(a.isActive());assertTrue(MemoryManager.getInstance().peek(a.getId()).isEmpty());
        assertTrue(NPCManager.getInstance().byState(NPCState.RESTING).isEmpty());
    }
    @Test void lifecycleRejectsInvalidAndDuplicateTransitions() {
        var lifecycle=NPCLifecycleManager.getInstance();UUID id=UUID.randomUUID();
        assertThrows(IllegalStateException.class,()->lifecycle.transition(id,NPCLifecycleState.ACTIVE));
        lifecycle.transition(id,NPCLifecycleState.CREATING);
        assertThrows(IllegalStateException.class,()->lifecycle.transition(id,NPCLifecycleState.CREATING));
        lifecycle.transition(id,NPCLifecycleState.INITIALIZING);lifecycle.transition(id,NPCLifecycleState.ACTIVE);
        lifecycle.transition(id,NPCLifecycleState.UNLOADING);lifecycle.transition(id,NPCLifecycleState.INACTIVE);
        lifecycle.transition(id,NPCLifecycleState.INITIALIZING);lifecycle.transition(id,NPCLifecycleState.ACTIVE);
        lifecycle.transition(id,NPCLifecycleState.UNLOADING);lifecycle.transition(id,NPCLifecycleState.REMOVED);lifecycle.forget(id);
    }
    @Test void servicesPublishClampedChanges() {
        var npc=spawn("social");UUID player=UUID.randomUUID();List<String> events=new ArrayList<>();
        NPCEventBus.getInstance().subscribe(NpcEvent.class,event->events.add(event.getName()));
        EmotionService.getInstance().adjust(npc,Emotion.FEAR,200);
        assertEquals(100,npc.getEmotionState().get(Emotion.FEAR));
        EmotionService.getInstance().decay(npc);assertEquals(98,npc.getEmotionState().get(Emotion.FEAR));
        RelationshipService.getInstance().adjustTrust(npc.getId(),player,200);
        assertEquals(100,npc.getInstance().getRelationship(player).getTrust());
        assertTrue(events.contains("RelationshipCreatedEvent"));
        assertTrue(events.contains("RelationshipUpdatedEvent"));
        RelationshipService.getInstance().remove(npc.getId(),player);
        assertFalse(npc.getInstance().hasRelationship(player));
        assertTrue(events.contains("RelationshipRemovedEvent"));
    }
    @Test void backgroundMutationIsRejectedAndSchedulerReturnsToOwner() throws Exception {
        var npc=spawn("thread");
        var rejected=CompletableFuture.supplyAsync(()->assertThrows(IllegalStateException.class,()->npc.setState(NPCState.DEAD)));
        rejected.get(2,TimeUnit.SECONDS);
        CompletableFuture.runAsync(()->ServerScheduler.getInstance().execute(()->npc.setState(NPCState.RESTING))).get();
        assertEquals(NPCState.IDLE,npc.getState());drain();assertEquals(NPCState.RESTING,npc.getState());
    }
    @Test void oldSessionWorkCannotCrossRestart() throws Exception {
        var old=ServerScheduler.getInstance().executor();var ran=new java.util.concurrent.atomic.AtomicBoolean();
        CompletableFuture.runAsync(()->old.execute(()->ran.set(true))).get();
        ServerScheduler.getInstance().close();
        Thread owner=Thread.currentThread();
        ServerScheduler.getInstance().bind(Runnable::run,()->Thread.currentThread()==owner);
        drain();old.execute(()->ran.set(true));assertFalse(ran.get());
    }
    @Test void listenersAreIsolatedAndAsyncPostsUseServerThread() throws Exception {
        List<String> delivered=new ArrayList<>();
        NPCEventBus.getInstance().subscribe(NpcEvent.class,event->{throw new IllegalStateException("deliberate");});
        NPCEventBus.getInstance().subscribe(NpcEvent.class,event->delivered.add(Thread.currentThread().getName()));
        CompletableFuture.runAsync(()->NPCEventBus.getInstance().post(new PlayerApproachEvent(UUID.randomUUID(),UUID.randomUUID(),"p",1))).get();
        assertTrue(delivered.isEmpty());drain();assertEquals(List.of(Thread.currentThread().getName()),delivered);
    }
    @Test void eventPrioritiesSubscriptionsAndMetricsAreDeterministic() {
        List<String> order=new ArrayList<>();
        var low=NPCEventBus.getInstance().subscribe(NpcEvent.class,EventPriority.LOW,event->order.add("low"));
        var critical=NPCEventBus.getInstance().subscribe(NpcEvent.class,EventPriority.CRITICAL,event->order.add("critical"));
        NPCEventBus.getInstance().post(new PlayerApproachEvent(UUID.randomUUID(),UUID.randomUUID(),"p",1));
        assertEquals(List.of("critical","low"),order);
        assertEquals(1,NPCEventBus.getInstance().metrics().published());
        assertEquals(2,NPCEventBus.getInstance().metrics().deliveries());
        critical.close(); low.close(); assertFalse(critical.active());
    }
    @Test void lifecyclePublishesBoundedHistoryAndMetrics() {
        UUID id=UUID.randomUUID();List<NPCLifecycleTransition> observed=new ArrayList<>();
        var subscription=NPCEventBus.getInstance().subscribe(yadi.samuraiai.event.npc.NPCLifecycleTransitionEvent.class,
                event->observed.add(event.transition()));
        var lifecycle=NPCLifecycleManager.getInstance();
        lifecycle.transition(id,NPCLifecycleState.UNINITIALIZED);
        lifecycle.transition(id,NPCLifecycleState.CREATING);
        lifecycle.transition(id,NPCLifecycleState.INITIALIZING);
        lifecycle.transition(id,NPCLifecycleState.LOADING_RUNTIME);
        lifecycle.transition(id,NPCLifecycleState.ACTIVE);
        assertEquals(5,observed.size());
        assertEquals(1,lifecycle.metrics().currentStates().get(NPCLifecycleState.ACTIVE));
        lifecycle.transition(id,NPCLifecycleState.REMOVING);lifecycle.transition(id,NPCLifecycleState.REMOVED);lifecycle.forget(id);
        subscription.close();
    }
    @Test void memoryWindowIsBoundedAndSnapshotStable() {
        var memory=new ConversationMemory(4);memory.addExchange("a","A");var old=memory.snapshot();
        memory.addExchange("b","B");memory.addExchange("c","C");
        assertEquals(4,memory.size());assertEquals("a",old.get(0).getContent());
        assertThrows(UnsupportedOperationException.class,()->old.clear());
        assertFalse(MemoryManager.getInstance().supports(MemoryKind.PERSISTENT));
    }
    @Test void snapshotRoundtripRejectsUnknownVersion() {
        var dto=yadi.samuraiai.npc.persistence.NPCSnapshot.capture(spawn("saved"));
        var persistence=yadi.samuraiai.npc.persistence.PersistenceBootstrap.getInstance();
        assertEquals(dto,persistence.deserialize(persistence.serialize(dto)));
        assertThrows(RuntimeException.class,()->persistence.deserialize(persistence.serialize(dto).replace("\"schemaVersion\":1","\"schemaVersion\":99")));
    }
    @Test void optionalIntegrationVersionDetectionIsExplicit() {
        assertTrue(yadi.samuraiai.integration.IntegrationLoader.supports("1.19.2.20250701"));
        assertFalse(yadi.samuraiai.integration.IntegrationLoader.supports("1.20.1"));
    }
}
