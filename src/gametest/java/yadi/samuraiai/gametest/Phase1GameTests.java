package yadi.samuraiai.gametest;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.core.BlockPos;
import yadi.samuraiai.spawn.*;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.npc.lifecycle.*;
import yadi.samuraiai.runtime.*;
import yadi.samuraiai.memory.MemoryManager;
import yadi.samuraiai.world.SpawnLocation;
import java.util.*;

@GameTestHolder("samuraiai")
@PrefixGameTestTemplate(false)
public final class Phase1GameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void lifecycleAndAvatars(GameTestHelper helper) {
        ServerScheduler.getInstance().requireServerThread();
        var service=NPCSpawnService.getInstance();
        require(service.getController().requiresPhysicalBody() == Boolean.getBoolean("samuraiai.expectCustomNpcs"),
                "Controller must match the selected integration profile, not silently fall back");
        var pos=helper.absolutePos(new BlockPos(2,2,2));
        for(String type:List.of("samurai","guard","merchant")) {
            var result=service.spawn(new NPCSpawnRequest(NPCTypeId.of(type),"phase1_"+type,
                    new SpawnLocation(helper.getLevel().dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ(),0)));
            require(result.success(),result.message());
            UUID id=result.instance().getIdentity().id();
            try {
                var npc=NPCManager.getInstance().find(id).orElseThrow();
                require(npc.isActive(),"Runtime must be active");
                require(NPCLifecycleManager.getInstance().getState(id)==NPCLifecycleState.ACTIVE,"Lifecycle ACTIVE");
                if(service.getController().requiresPhysicalBody())
                    require(service.getController().isPhysicalPresent(npc.getInstance()),"CustomNPCs body must exist");
                npc.getBrain().tick(yadi.samuraiai.context.NPCContext.of(npc),
                        new yadi.samuraiai.perception.WorldPerceptionSystem().perceive(npc),npc,npc.getController());
                require(NPCManager.getInstance().byWorld(npc.getInstance().getLocation().dimensionKey()).contains(npc),"World index");
                var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();
                require(dispatcher.getRoot().getChild("samuraiai")!=null,"Command registered");
                for(String command:List.of("samuraiai list","samuraiai types"))
                    require(helper.getLevel().getServer().getCommands().performPrefixedCommand(
                            helper.getLevel().getServer().createCommandSourceStack().withPermission(4),command)>0,"Command "+command);
            } finally { service.remove(id,"gametest",true); }
            require(NPCManager.getInstance().find(id).isEmpty(),"No runtime after removal");
            require(MemoryManager.getInstance().peek(id).isEmpty(),"Memory cleaned");
            require(!service.getController().isPhysicalPresent(result.instance()),"No avatar after removal");
        }
        System.out.println("PHASE1_GAMETEST_OK customnpcs="+service.getController().requiresPhysicalBody());
        helper.succeed();
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
