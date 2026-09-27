package yadi.samuraiai;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import yadi.samuraiai.runtime.*;
import yadi.samuraiai.spawn.NPCSpawnService;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.world.SpawnLocation;

public abstract class ServerTestBase {
    protected final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private Thread owner;
    @BeforeEach void bind() {
        owner = Thread.currentThread();
        ServerScheduler.getInstance().bind(task -> { if (Thread.currentThread() == owner) task.run(); else tasks.add(task); },
                () -> Thread.currentThread() == owner);
    }
    @AfterEach void unbind() {
        NPCSpawnService.getInstance().removeAll("test", true);
        yadi.samuraiai.event.NPCEventBus.getInstance().clear();
        yadi.samuraiai.npc.relationship.RelationshipService.getInstance().clear();
        ServerScheduler.getInstance().close();
        tasks.clear();
    }
    protected void drain() { Runnable task; while ((task=tasks.poll()) != null) task.run(); }
    protected NPCRuntime spawn(String name) {
        var result = NPCSpawnService.getInstance().spawn(new yadi.samuraiai.spawn.NPCSpawnRequest(
                NPCTypeId.SAMURAI,name,new SpawnLocation("minecraft:overworld",0,64,0,0)));
        Assertions.assertTrue(result.success(),result.message());
        return NPCManager.getInstance().find(result.instance().getIdentity().id()).orElseThrow();
    }
}
